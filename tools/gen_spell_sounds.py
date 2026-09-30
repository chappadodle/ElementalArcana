"""Synthesizes spell sound effects from scratch (numpy + scipy, converted to .ogg with sox).

Run from the project root:  python3 tools/gen_spell_sounds.py [icicle] [fireball] [frost] [wind] [water]
Each sound is layered from small building blocks (clicks, crackles, bell-like "tinkles", low
thuds) with fixed random seeds, so re-running gives identical files. No samples are used, so
there is nothing to license.
"""
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44100
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/sounds"


# ---- building blocks ----

def times(duration):
    return np.arange(int(duration * SR)) / SR


def silence(duration):
    return np.zeros(int(duration * SR))


def place(buffer, sound, at):
    start = int(at * SR)
    end = min(len(buffer), start + len(sound))
    if start < end:
        buffer[start:end] += sound[:end - start]


def filtered(signal, kind, freq, order=4):
    sos = butter(order, freq, btype=kind, fs=SR, output="sos")
    return sosfilt(sos, signal)


def click(rng, amp=1.0, length=0.004, brightness=3000):
    """A tiny tick: the grain of every crackle. Band-limited so crackles sound crisp, not like static."""
    tick = rng.standard_normal(int(length * SR)) * np.exp(-times(length) / (length / 4))
    return filtered(tick, "bandpass", [brightness, min(brightness * 3.5, 11000)], order=2) * amp


def crackle(rng, duration, count, amp_range=(0.2, 0.6), shape=lambda x: x, brightness=3000):
    """Clicks scattered over time; `shape` maps a uniform 0..1 draw to where along the sound it lands."""
    out = silence(duration + 0.01)
    for _ in range(count):
        at = shape(rng.random()) * duration
        place(out, click(rng, rng.uniform(*amp_range), brightness=brightness), at)
    return out[:int(duration * SR)]


def tinkle(rng, freq, duration, tau, amp=1.0):
    """A small struck-crystal tone: inharmonic partials (like a tiny bell) with fast attack."""
    t = times(duration)
    out = np.zeros_like(t)
    for ratio, weight, decay in ((1.0, 1.0, 1.0), (2.76, 0.5, 0.7), (5.40, 0.25, 0.5), (8.93, 0.12, 0.35)):
        detune = 1 + rng.uniform(-0.004, 0.004)
        out += weight * np.sin(2 * np.pi * freq * ratio * detune * t) * np.exp(-t / (tau * decay))
    attack = np.minimum(1, t / 0.002)
    return out * attack * amp / 1.9


def thud(duration, f0, f1, tau, amp=1.0):
    """A low body hit with a quick downward pitch drop."""
    t = times(duration)
    freq = f1 + (f0 - f1) * np.exp(-t / 0.03)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    return np.sin(phase) * np.exp(-t / tau) * np.minimum(1, t / 0.002) * amp


def normalize(signal, peak_db):
    peak = np.max(np.abs(signal))
    return signal if peak == 0 else signal / peak * 10 ** (peak_db / 20)


def fade_out(signal, duration):
    n = min(len(signal), int(duration * SR))
    signal[-n:] *= np.linspace(1, 0, n)
    return signal


def write(name, signal, sox_effects=(), peak_db=-1.0):
    path = OUT / f"{name}.ogg"
    path.parent.mkdir(parents=True, exist_ok=True)
    signal = normalize(signal, peak_db)
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        with wave.open(tmp.name, "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(SR)
            wav.writeframes((np.clip(signal, -1, 1) * 32767).astype(np.int16).tobytes())
        subprocess.run(["sox", tmp.name, str(path), *sox_effects], check=True)
    print("wrote", path.relative_to(OUT.parent.parent.parent.parent.parent.parent))


# ---- sounds ----

def icicle_impact(variant, seed):
    """Shatter: a sharp transient, a bright glassy crash, shards tinkling down, and a low thud."""
    rng = np.random.default_rng(seed)
    duration = 0.9
    out = silence(duration)
    for i in range(3):
        place(out, click(rng, 1.0, length=0.006, brightness=2000), i * rng.uniform(0.002, 0.004))
    crash = filtered(rng.standard_normal(int(0.4 * SR)), "highpass", 2500) * np.exp(-times(0.4) / 0.06)
    place(out, crash * 0.7, 0.0)
    for _ in range(rng.integers(10, 15)):
        at = min(rng.exponential(0.07), 0.3)
        amp = rng.uniform(0.15, 0.4) * (1 - at / 0.4)
        place(out, tinkle(rng, rng.uniform(2500, 7500), 0.5, rng.uniform(0.05, 0.25), amp=amp), at)
    body = filtered(thud(0.3, 110, 55, 0.08, amp=0.5), "lowpass", 400, order=2)
    place(out, body, 0.0)
    place(out, crackle(rng, 0.35, 18, amp_range=(0.05, 0.18), shape=lambda x: x ** 2), 0.05)
    write(f"spell/icicle/impact{variant}", fade_out(out, 0.1), ["reverb", "35"])


# ---- fireball signature sounds ----

def noise(rng, duration):
    return rng.standard_normal(int(duration * SR))


def seamless(signal, overlap):
    """Makes a loop: the last `overlap` seconds are crossfaded into the start, then dropped."""
    n = int(overlap * SR)
    body = signal[:-n].copy()
    ramp = np.linspace(0, 1, n)
    body[:n] = body[:n] * ramp + signal[-n:] * (1 - ramp)
    return body


def smooth_random(rng, duration, rate, lo=0.0, hi=1.0):
    """A slowly wandering control curve (for swells and flutter): random points joined smoothly."""
    points = rng.uniform(lo, hi, int(duration * rate) + 3)
    x = np.linspace(0, len(points) - 1, int(duration * SR))
    i = np.floor(x).astype(int)
    f = x - i
    f = f * f * (3 - 2 * f)
    return points[i] * (1 - f) + points[np.minimum(i + 1, len(points) - 1)] * f


def sweep(duration, f_start, f_end, curve=1.0):
    """A sine whose pitch glides from f_start to f_end."""
    t = times(duration) / duration
    freq = f_start + (f_end - f_start) * t ** curve
    return np.sin(2 * np.pi * np.cumsum(freq) / SR)


def sun_hum(seed=71):
    """Sunfire held: a deep solar hum, two beating low tones with harmonics, breathing slowly,
    over a soft plasma roar and a faint high sizzle. Loops seamlessly."""
    rng = np.random.default_rng(seed)
    duration = 4.5
    t = times(duration)
    tone = np.zeros_like(t)
    for freq, weight in ((55, 1.0), (55.5, 0.8), (110, 0.45), (165.25, 0.25), (220, 0.15)):
        tone += weight * np.sin(2 * np.pi * freq * t + rng.uniform(0, 6.28))
    breathing = 0.75 + 0.25 * np.sin(2 * np.pi * 0.5 * t)
    roar = filtered(noise(rng, duration), "lowpass", 380) * smooth_random(rng, duration, 3, 0.5, 1.0) * 1.2
    sizzle = filtered(noise(rng, duration), "bandpass", [3500, 7000], order=2) * 0.06
    sizzle += crackle(rng, duration, 40, amp_range=(0.02, 0.07), brightness=4000)
    out = tone * breathing * 0.5 + roar + sizzle
    write("spell/fireball/sun_hum", seamless(out, 0.5))


def sun_launch(seed=72):
    """Sunfire thrown: a rising surge that bursts into a heavy push of air."""
    rng = np.random.default_rng(seed)
    duration = 1.6
    out = silence(duration)
    rise = 0.35
    swell = sweep(rise, 70, 240, curve=2) * np.linspace(0, 1, int(rise * SR)) ** 2
    swell_noise = filtered(noise(rng, rise), "bandpass", [300, 2500], order=2) * np.linspace(0, 1, int(rise * SR)) ** 3
    place(out, swell * 0.5 + swell_noise * 0.6, 0.0)
    place(out, thud(0.6, 140, 45, 0.18, amp=1.0), rise)
    push = filtered(noise(rng, 1.2), "lowpass", 1400) * np.exp(-times(1.2) / 0.35)
    place(out, push * 0.9, rise)
    place(out, crackle(rng, 0.8, 30, amp_range=(0.05, 0.2), shape=lambda x: x ** 1.5, brightness=2500), rise)
    write("spell/fireball/sun_launch", fade_out(out, 0.3), ["reverb", "40"], peak_db=-3.0)


def sun_roar(seed=73):
    """Sunfire in flight: a heavy roar of fire with a low rumble and flutter. Loops seamlessly."""
    rng = np.random.default_rng(seed)
    duration = 3.5
    flutter = 0.7 + 0.3 * smooth_random(rng, duration, 12)
    roar = filtered(noise(rng, duration), "bandpass", [80, 900], order=2) * flutter
    rumble = filtered(noise(rng, duration), "lowpass", 90) * 2.5
    hiss = filtered(noise(rng, duration), "highpass", 2500) * 0.08 * flutter
    out = roar + rumble + hiss + crackle(rng, duration, 50, amp_range=(0.04, 0.15), brightness=1800)
    write("spell/fireball/sun_roar", seamless(out, 0.4))


def sun_blast(seed=74):
    """Sunfire exploding: a sharp crack, a huge falling boom, and a long rolling thunder tail."""
    rng = np.random.default_rng(seed)
    duration = 6.0
    out = silence(duration)
    for i in range(4):
        place(out, click(rng, 1.0, length=0.01, brightness=1200), i * 0.004)
    place(out, thud(1.5, 90, 28, 0.6, amp=1.3), 0.0)
    body_len = 1.5
    body = noise(rng, body_len)
    # The blast's roar darkens as it rolls out: filter it in slices from bright to dark.
    darkened = np.zeros_like(body)
    slices = 12
    for k in range(slices):
        a, b = k * len(body) // slices, (k + 1) * len(body) // slices
        cutoff = 2500 * (1 - k / slices) ** 2 + 180
        darkened[a:b] = filtered(body, "lowpass", cutoff, order=2)[a:b]
    place(out, darkened * np.exp(-times(body_len) / 0.45) * 1.1, 0.0)
    tail_len = 5.5
    swells = smooth_random(rng, tail_len, 2.5, 0.2, 1.0) ** 2
    tail = filtered(noise(rng, tail_len), "lowpass", 160) * swells * np.exp(-times(tail_len) / 2.2) * 6.0
    place(out, tail, 0.3)
    place(out, crackle(rng, 2.0, 60, amp_range=(0.03, 0.15), shape=lambda x: x ** 2, brightness=1500), 0.1)
    write("spell/fireball/sun_blast", fade_out(out, 0.8), ["reverb", "60"], peak_db=-3.0)


def meteor_roar(seed=81):
    """Meteor falling: a rough, rumbling roar with an airy whistle and tumbling rock. Loops seamlessly."""
    rng = np.random.default_rng(seed)
    duration = 3.0
    flutter = 0.75 + 0.25 * smooth_random(rng, duration, 8)
    roar = filtered(noise(rng, duration), "bandpass", [150, 800], order=2) * flutter
    rumble = filtered(noise(rng, duration), "lowpass", 70) * 2.0
    whistle = filtered(noise(rng, duration), "bandpass", [1150, 1300], order=2) * 1.2 * smooth_random(rng, duration, 2, 0.6, 1.0)
    rocks = crackle(rng, duration, 45, amp_range=(0.05, 0.2), brightness=700)
    out = roar + rumble + whistle + rocks
    write("spell/fireball/meteor_roar", seamless(out, 0.4))


def meteor_impact(seed=82):
    """Meteor landing: a ground-shaking thump, a crunch of rock, debris raining down, and a rumble."""
    rng = np.random.default_rng(seed)
    duration = 4.0
    out = silence(duration)
    place(out, thud(1.2, 70, 24, 0.5, amp=1.4), 0.0)
    crunch = filtered(noise(rng, 0.5), "bandpass", [250, 3000], order=2) * np.exp(-times(0.5) / 0.08)
    place(out, crunch * 1.0, 0.0)
    for _ in range(8):
        place(out, click(rng, rng.uniform(0.5, 1.0), length=0.012, brightness=600), rng.uniform(0, 0.05))
    place(out, crackle(rng, 2.2, 90, amp_range=(0.05, 0.3), shape=lambda x: x ** 1.8, brightness=900), 0.08)
    rumble = filtered(noise(rng, 3.8), "lowpass", 110) * np.exp(-times(3.8) / 1.3) * 3.5
    place(out, rumble, 0.05)
    write("spell/fireball/meteor_impact", fade_out(out, 0.5), ["reverb", "45"], peak_db=-3.0)


def icicle():
    icicle_impact(1, 41)
    icicle_impact(2, 42)
    icicle_impact(3, 43)


def fireball():
    sun_hum()
    sun_launch()
    sun_roar()
    sun_blast()
    meteor_roar()
    meteor_impact()


# ---- icicle signature sounds ----

def lance_forge(seed=101):
    """Glacial Lance forming: the icicles' chimes climb in a shimmer and fuse into one ringing tone."""
    rng = np.random.default_rng(seed)
    duration = 1.8
    out = silence(duration)
    for i in range(14):
        at = 0.6 * (i / 14) ** 0.8
        freq = 900 * 2 ** (i / 7) * rng.uniform(0.98, 1.02)
        place(out, tinkle(rng, freq, 0.6, 0.18, amp=0.35 + 0.02 * i), at)
    t = times(1.2)
    ring = sum(w * np.sin(2 * np.pi * f * t) for f, w in ((880, 1.0), (1320, 0.5), (1760, 0.35), (2640, 0.15)))
    ring *= np.minimum(1, t / 0.02) * np.exp(-t / 0.5) * 0.6
    place(out, ring, 0.55)
    place(out, thud(0.4, 180, 90, 0.12, amp=0.4), 0.55)
    write("spell/icicle/lance_forge", fade_out(out, 0.2), ["reverb", "50"], peak_db=-3.0)


def lance_launch(seed=102):
    """Glacial Lance thrown: a sharp icy crack and a deep, heavy rush of air."""
    rng = np.random.default_rng(seed)
    duration = 1.3
    out = silence(duration)
    for i in range(3):
        place(out, click(rng, 1.0, length=0.006, brightness=3500), i * 0.003)
    crack = filtered(noise(rng, 0.25), "highpass", 3000) * np.exp(-times(0.25) / 0.04)
    place(out, crack * 0.6, 0.0)
    rush_len = 1.2
    envelope = np.minimum(1, times(rush_len) / 0.08) * np.exp(-times(rush_len) / 0.35)
    rush = filtered(noise(rng, rush_len), "bandpass", [120, 900], order=2) * envelope
    place(out, rush * 1.2, 0.02)
    place(out, thud(0.5, 120, 45, 0.15, amp=0.7), 0.0)
    write("spell/icicle/lance_launch", fade_out(out, 0.2), ["reverb", "35"], peak_db=-3.0)


def lance_quake(seed=103):
    """Glacial Lance landing: a splitting crack, ice spikes bursting up in a cascade of breaking
    glass-like shards, and a low rumble through the ground."""
    rng = np.random.default_rng(seed)
    duration = 4.0
    out = silence(duration)
    for i in range(5):
        place(out, click(rng, 1.0, length=0.012, brightness=900), i * 0.004)
    place(out, thud(1.2, 80, 26, 0.45, amp=1.2), 0.0)
    crack = filtered(noise(rng, 0.6), "bandpass", [800, 6000], order=2) * np.exp(-times(0.6) / 0.09)
    place(out, crack * 0.9, 0.0)
    # The spikes erupting one after another, each a small crash with shards.
    for k in range(9):
        at = 0.05 + k * 0.045 + rng.uniform(0, 0.02)
        burst = filtered(noise(rng, 0.3), "highpass", 2000) * np.exp(-times(0.3) / 0.05)
        place(out, burst * rng.uniform(0.3, 0.5), at)
        for _ in range(4):
            place(out, tinkle(rng, rng.uniform(2500, 7000), 0.4, rng.uniform(0.05, 0.2), amp=rng.uniform(0.1, 0.25)),
                  at + rng.uniform(0, 0.1))
    place(out, crackle(rng, 1.5, 70, amp_range=(0.05, 0.2), shape=lambda x: x ** 1.6, brightness=2500), 0.4)
    rumble = filtered(noise(rng, 3.6), "lowpass", 100) * np.exp(-times(3.6) / 1.2) * 3.0
    place(out, rumble, 0.05)
    write("spell/icicle/lance_quake", fade_out(out, 0.5), ["reverb", "55"], peak_db=-3.0)


def winter_blizzard(seed=104):
    """Endless Winter: a howling blizzard wind, its pitch rising and falling in gusts. Loops."""
    rng = np.random.default_rng(seed)
    duration = 4.5
    base = noise(rng, duration)
    # A howl: noise through a band that sweeps up and down, slice by slice.
    sweep = smooth_random(rng, duration, 1.5, 0, 1)
    howl = np.zeros_like(base)
    slices = 90
    for k in range(slices):
        a, b = k * len(base) // slices, (k + 1) * len(base) // slices
        centre = 350 + 900 * sweep[(a + b) // 2]
        band = filtered(base[max(0, a - 2000):b], "bandpass", [centre * 0.8, centre * 1.25], order=2)
        howl[a:b] = band[-(b - a):]
    gusts = 0.5 + 0.5 * smooth_random(rng, duration, 2, 0, 1)
    hiss = filtered(noise(rng, duration), "highpass", 3000) * 0.12
    low = filtered(noise(rng, duration), "lowpass", 200) * 0.8
    out = howl * gusts * 1.4 + hiss * gusts + low
    write("spell/icicle/winter_blizzard", seamless(out, 0.5))


def frost():
    lance_forge()
    lance_launch()
    lance_quake()
    winter_blizzard()


# ---- wind blade signature sounds ----

def swish(rng, duration, f_start, f_end, amp=1.0):
    """A blade cutting air: noise through a narrow band that sweeps from f_start to f_end, with a
    sharp attack and a quick tail."""
    base = noise(rng, duration)
    out = np.zeros_like(base)
    slices = 24
    for k in range(slices):
        a, b = k * len(base) // slices, (k + 1) * len(base) // slices
        centre = f_start + (f_end - f_start) * (k / slices) ** 0.7
        band = filtered(base[max(0, a - 1500):b], "bandpass", [centre * 0.75, min(centre * 1.3, 20000)], order=2)
        out[a:b] = band[-(b - a):]
    t = times(duration)
    return out * np.minimum(1, t / 0.008) * np.exp(-t / (duration * 0.35)) * amp


def scythe_roar(seed=111):
    """Storm Scythe in flight: a dark howling storm wind with crackles of static. Loops."""
    rng = np.random.default_rng(seed)
    duration = 4.0
    sweep = smooth_random(rng, duration, 2, 0, 1)
    base = noise(rng, duration)
    howl = np.zeros_like(base)
    slices = 80
    for k in range(slices):
        a, b = k * len(base) // slices, (k + 1) * len(base) // slices
        centre = 180 + 500 * sweep[(a + b) // 2]
        band = filtered(base[max(0, a - 2000):b], "bandpass", [centre * 0.75, centre * 1.3], order=2)
        howl[a:b] = band[-(b - a):]
    rumble = filtered(noise(rng, duration), "lowpass", 90) * 1.6
    static = crackle(rng, duration, 70, amp_range=(0.05, 0.25), brightness=5000)
    out = howl * 1.5 + rumble + static
    write("spell/wind_blade/scythe_roar", seamless(out, 0.5))


def scythe_strike(seed=112):
    """Storm Scythe striking: a thunder-crack of lightning, a heavy blast of wind, and a rolling tail."""
    rng = np.random.default_rng(seed)
    duration = 3.0
    out = silence(duration)
    for i in range(6):
        place(out, click(rng, 1.0, length=0.008, brightness=2500), i * 0.006)
    crack = filtered(noise(rng, 0.4), "highpass", 1500) * np.exp(-times(0.4) / 0.06)
    place(out, crack * 0.9, 0.0)
    place(out, swish(rng, 0.7, 2500, 250, amp=1.0), 0.01)
    place(out, thud(0.8, 90, 35, 0.3, amp=0.9), 0.0)
    tail = filtered(noise(rng, 2.6), "lowpass", 180) * smooth_random(rng, 2.6, 3, 0.3, 1.0) * np.exp(-times(2.6) / 0.9) * 2.0
    place(out, tail, 0.15)
    write("spell/wind_blade/scythe_strike", fade_out(out, 0.4), ["reverb", "45"], peak_db=-3.0)


def thousand_slash(seed=113):
    """Thousand Cuts: three razor-thin swishes in quick succession."""
    rng = np.random.default_rng(seed)
    duration = 0.6
    out = silence(duration)
    for k, (at, f0, f1) in enumerate(((0.0, 9000, 3000), (0.07, 10000, 3500), (0.13, 8500, 2800))):
        place(out, swish(rng, 0.22, f0, f1, amp=1.0 - 0.15 * k), at)
    write("spell/wind_blade/thousand_slash", fade_out(out, 0.1), ["reverb", "20"], peak_db=-3.0)


def wind():
    scythe_roar()
    scythe_strike()
    thousand_slash()


# ---- hydro jet signature sounds ----

def gurgle(rng, duration, count, low=300, high=900, amp=0.25):
    """Bubbles: short sine blips whose pitch rises as they pop, scattered through the sound."""
    out = silence(duration + 0.1)
    for _ in range(count):
        length = rng.uniform(0.03, 0.08)
        t = times(length)
        f0 = rng.uniform(low, high)
        freq = f0 * (1 + 1.5 * t / length)
        blip = np.sin(2 * np.pi * np.cumsum(freq) / SR) * np.sin(np.pi * t / length) * rng.uniform(0.4, 1.0)
        place(out, blip * amp, rng.uniform(0, duration))
    return out[:int(duration * SR)]


def lance_surge(seed=121):
    """Tsunami Lance thrown: a rush of water swelling and surging forward, full of bubbles."""
    rng = np.random.default_rng(seed)
    duration = 1.4
    base = noise(rng, duration)
    out = np.zeros_like(base)
    slices = 30
    for k in range(slices):
        a, b = k * len(base) // slices, (k + 1) * len(base) // slices
        centre = 250 + 1100 * (k / slices) ** 0.6
        band = filtered(base[max(0, a - 1500):b], "bandpass", [centre * 0.6, centre * 1.6], order=2)
        out[a:b] = band[-(b - a):]
    t = times(duration)
    envelope = np.minimum(1, t / 0.25) * np.exp(-np.maximum(0, t - 0.3) / 0.35)
    out = out * envelope * 1.3 + gurgle(rng, duration, 25) * envelope
    place(out, thud(0.5, 110, 50, 0.15, amp=0.5), 0.2)
    write("spell/hydro_jet/lance_surge", fade_out(out, 0.2), ["reverb", "35"], peak_db=-3.0)


def lance_crash(seed=122):
    """Tsunami Lance landing: a wave crashing down, a deep thump of water, a spray of droplets and
    a long hiss as it washes out."""
    rng = np.random.default_rng(seed)
    duration = 3.5
    out = silence(duration)
    place(out, thud(0.9, 90, 35, 0.35, amp=1.0), 0.0)
    crash = filtered(noise(rng, 1.2), "bandpass", [300, 5000], order=2) * np.exp(-times(1.2) / 0.25)
    place(out, crash * 1.1, 0.0)
    place(out, crackle(rng, 1.5, 120, amp_range=(0.03, 0.15), shape=lambda x: x ** 1.5, brightness=3500), 0.05)
    wash = filtered(noise(rng, 3.2), "bandpass", [1500, 7000], order=2) * smooth_random(rng, 3.2, 3, 0.4, 1.0) * np.exp(-times(3.2) / 1.1) * 0.5
    place(out, wash, 0.2)
    place(out, gurgle(rng, 2.0, 30, 200, 600, amp=0.2), 0.3)
    write("spell/hydro_jet/lance_crash", fade_out(out, 0.4), ["reverb", "50"], peak_db=-3.0)


def maelstrom_swirl(seed=123):
    """Maelstrom: a deep, churning whirlpool, water rushing round and round, with bubbles. Loops."""
    rng = np.random.default_rng(seed)
    duration = 4.0
    t = times(duration)
    churn = filtered(noise(rng, duration), "bandpass", [80, 600], order=2)
    # Round and round: a slow, slightly uneven swell.
    swirl = 0.65 + 0.35 * np.sin(2 * np.pi * 0.75 * t + 0.4 * np.sin(2 * np.pi * 0.25 * t))
    rush = filtered(noise(rng, duration), "bandpass", [700, 2500], order=2) * 0.3
    out = churn * swirl * 1.4 + rush * swirl + gurgle(rng, duration, 60, 150, 500, amp=0.3)
    write("spell/hydro_jet/maelstrom_swirl", seamless(out, 0.5))


def water():
    lance_surge()
    lance_crash()
    maelstrom_swirl()


GROUPS = {"icicle": icicle, "fireball": fireball, "frost": frost, "wind": wind, "water": water}


def main(names):
    """Writes the named groups of sounds (all of them when none are named). The .ogg encoder isn't
    byte-for-byte repeatable, so regenerate only what changed."""
    for name in names or GROUPS:
        GROUPS[name]()

if __name__ == "__main__":
    import sys
    main(sys.argv[1:])
