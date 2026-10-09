"""Nature ambience and train sounds (all synthesized). Needs numpy and ffmpeg.
Run: python3 store_assets/make_sfx.py
  amb_day    a soft daytime breeze, a seamless loop (birds are separate one-shots)
  sfx_bird1..3  single bird songs, played now and then at random during the day
  sfx_install   buying a new device: a wooden clunk, two bolts tightened, a bright chime
  sfx_upgrade   upgrading a device: a bubbly pop with a rising sparkle
  amb_night  crickets, a seamless loop
  amb_storm  wind with gusts, a seamless loop
  sfx_wolf   a far-away wolf howl (played now and then at night)
  sfx_brake  the train slowing down and stopping at a station (hiss + a soft bell)
  sfx_pass   passing a station: a level-crossing bell going by
"""
import os, sys
import numpy as np
sys.path.insert(0, os.path.dirname(__file__))
from make_music import SR, save, tvec, reverb

rng = np.random.default_rng(11)


def lowpass(x, a):
    """One-pole low-pass, a in (0, 1): smaller = darker."""
    from scipy.signal import lfilter
    return lfilter([a], [1, -(1 - a)], x)


def loopify(x, fade_sec=1.0):
    """Crossfade the end into the start so the sound loops without a click."""
    n = int(SR * fade_sec)
    head, tail = x[:n].copy(), x[-n:].copy()
    w = np.linspace(0, 1, n)
    x = x[:-n].copy()
    x[:n] = head * w + tail * (1 - w)
    return x


def chirp(f0, f1, dur, vol):
    t = tvec(dur)
    f = np.linspace(f0, f1, len(t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = np.sin(np.pi * np.clip(t / dur, 0, 1)) ** 2
    return np.sin(ph) * env * vol


def place(buf, sig, start):
    i = int(start * SR)
    n = min(len(sig), len(buf) - i)
    if n > 0:
        buf[i:i + n] += sig[:n]


def birds(seconds=32.0):
    buf = np.zeros(int(SR * seconds))
    # a few different birds, each with its own little song
    songs = [
        lambda: [chirp(3200, 4200, 0.08, 0.5), chirp(4200, 3000, 0.1, 0.45)],
        lambda: [chirp(2500, 2600, 0.12, 0.4)] * 3,
        lambda: [chirp(5200, 3800, 0.06, 0.35), chirp(5000, 3600, 0.06, 0.3), chirp(4800, 3400, 0.06, 0.25)],
        lambda: [chirp(1900, 2400, 0.25, 0.35), chirp(2400, 1800, 0.3, 0.3)],
    ]
    t = 0.3
    while t < seconds - 2:
        song = songs[rng.integers(0, len(songs))]()
        pan_vol = rng.uniform(0.35, 1.0)
        tt = t
        for note in song:
            place(buf, note * pan_vol, tt)
            tt += len(note) / SR + rng.uniform(0.03, 0.09)
        t += rng.uniform(0.8, 2.6)
    breeze = lowpass(rng.standard_normal(len(buf)), 0.01) * 0.6
    buf = reverb(buf, 0.4) + breeze
    return loopify(buf / np.max(np.abs(buf)) * 0.5)


def crickets(seconds=24.0):
    buf = np.zeros(int(SR * seconds))
    for c in range(4):
        f = rng.uniform(4300, 5200)
        rate = rng.uniform(2.2, 3.4)          # chirp groups per second
        vol = rng.uniform(0.15, 0.35)
        t = rng.uniform(0, 0.5)
        while t < seconds:
            for k in range(3):                   # each chirp is three quick pulses
                place(buf, chirp(f, f * 0.98, 0.022, vol), t + k * 0.03)
            t += 1 / rate + rng.uniform(-0.04, 0.04)
    hum = lowpass(rng.standard_normal(len(buf)), 0.004) * 0.4
    buf = reverb(buf, 0.3) + hum
    return loopify(buf / np.max(np.abs(buf)) * 0.45)


def wind(seconds=20.0):
    n = rng.standard_normal(int(SR * seconds))
    base = lowpass(n, 0.02)
    whistle = lowpass(rng.standard_normal(len(n)), 0.15) * 0.25
    t = np.arange(len(n)) / SR
    gust = 0.55 + 0.45 * np.sin(2 * np.pi * t / 6.5) * np.sin(2 * np.pi * t / 2.7 + 1.0)
    buf = (base * 3 + whistle) * gust
    return loopify(buf / np.max(np.abs(buf)) * 0.6, 2.0)


def wolf():
    dur = 3.4
    t = tvec(dur)
    # rising then falling pitch with a little vibrato
    f = 380 + 260 * np.sin(np.pi * np.clip(t / 2.6, 0, 1)) ** 0.7 - 60 * np.clip((t - 2.6) / 0.8, 0, 1)
    f = f * (1 + 0.012 * np.sin(2 * np.pi * 5.5 * t))
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = np.minimum(1, t / 0.35) * np.minimum(1, (dur - t) / 0.9)
    s = (np.sin(ph) + 0.3 * np.sin(2 * ph) + 0.1 * np.sin(3 * ph)) * env
    s = lowpass(s, 0.25)
    s = reverb(s * 0.6, 0.9)
    return s / np.max(np.abs(s)) * 0.5


def bell(f, dur, vol):
    t = tvec(dur)
    return (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 4)) * np.exp(-t * 2.5) * vol


def brake():
    dur = 2.6
    t = tvec(dur)
    hiss = lowpass(rng.standard_normal(len(t)), 0.35) - lowpass(rng.standard_normal(len(t)), 0.02)
    env = np.minimum(1, t / 0.1) * np.exp(-t * 1.3)
    squeal = np.sin(2 * np.pi * (1400 - 300 * t / dur) * t) * np.exp(-t * 2.2) * 0.08
    s = hiss * env * 0.5 + squeal
    out = np.zeros(int(SR * 3.6))
    out[:len(s)] += s
    # a soft "ding-dong" when the train has stopped
    place(out, bell(784, 1.4, 0.35), 1.8)
    place(out, bell(659, 1.6, 0.35), 2.15)
    out = reverb(out, 0.4)
    return out / np.max(np.abs(out)) * 0.55


def passing():
    dur = 2.4
    out = np.zeros(int(SR * dur))
    t = np.arange(len(out)) / SR
    # the crossing bell gets louder and then fades as the train passes, a little lower after it passes (doppler)
    k = 0
    while k * 0.22 < dur - 0.3:
        tt = k * 0.22
        closeness = np.exp(-((tt - 0.9) / 0.6) ** 2)
        f = 1046 if tt < 0.9 else 990
        place(out, bell(f, 0.5, 0.15 + 0.35 * closeness), tt)
        k += 1
    whoosh = lowpass(rng.standard_normal(len(out)), 0.08) * np.exp(-((t - 0.95) / 0.35) ** 2) * 0.8
    out = reverb(out + whoosh, 0.3)
    return out / np.max(np.abs(out)) * 0.5


def breeze(seconds=24.0):
    n = rng.standard_normal(int(SR * seconds))
    base = lowpass(n, 0.012)
    air = lowpass(rng.standard_normal(len(n)), 0.08) * 0.12
    t = np.arange(len(n)) / SR
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * t / 8.0) * np.sin(2 * np.pi * t / 3.0 + 0.7)
    buf = (base * 3 + air) * swell
    return loopify(buf / np.max(np.abs(buf)) * 0.35, 2.0)


def bird_song(kind):
    songs = {
        1: [chirp(3200, 4200, 0.08, 0.5), chirp(4200, 3000, 0.1, 0.45), chirp(3400, 4400, 0.08, 0.45), chirp(4400, 3100, 0.12, 0.4)],
        2: [chirp(2500, 2650, 0.12, 0.4)] * 3 + [chirp(2650, 2000, 0.25, 0.35)],
        3: [chirp(1900, 2400, 0.25, 0.4), chirp(2400, 1800, 0.3, 0.35), chirp(1950, 2450, 0.22, 0.3)],
    }[kind]
    out = np.zeros(int(SR * 2.2))
    tt = 0.05
    for note in songs:
        place(out, note, tt)
        tt += len(note) / SR + 0.06
    out = reverb(out, 0.5)
    return out / np.max(np.abs(out)) * 0.5


def install():
    out = np.zeros(int(SR * 1.3))
    t = tvec(0.25)
    # wooden clunk: a low knock with a short noisy attack
    knock = (np.sin(2 * np.pi * 150 * t) + 0.5 * np.sin(2 * np.pi * 310 * t)) * np.exp(-t * 28)
    knock += lowpass(rng.standard_normal(len(t)), 0.3) * np.exp(-t * 60) * 0.6
    place(out, knock * 0.9, 0.0)
    # two quick ratchet turns (bolts being tightened)
    for k, start in enumerate((0.16, 0.34)):
        for j in range(5):
            tc = tvec(0.018)
            click = lowpass(rng.standard_normal(len(tc)), 0.5) * np.exp(-tc * 300) * (0.5 + 0.1 * j)
            place(out, click, start + j * 0.026)
    # bright "done" chime: two rising notes
    for f, st in ((1047, 0.58), (1568, 0.68)):
        tb = tvec(0.6)
        place(out, (np.sin(2 * np.pi * f * tb) + 0.3 * np.sin(2 * np.pi * f * 3 * tb) * np.exp(-tb * 10)) * np.exp(-tb * 5) * 0.4, st)
    out = reverb(out, 0.3)
    return out / np.max(np.abs(out)) * 0.6


def upgrade():
    out = np.zeros(int(SR * 0.9))
    t = tvec(0.16)
    # bubbly pop: fast upward pitch bend
    f = 300 + 900 * (t / 0.16) ** 0.6
    ph = 2 * np.pi * np.cumsum(f) / SR
    place(out, np.sin(ph) * np.exp(-t * 18) * 0.8, 0.0)
    # rising sparkle arpeggio
    for i, m in enumerate((84, 88, 91, 96)):
        tb = tvec(0.35)
        fr = 440 * 2 ** ((m - 69) / 12)
        place(out, np.sin(2 * np.pi * fr * tb) * np.exp(-tb * 12) * (0.25 + 0.04 * i), 0.07 + i * 0.045)
    out = reverb(out, 0.35)
    return out / np.max(np.abs(out)) * 0.55


def horn_deep():
    """A deep diesel two-tone horn."""
    t = tvec(2.0)
    env = np.minimum(1, t / 0.12) * np.minimum(1, (2.0 - t) / 0.5)
    tone = sum(np.sign(np.sin(2 * np.pi * f * t)) * 0.3 + np.sin(2 * np.pi * f * t) for f in (138.6, 174.6)) / 2
    tone = lowpass(tone, 0.12)
    out = reverb(tone * env, 0.5)
    return out / np.max(np.abs(out)) * 0.6


def horn_chorus():
    """A bright steam whistle chord that slides up a little."""
    t = tvec(2.2)
    env = np.minimum(1, t / 0.2) * np.minimum(1, (2.2 - t) / 0.7)
    bend = 1 + 0.03 * np.minimum(1, t / 0.4)
    out = np.zeros(len(t))
    for f in (523.3, 659.3, 784.0):
        ph = 2 * np.pi * np.cumsum(f * bend) / SR
        out += np.sin(ph) + 0.2 * np.sin(2 * ph)
    breath = lowpass(rng.standard_normal(len(t)), 0.3) * 0.15
    out = reverb((out / 3 + breath) * env, 0.6)
    return out / np.max(np.abs(out)) * 0.55


if __name__ == "__main__" and len(sys.argv) > 1 and sys.argv[1] == "v3":
    save("sfx_horn2", horn_deep(), q=4)
    save("sfx_horn3", horn_chorus(), q=4)
elif __name__ == "__main__" and len(sys.argv) > 1 and sys.argv[1] == "v2":
    save("amb_day", breeze(), q=2)
    for k in (1, 2, 3):
        save(f"sfx_bird{k}", bird_song(k), q=3)
    save("sfx_install", install(), q=4)
    save("sfx_upgrade", upgrade(), q=4)
elif __name__ == "__main__":
    save("amb_day", birds(), q=2)
    save("amb_night", crickets(), q=2)
    save("amb_storm", wind(), q=2)
    save("sfx_wolf", wolf(), q=3)
    save("sfx_brake", brake(), q=3)
    save("sfx_pass", passing(), q=3)
