#!/usr/bin/env python3
"""Synthesises every sound in the mod from scratch with numpy (no samples, no recordings)
and encodes them to Ogg Vorbis with ffmpeg.  Usage: python3 tools/gen_sounds.py"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms", "sounds")
rng = np.random.default_rng(1989)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def band_noise(seconds, lo, hi):
    """Noise shaped in the frequency domain, so it is perfectly periodic (loops seamlessly)."""
    n = int(SR * seconds)
    spec = np.fft.rfft(rng.standard_normal(n))
    f = np.fft.rfftfreq(n, 1 / SR)
    spec *= ((f >= lo) & (f <= hi)).astype(float)
    out = np.fft.irfft(spec, n)
    return out / (np.max(np.abs(out)) + 1e-9)


def circular_echo(x, delays_gains):
    """Echo that wraps around the end of the buffer, keeping loops seamless."""
    out = x.copy()
    for delay, gain in delays_gains:
        out += gain * np.roll(x, int(delay * SR))
    return out


def env(n, attack, release):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a:
        e[:a] = np.linspace(0, 1, a)
    if r:
        e[-r:] *= np.linspace(1, 0, r)
    return e


def decay(seconds, rate):
    return np.exp(-rate * t(seconds))


def chirp(seconds, f0, f1):
    tt = t(seconds)
    return np.sin(2 * np.pi * (f0 * tt + (f1 - f0) * tt * tt / (2 * seconds)))


def place(buf, sound, at):
    i = int(at * SR)
    idx = (i + np.arange(len(sound))) % len(buf)
    np.add.at(buf, idx, sound)


def write(name, x, peak=0.8):
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    path = os.path.join(OUT, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
    os.remove(wav)
    print("  %-32s %.2fs" % (name, len(x) / SR))


# ------------------------------------------------------------ ambient loops
def ambient_level_0():
    """Fluorescent ballast hum: mains harmonics plus a thin, nervous high buzz."""
    s = 10.0
    tt = t(s)
    hum = sum(a * np.sin(2 * np.pi * f * tt + p) for f, a, p in
              ((60, 1.0, 0), (120, 0.8, 1), (180, 0.35, 2), (240, 0.2, 0.5), (360, 0.1, 1.3)))
    hum *= 1 + 0.08 * np.sin(2 * np.pi * 0.3 * tt)
    buzz = band_noise(s, 5200, 9000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 120 * tt)))
    air = band_noise(s, 150, 900)
    return hum * 0.55 + buzz * 0.07 + air * 0.10


def ambient_poolrooms():
    """Water lapping in a big tiled hall, with scattered drips and long echoes."""
    s = 12.0
    tt = t(s)
    lap = band_noise(s, 120, 1400) * (0.55 + 0.45 * np.sin(2 * np.pi * 0.25 * tt) * np.sin(2 * np.pi * (1 / 12) * tt))
    room = band_noise(s, 40, 160) * 0.5
    drips = np.zeros(len(tt))
    for at in (0.9, 2.7, 3.1, 5.6, 7.4, 8.05, 10.3):
        f = rng.uniform(900, 1900)
        d = 0.09
        blip = chirp(d, f, f * 1.7) * decay(d, 40)
        place(drips, blip * rng.uniform(0.5, 1.0), at)
    drips = circular_echo(drips, ((0.19, 0.5), (0.41, 0.33), (0.83, 0.2), (1.37, 0.1)))
    return lap * 0.35 + room * 0.3 + drips * 0.5


def ambient_carpark():
    """Ventilation drone in a concrete box, with far-off ticking metal."""
    s = 12.0
    tt = t(s)
    drone = np.sin(2 * np.pi * 45 * tt) + 0.7 * np.sin(2 * np.pi * 45.5 * tt) + 0.4 * np.sin(2 * np.pi * 90.25 * tt)
    vent = band_noise(s, 60, 420) * (0.8 + 0.2 * np.sin(2 * np.pi * 0.5 * tt))
    ticks = np.zeros(len(tt))
    for at in (1.4, 4.9, 5.3, 9.6):
        d = 0.25
        tick = sum(np.sin(2 * np.pi * f * t(d)) for f in (1730, 2610, 3890)) * decay(d, 30)
        place(ticks, tick * rng.uniform(0.4, 1.0), at)
    ticks = circular_echo(ticks, ((0.31, 0.55), (0.67, 0.4), (1.1, 0.25), (1.9, 0.12)))
    return drone * 0.45 + vent * 0.4 + ticks * 0.06


# ------------------------------------------------------------ one-shots
def light_buzz():
    s = 0.55
    tt = t(s)
    crackle = (rng.random(len(tt)) < 0.004).astype(float)
    crackle = np.convolve(crackle, decay(0.004, 900), mode="same")
    gate = (np.sin(2 * np.pi * 9 * tt + 1) > -0.3).astype(float)
    return (np.sign(np.sin(2 * np.pi * 120 * tt)) * 0.35 + band_noise(s, 3000, 9000) * 0.3) * gate * env(len(tt), 0.01, 0.1) + crackle


def fixture_ambient():
    """A filament that will not quite stop ringing."""
    s = 1.8
    tt = t(s)
    ring = sum(np.sin(2 * np.pi * f * tt) * (1 + 0.5 * np.sin(2 * np.pi * r * tt)) for f, r in ((2093, 7), (3322, 11), (4699, 5)))
    return (ring * 0.3 + np.sin(2 * np.pi * 120 * tt) * 0.2) * env(len(tt), 0.5, 0.9)


def fixture_step():
    """A weighted metal base dragged a few centimetres across the floor."""
    s = 0.42
    n = int(SR * s)
    grit = band_noise(s, 260, 1500) * (0.6 + 0.4 * np.abs(band_noise(s, 8, 40)))
    return grit * env(n, 0.03, 0.25) + np.sin(2 * np.pi * 70 * t(s)) * decay(s, 14) * 0.5


def fixture_attack():
    s = 0.6
    n = int(SR * s)
    pop = band_noise(s, 400, 12000) * decay(s, 28)
    zap = chirp(s, 2400, 180) * decay(s, 9) * (np.sign(np.sin(2 * np.pi * 60 * t(s))) * 0.5 + 0.5)
    click = np.zeros(n)
    click[:40] = 1
    return pop * 0.8 + zap * 0.6 + click


def fixture_hurt():
    s = 0.4
    tt = t(s)
    return sum(np.sin(2 * np.pi * f * tt) * np.exp(-r * tt) for f, r in ((523, 12), (1291, 18), (2210, 26), (3417, 34)))


def fixture_death():
    s = 1.3
    tt = t(s)
    n = len(tt)
    clang = sum(np.sin(2 * np.pi * f * tt * (1 - 0.25 * tt / s)) * np.exp(-r * tt) for f, r in ((392, 4), (911, 6), (1777, 9)))
    glass = np.zeros(n)
    for _ in range(14):
        d = 0.05
        place(glass, np.sin(2 * np.pi * rng.uniform(3500, 8000) * t(d)) * decay(d, 70) * rng.uniform(0.2, 0.7), rng.uniform(0.15, 0.9))
    return clang + glass * 0.6 * env(n, 0, 0.3)


def moth_flutter():
    s = 0.6
    tt = t(s)
    return band_noise(s, 900, 4200) * (0.5 + 0.5 * np.sin(2 * np.pi * 46 * tt)) ** 3 * env(len(tt), 0.12, 0.25)


def moth_hurt():
    s = 0.14
    return chirp(s, 5200, 3900) * env(int(SR * s), 0.005, 0.08)


def moth_death():
    s = 0.34
    return chirp(s, 4600, 1500) * decay(s, 9) + band_noise(s, 1200, 4000) * decay(s, 14) * 0.5


def noclip_hum():
    """The tell: a hum that climbs while the wall decides whether you are solid."""
    s = 2.5
    tt = t(s)
    rise = chirp(s, 70, 150) + 0.5 * chirp(s, 140, 300)
    trem = 1 + 0.6 * (tt / s) * np.sin(2 * np.pi * (4 + 10 * tt / s) * tt)
    return rise * trem * env(len(tt), 0.3, 0.4) + band_noise(s, 2000, 6000) * 0.04 * (tt / s)


def transit():
    s = 1.6
    tt = t(s)
    n = len(tt)
    whoosh = np.zeros(n)
    for k in range(8):  # bandpass sweep built from stacked noise bands
        lo = 200 + k * 500
        whoosh += band_noise(s, lo, lo + 500) * np.exp(-((tt - (0.15 + k * 0.08)) ** 2) / 0.02)
    sub = chirp(s, 110, 32) * decay(s, 3)
    out = np.concatenate([whoosh * 0.7 + sub, np.zeros(int(0.6 * SR))])
    return circular_echo(out, ((0.11, 0.4), (0.23, 0.25), (0.47, 0.12)))


def key_fob_beep():
    s = 0.5
    n = int(SR * s)
    out = np.zeros(n)
    for at in (0.0, 0.17):
        d = 0.09
        tone = np.sign(np.sin(2 * np.pi * 2450 * t(d))) * 0.5 + np.sin(2 * np.pi * 2450 * t(d))
        place(out, tone * env(int(SR * d), 0.004, 0.02), at)
    return circular_echo(out, ((0.06, 0.2), (0.13, 0.1)))


def main():
    print("synthesising sounds ->", OUT)
    write("ambient/level_0", ambient_level_0(), 0.5)
    write("ambient/poolrooms", ambient_poolrooms(), 0.6)
    write("ambient/carpark", ambient_carpark(), 0.6)
    write("block/light_buzz", light_buzz(), 0.5)
    write("entity/fixture_ambient", fixture_ambient(), 0.6)
    write("entity/fixture_step", fixture_step())
    write("entity/fixture_attack", fixture_attack(), 0.9)
    write("entity/fixture_hurt", fixture_hurt())
    write("entity/fixture_death", fixture_death())
    write("entity/moth_flutter", moth_flutter(), 0.5)
    write("entity/moth_hurt", moth_hurt(), 0.6)
    write("entity/moth_death", moth_death(), 0.6)
    write("event/noclip_hum", noclip_hum(), 0.7)
    write("event/transit", transit(), 0.8)
    write("item/key_fob_beep", key_fob_beep(), 0.6)


if __name__ == "__main__":
    main()
