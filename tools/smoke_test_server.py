#!/usr/bin/env python3
"""Production smoke test: runs the BUILT jar on a stock Fabric dedicated server (no dev
environment, no test mod), generates chunks in all three dimensions, summons both entities,
then stops and reports every WARN/ERROR line in the server's latest.log.
Usage: ./gradlew build && python3 tools/smoke_test_server.py"""
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = os.path.join(ROOT, "build", "prodtest")
MC, LOADER, FABRIC_API = "26.3", "0.20.1", "0.166.2+26.3"


def fetch(url, dest):
    if not os.path.exists(dest):
        print("downloading", url)
        with urllib.request.urlopen(url, timeout=300) as r, open(dest, "wb") as fh:
            shutil.copyfileobj(r, fh)


def main():
    java = os.path.join(ROOT, ".jdk25", "bin", "java")
    java = java if os.path.exists(java) else "java"
    shutil.rmtree(os.path.join(WORK, "world"), ignore_errors=True)
    shutil.rmtree(os.path.join(WORK, "logs"), ignore_errors=True)
    os.makedirs(os.path.join(WORK, "mods"), exist_ok=True)
    installer = json.load(urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/installer", timeout=60))[0]["version"]
    fetch(f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{LOADER}/{installer}/server/jar", os.path.join(WORK, "fabric-server-launch.jar"))
    fetch(f"https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{FABRIC_API}/fabric-api-{FABRIC_API}.jar",
          os.path.join(WORK, "mods", f"fabric-api-{FABRIC_API}.jar"))
    for old in glob.glob(os.path.join(WORK, "mods", "backrooms-mod-*.jar")):
        os.remove(old)
    jar = [j for j in glob.glob(os.path.join(ROOT, "build", "libs", "backrooms-mod-*.jar")) if "sources" not in j][0]
    shutil.copy(jar, os.path.join(WORK, "mods"))
    with open(os.path.join(WORK, "eula.txt"), "w") as fh:
        fh.write("eula=true\n")
    with open(os.path.join(WORK, "server.properties"), "w") as fh:
        fh.write("online-mode=false\nserver-port=25599\npause-when-empty-seconds=-1\n")

    proc = subprocess.Popen([java, "-Xmx2G", "-jar", "fabric-server-launch.jar", "nogui"], cwd=WORK,
                            stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    lines = []

    def pump():
        for line in proc.stdout:
            lines.append(line.rstrip())

    threading.Thread(target=pump, daemon=True).start()
    import atexit
    atexit.register(lambda: proc.poll() is None and proc.kill())  # never leave a server holding the port

    def wait_for(pattern, timeout):
        end = time.time() + timeout
        seen = 0
        while time.time() < end:
            for line in lines[seen:]:
                if re.search(pattern, line):
                    return line
            seen = len(lines)
            if proc.poll() is not None:
                break
            time.sleep(0.5)
        print("\n".join(lines[-40:]))
        sys.exit("TIMEOUT waiting for: " + pattern)

    def command(text, expect, timeout=120):
        before = len(lines)
        proc.stdin.write(text + "\n")
        proc.stdin.flush()
        end = time.time() + timeout
        while time.time() < end:
            for line in lines[before:]:
                if re.search(expect, line):
                    print("  >", text, "\n     ", line.split("]: ", 1)[-1])
                    return
            time.sleep(0.3)
        print("\n".join(lines[-30:]))
        sys.exit("FAILED: " + text)

    print(wait_for(r"Done \(", 600).split("]: ", 1)[-1])
    # one block per level that the layout guarantees: carpet floor, a corner column, a striped pillar base
    proofs = {"level_0": "4 32 4 backrooms:damp_carpet", "poolrooms": "0 34 0 backrooms:pool_tile", "carpark": "0 33 0 backrooms:hazard_pillar"}
    for dim, proof in proofs.items():
        command(f"execute in backrooms:{dim} run forceload add -32 -32 31 31", r"Marked \d+ chunks")
        deadline = time.time() + 120
        while True:  # chunk generation is asynchronous, so poll until the block is there
            before = len(lines)
            proc.stdin.write(f"execute in backrooms:{dim} if block {proof}\n")
            proc.stdin.flush()
            time.sleep(1.0)
            if any("Test passed" in line for line in lines[before:]):
                print(f"  > chunks generated in backrooms:{dim}: block {proof} present")
                break
            if time.time() > deadline:
                sys.exit(f"FAILED: backrooms:{dim} did not generate {proof}")
        command(f"execute in backrooms:{dim} run summon backrooms:fixture 4.5 33 4.5", r"Summoned new Fixture")
        command(f"execute in backrooms:{dim} run summon backrooms:tube_moth 4.5 34 4.5", r"Summoned new Tube Moth")
    command("give @a backrooms:key_fob", r"No player was found")
    command("stop", r"Stopping (the )?server")
    proc.wait(timeout=120)

    log = open(os.path.join(WORK, "logs", "latest.log")).read().splitlines()
    bad = [l for l in log if re.search(r"/(WARN|ERROR|FATAL)\]", l)]
    mods = [l for l in log if "backrooms" in l.lower() or "Backrooms" in l]
    print("\nserver log: %d lines, %d WARN/ERROR/FATAL" % (len(log), len(bad)))
    for line in bad:
        print("  ", line[:200])
    print("mod lines:")
    for line in mods[:6]:
        print("  ", line[:200])


if __name__ == "__main__":
    main()
