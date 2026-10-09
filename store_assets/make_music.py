"""Five cozy background tracks (about two minutes each), a soft UI click and a gentle train horn.
All synthesized here, so there are no licensing issues. Needs numpy and ffmpeg.
Run: python3 store_assets/make_music.py
The game plays the tracks in random order, one after another."""
import os, subprocess, wave
import numpy as np

SR = 32000
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def hz(m): return 440.0 * 2 ** ((m - 69) / 12)


def tvec(dur): return np.arange(int(SR * dur)) / SR


# ---------------- instruments ----------------
def musicbox(f, dur, vol):
    t = tvec(dur)
    env = np.exp(-t * 3.0) * np.minimum(1, t * 500)
    return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 5) + 0.1 * np.sin(2 * np.pi * f * 3.01 * t) * np.exp(-t * 9)) * env * vol


def epiano(f, dur, vol):
    t = tvec(dur)
    env = np.exp(-t * 1.6) * np.minimum(1, t * 200)
    trem = 1 + 0.08 * np.sin(2 * np.pi * 4.5 * t)
    return (np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * f * 2 * t) * np.exp(-t * 3) + 0.08 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 6)) * env * trem * vol


def marimba(f, dur, vol):
    t = tvec(min(dur, 1.2))
    env = np.exp(-t * 6) * np.minimum(1, t * 800)
    return (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 18)) * env * vol


_PLUCK = {}


def pluck(f, dur, vol, rng=np.random.default_rng(3)):
    key = (round(f, 2), round(dur, 2))
    if key in _PLUCK:
        return _PLUCK[key] * vol
    n = int(SR * dur)
    period = max(2, int(SR / f))
    buf = rng.uniform(-1, 1, period)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % period]
        buf[i % period] = 0.497 * (buf[i % period] + buf[(i + 1) % period])
    out = out * np.minimum(1, np.arange(n) / 40)
    _PLUCK[key] = out
    return out * vol


def flute(f, dur, vol, rng=np.random.default_rng(5)):
    t = tvec(dur)
    a = np.minimum(1, t / 0.12) * np.minimum(1, (dur - t) / 0.25)
    vib = np.sin(2 * np.pi * 5 * t) * 0.004 * np.minimum(1, t / 0.4)
    ph = 2 * np.pi * f * np.cumsum(1 + vib) / SR
    breath = np.convolve(rng.standard_normal(len(t)), np.ones(30) / 30, 'same') * 0.05
    return (np.sin(ph) + 0.12 * np.sin(2 * ph) + breath) * a * vol


def pad(f, dur, vol):
    t = tvec(dur)
    a = np.minimum(1, t / 0.9) * np.minimum(1, (dur - t) / 0.9)
    return sum(np.sin(2 * np.pi * f * (1 + d) * t) for d in (-0.003, 0, 0.003)) / 3 * a * vol


def bass(f, dur, vol):
    t = tvec(dur)
    return np.sin(2 * np.pi * f * t) * np.minimum(1, t * 60) * np.exp(-t * 1.2) * vol


def brush(dur, vol, rng=np.random.default_rng(9)):
    t = tvec(dur)
    n = rng.standard_normal(len(t))
    n = n - np.convolve(n, np.ones(8) / 8, 'same')
    return n * np.exp(-t * 30) * vol


INSTR = {'musicbox': musicbox, 'epiano': epiano, 'marimba': marimba, 'pluck': pluck, 'flute': flute}


def place(buf, sig, start):
    i = int(start * SR)
    if i < 0:
        sig = sig[-i:]; i = 0
    n = min(len(sig), len(buf) - i)
    if n > 0:
        buf[i:i + n] += sig[:n]


def reverb(x, wet):
    out = x.copy()
    for delay, g in ((0.113, .45), (0.167, .35), (0.241, .28), (0.331, .2), (0.457, .12)):
        d = int(delay * SR)
        out[d:] += x[:-d] * g * wet
    return out


def save(name, x, q=2):
    os.makedirs(OUT, exist_ok=True)
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", str(q), os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(wav)
    print("saved", name, round(len(x) / SR, 1), "s")


# ---------------- tracks ----------------
MAJ = [0, 2, 4, 5, 7, 9, 11]
RHYTHMS = [[0, 1, 2, 3], [0, 1.5, 2, 3], [0, 2, 2.5, 3], [0, 1, 1.5, 2.5, 3], [0, 2], [0, 0.5, 1, 2, 3], [0, 1, 3]]


def track(name, key, bpm, prog, lead, comp, seed, swing=0.0, drums=False, bars=36, lead_oct=0):
    rng = np.random.default_rng(seed)
    beat = 60 / bpm
    bar = beat * 4
    length = bar * bars
    buf = np.zeros(int(SR * (length + 4)))
    scale = [key + 60 + lead_oct + s for s in MAJ] + [key + 72 + lead_oct + s for s in MAJ]
    # a two-bar motif that is reused (shifted to the chord) so the melody feels like a tune
    motif = [(rng.choice(len(RHYTHMS)), rng.integers(0, 5)) for _ in range(2)]
    for b in range(bars):
        root, quality = prog[b % len(prog)]
        r = key + root
        chord = [r + 48, r + 52 if quality == 'M' else r + 51, r + 55, r + 59 if quality == 'M' else r + 58]
        t0 = b * bar
        section = (b // 8) % 4                       # A, B, A', outro feel
        # pad + bass
        for n in chord[:3]:
            place(buf, pad(hz(n + 12), bar + 0.9, 0.07), t0 - 0.45)
        place(buf, bass(hz(r + 36), bar * 0.6, 0.45), t0)
        place(buf, bass(hz(r + 43), bar * 0.4, 0.25), t0 + bar * 0.5)
        # accompaniment: arpeggio on eighths (sparser in the B section)
        for k in range(8):
            if section == 1 and k % 2:
                continue
            if rng.random() < 0.85:
                note = chord[[0, 2, 1, 3, 2, 1, 3, 2][k]] + 12
                sw = swing * beat / 2 if k % 2 else 0
                place(buf, INSTR[comp](hz(note), 1.4, 0.13), t0 + k * beat / 2 + sw)
        # melody: motif in A sections, freer in B
        if section in (0, 2) or rng.random() < 0.7:
            ri, start = motif[b % 2] if section != 1 else (rng.integers(0, len(RHYTHMS)), rng.integers(0, 7))
            pos = int(start) + (root // 2) % 3
            for j, beat_pos in enumerate(RHYTHMS[ri]):
                strong = beat_pos in (0, 2)
                if strong:   # land on a chord tone
                    targets = [i for i, n in enumerate(scale) if (n - r) % 12 in (0, 4 if quality == 'M' else 3, 7)]
                    pos = min(targets, key=lambda i: abs(i - pos))
                else:
                    pos = int(np.clip(pos + rng.choice([-2, -1, 1, 1, 2]), 0, len(scale) - 1))
                dur = 1.6 if lead != 'flute' else beat * 1.1
                place(buf, INSTR[lead](hz(scale[pos]), dur, 0.2 if lead != 'flute' else 0.13), t0 + beat_pos * beat)
        if drums:
            for k in range(8):
                place(buf, brush(0.12, 0.05 if k % 2 else 0.08), t0 + k * beat / 2 + (swing * beat / 2 if k % 2 else 0))
    buf = reverb(buf, 0.5)[: int(SR * (length + 2))]
    fade = int(SR * 3)
    buf[:int(SR * 0.5)] *= np.linspace(0, 1, int(SR * 0.5))
    buf[-fade:] *= np.linspace(1, 0, fade)
    save(name, buf / (np.max(np.abs(buf)) + 1e-9) * 0.55)


if __name__ == "__main__":
    # (root offset from the key, chord quality)
    track("music_1", 0, 72, [(0, 'M'), (9, 'm'), (5, 'M'), (7, 'M')], 'musicbox', 'musicbox', 1)
    track("music_2", 5, 84, [(0, 'M'), (4, 'm'), (9, 'm'), (5, 'M'), (2, 'm'), (7, 'M'), (0, 'M'), (7, 'M')], 'epiano', 'pluck', 2, swing=0.3, drums=True)
    track("music_3", 7, 66, [(0, 'M'), (5, 'M'), (9, 'm'), (5, 'M')], 'flute', 'epiano', 3, lead_oct=0)
    track("music_4", 2, 92, [(0, 'M'), (7, 'M'), (9, 'm'), (5, 'M')], 'marimba', 'marimba', 4, drums=True, lead_oct=12)
    track("music_5", 3, 76, [(5, 'M'), (7, 'M'), (4, 'm'), (9, 'm'), (2, 'm'), (7, 'M'), (0, 'M'), (0, 'M')], 'pluck', 'musicbox', 5, swing=0.2, lead_oct=0)

    # a soft, woody UI click
    t = tvec(0.09)
    click = (np.sin(2 * np.pi * 900 * t) * 0.6 + np.sin(2 * np.pi * 1450 * t) * 0.25) * np.exp(-t * 70) * np.minimum(1, t * 3000)
    save("sfx_click", click * 0.5, q=4)
    # a gentle two-tone train horn
    t = tvec(1.8)
    env = np.minimum(1, t / 0.25) * np.minimum(1, (1.8 - t) / 0.6)
    horn = sum(np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 2 * t) + 0.15 * np.sin(2 * np.pi * f * 3 * t) for f in (311.1, 392.0)) / 2
    horn = reverb(horn * env, 0.6)
    save("sfx_horn", horn / np.max(np.abs(horn)) * 0.6, q=4)
