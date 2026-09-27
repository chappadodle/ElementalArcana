"""Synthesizes spell sound effects from scratch (numpy + scipy, converted to .ogg with sox).

Run from the project root:  python3 tools/gen_spell_sounds.py
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


def main():
    icicle_impact(1, 41)
    icicle_impact(2, 42)
    icicle_impact(3, 43)


if __name__ == "__main__":
    main()
