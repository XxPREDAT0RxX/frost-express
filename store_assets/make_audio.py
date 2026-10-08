"""Generates the game's music and sound effects as OGG files (all synthesized, no licensing issues).
Needs: numpy and ffmpeg. Run: python store_assets/make_audio.py
Edit the chords / notes / tempo below to change the sound."""
import subprocess, wave, os
import numpy as np

SR = 32000
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
os.makedirs(OUT, exist_ok=True)

def hz(m): return 440.0 * 2 ** ((m - 69) / 12)

def bell(freq, dur, vol=1.0):
    t = np.arange(int(SR * dur)) / SR
    env = np.exp(-t * 3.2) * np.minimum(1, t * 400)
    s = np.sin(2*np.pi*freq*t) + 0.35*np.sin(2*np.pi*freq*2*t)*np.exp(-t*5) + 0.12*np.sin(2*np.pi*freq*3.01*t)*np.exp(-t*8)
    return s * env * vol

def pad(freq, dur, vol=1.0):
    t = np.arange(int(SR * dur)) / SR
    a = np.minimum(1, t / 0.8) * np.minimum(1, (dur - t) / 0.8)
    s = sum(np.sin(2*np.pi*freq*(1+d)*t) for d in (-0.003, 0.0, 0.003))
    return s / 3 * a * vol

def bass(freq, dur, vol=1.0):
    t = np.arange(int(SR * dur)) / SR
    env = np.minimum(1, t * 60) * np.exp(-t * 1.1)
    return np.sin(2*np.pi*freq*t) * env * vol

def place(buf, sig, start):
    i = int(start * SR)
    n = min(len(sig), len(buf) - i)
    if n > 0: buf[i:i+n] += sig[:n]

def reverb(x, wet=0.35):
    out = x.copy()
    for delay, g in ((0.113, .45), (0.167, .35), (0.241, .28), (0.331, .2)):
        d = int(delay * SR)
        out[d:] += x[:-d] * g * wet
    return out

def norm(x, peak=0.8):
    return x / (np.max(np.abs(x)) + 1e-9) * peak

def save(name, x):
    wav = os.path.join(OUT, name + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "3", os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(wav)

# ---- background music: cozy chord loop, 8 bars at 72 bpm ----
BPM = 72
beat = 60 / BPM
bar = beat * 4
chords = [  # (bass note, chord notes, melody pool) as MIDI numbers
    (36, [60, 64, 67, 71], [76, 79, 83, 72]),   # Cmaj7
    (45, [57, 60, 64, 67], [76, 72, 79, 69]),   # Am7
    (41, [53, 57, 60, 64], [77, 81, 72, 76]),   # Fmaj7
    (43, [55, 59, 62, 64], [74, 79, 71, 76]),   # G6
] * 2
length = bar * len(chords)
buf = np.zeros(int(SR * (length + 3)))
rng = np.random.default_rng(7)
for i, (b, notes, mel) in enumerate(chords):
    t0 = i * bar
    for n in notes: place(buf, pad(hz(n - 12), bar + 0.8, 0.10), t0 - 0.4 if i else t0)
    place(buf, bass(hz(b), bar, 0.5), t0)
    place(buf, bass(hz(b), bar / 2, 0.3), t0 + bar / 2)
    for k in range(8):  # gentle music-box arpeggio on eighth notes
        if rng.random() < 0.8:
            note = notes[k % 4] + (12 if k % 4 == 3 else 0)
            place(buf, bell(hz(note), 1.6, 0.22), t0 + k * beat / 2)
    place(buf, bell(hz(mel[rng.integers(0, 4)]), 2.0, 0.16), t0 + beat * (1 + rng.integers(0, 3)))
buf = reverb(buf, 0.6)
n = int(SR * length)
loop = buf[:n].copy()
loop[: len(buf) - n] += buf[n:]  # fold the reverb tail back onto the start so the loop is seamless
save("music_cozy", norm(loop, 0.55))

# ---- sound effects ----
def sfx(name, notes, gap=0.07, dur=0.9, vol=0.7):
    b = np.zeros(int(SR * (dur + gap * len(notes) + 0.3)))
    for i, m in enumerate(notes): place(b, bell(hz(m), dur, 0.8), i * gap)
    save(name, norm(reverb(b, 0.3), vol))

def tok(freq, dur=0.14):
    t = np.arange(int(SR * dur)) / SR
    return (np.sin(2*np.pi*freq*t) * np.exp(-t*55) + 0.5*np.sin(2*np.pi*freq*2.1*t) * np.exp(-t*85))

# Buy: two soft wooden "tok" sounds (low pitched, not a bell)
b = np.zeros(int(SR * 0.3))
place(b, tok(380), 0.0); place(b, tok(500), 0.07)
save("sfx_buy", norm(b, 0.7))
sfx("sfx_coin", [88], 0.0, 0.5, 0.5)
# UI click: a crisp mechanical tick (short noise burst plus a tiny high sine)
ct = np.arange(int(SR * 0.05)) / SR
noise = np.diff(np.random.default_rng(3).normal(size=len(ct) + 1))
click = noise * np.exp(-ct * 220) + 0.6 * np.sin(2*np.pi*1900*ct) * np.exp(-ct * 260)
save("sfx_click", norm(click, 0.6))
sfx("sfx_evolve", [60, 64, 67, 72, 76, 79, 84], 0.11, 1.4, 0.8)
t = np.arange(int(SR * 0.9)) / SR
whoosh = np.random.default_rng(1).normal(size=len(t)) * np.sin(np.pi * t / 0.9) ** 2 * 0.25
sweep = np.sin(2*np.pi*(300 + 700 * t / 0.9) * t) * np.sin(np.pi * t / 0.9) ** 1.5 * 0.5
save("sfx_boost", norm(whoosh + sweep, 0.6))
d = np.arange(int(SR * 0.25)) / SR
save("sfx_deny", norm(np.sin(2*np.pi*120*d) * np.exp(-d * 18), 0.5))
print("done")
