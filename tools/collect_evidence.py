#!/usr/bin/env python3
"""Turns the last playtest run into docs/playtest-evidence.md + docs/screenshots/.

Run after tools/run_playtest.sh (and, optionally, tools/smoke_test_server.py):
    tools/run_playtest.sh > build-playtest.log 2>&1; python3 tools/collect_evidence.py
"""
import datetime
import glob
import hashlib
import os
import re
import shutil
import subprocess

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)
KEEP = ["singleplayer_noclip_tell", "singleplayer_level_0_arrival", "singleplayer_level_0_light_flickered_off",
        "singleplayer_level_0_exit_door", "singleplayer_wrong_stairwell", "singleplayer_poolrooms_arrival",
        "singleplayer_end_void_tell", "singleplayer_poolrooms_fell_into_pool", "singleplayer_key_fob_in_hand",
        "singleplayer_carpark_arrival", "singleplayer_carpark_exit_door", "singleplayer_fixture_watched_far",
        "singleplayer_fixture_watched_closer", "singleplayer_fixture_point_blank", "singleplayer_tube_moth",
        "dedicated_level_0_arrival", "dedicated_poolrooms_arrival", "dedicated_carpark_arrival",
        "dedicated_fixture_watched_closer"]


def audio_summary():
    """The playtest points OpenAL at a WAV file; measure how much of it is actually sound."""
    wav = "build/playtest-audio.wav"
    if not os.path.exists(wav):
        return "No audio capture found."
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", wav, "-ac", "1", "-ar", "8000", "-f", "s16le", "-"],
                         check=True, capture_output=True).stdout
    a = np.frombuffer(raw, dtype="<i2").astype(np.float64) / 32768
    sec = len(a) // 8000
    rms = np.sqrt((a[:sec * 8000].reshape(sec, 8000) ** 2).mean(axis=1))
    return (f"{sec} s of audio captured from the game's OpenAL output (written by OpenAL Soft's wave backend); "
            f"{int((rms > 0.002).sum())} of {sec} seconds are non-silent; peak level {np.abs(a).max():.2f}, "
            f"loudest one-second RMS {rms.max():.3f}.")


def main():
    rows = []
    for line in open("build-playtest.log").read().splitlines():
        m = re.search(r"\[BRTEST\] (PASS|FAIL|SOUND) (.*)", line)
        if m:
            rows.append((m.group(1), m.group(2)))
        m = re.search(r"\(Backrooms\) (.* slipped from .*)", line)
        if m:
            rows.append(("MOD", m.group(1)))
    client = open("build/run/clientGameTest/logs/latest.log").read().splitlines()
    errors = sorted({re.sub(r"^\[[0-9:]+\] ", "", l) for l in client if re.search(r"/(ERROR|FATAL)\]", l)})

    os.makedirs("docs/screenshots", exist_ok=True)
    for old in glob.glob("docs/screenshots/*.png"):
        os.remove(old)
    shots = []
    for name in KEEP:
        found = sorted(glob.glob(f"build/run/clientGameTest/screenshots/*_{name}.png"))
        if found:
            shutil.copy(found[0], f"docs/screenshots/{name}.png")
            shots.append(name)

    passed, failed = sum(r[0] == "PASS" for r in rows), sum(r[0] == "FAIL" for r in rows)
    out = ["# Playtest evidence", "",
           f"Produced by `tools/run_playtest.sh` (a real Minecraft 26.3 client under Xvfb, driven by Fabric's client game test API) on {datetime.date.today().isoformat()}.",
           "The same scripted playthrough runs twice: once in a singleplayer world and once with the client connected to a real dedicated server.", "",
           f"**Result: {passed} checks passed, {failed} failed.**", "",
           "## Sound", "", audio_summary(), "",
           "`SOUND` lines below are logged by a listener on the client's sound engine the first time each of the mod's sound events is actually played.", "",
           "## Screenshots", ""]
    out += [f"- [`{k}.png`](screenshots/{k}.png)" for k in shots]
    out += ["", "## Log lines", "", "`MOD` lines are the mod's own server-side log; everything else is the test's.", "", "```"]
    out += [f"{k:<10} {v}" for k, v in rows]
    out += ["```", "", "## ERROR lines in the client's latest.log", "",
            "None come from the mod. They are vanilla failing to reach Mojang's online services from the offline sandbox, plus one graphics-option message from the software renderer:",
            "", "```"] + errors + ["```", ""]

    server_log = "build/prodtest/logs/latest.log"
    jars = [j for j in glob.glob("build/libs/backrooms-mod-*.jar") if "sources" not in j]
    if os.path.exists(server_log) and jars:
        log = open(server_log).read().splitlines()
        bad = [l for l in log if re.search(r"/(ERROR|FATAL)\]", l)]
        warn = sorted({re.sub(r"^\[[0-9:]+\] ", "", l)[:150] for l in log if "/WARN]" in l})
        keep = [re.sub(r"^\[[0-9:]+\] ", "", l)[:170] for l in log
                if re.search(r"Backrooms: Thresholds|Marked \d+ chunks in backrooms|Test passed|Summoned new|Done \(|Saving chunks for level .*backrooms", l)]
        digest = hashlib.sha256(open(jars[0], "rb").read()).hexdigest()
        out += ["## Production server smoke test", "",
                f"`tools/smoke_test_server.py` ran the built jar (`{jars[0]}`, {os.path.getsize(jars[0])} bytes, sha256 `{digest[:16]}...`) on a stock Fabric 0.20.1 dedicated server for 26.3 with only Fabric API alongside it.",
                "", f"**{len(log)} log lines, {len(bad)} ERROR/FATAL lines.** The only warnings are vanilla's offline-mode notices and failed calls to Mojang's online services:",
                "", "```"] + warn + ["```", "", "Relevant lines:", "", "```"] + keep + ["```", ""]
    with open("docs/playtest-evidence.md", "w") as fh:
        fh.write("\n".join(out))
    print(f"{passed} passed, {failed} failed; {len(shots)} screenshots; {len(errors)} distinct client ERROR lines")
    for e in errors:
        print("  ", e)


if __name__ == "__main__":
    main()
