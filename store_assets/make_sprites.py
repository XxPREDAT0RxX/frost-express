"""Renders every 3D-shaded sprite used by the game (devices, train furniture, car walls, worker).

Run:  python3 store_assets/make_sprites.py            (all)
      python3 store_assets/make_sprites.py samovar    (only names containing "samovar")
Output: app/src/main/res/drawable-nodpi/*.webp
Device sprites live on a 64 x 64 unit canvas (the object is centred at 32, 32).
Rotors (parts that spin in the game) are separate sprites on the same canvas, spun around their pivot in code.
"""
import math, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from render3d import Sprite, c

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
os.makedirs(OUT, exist_ok=True)
JOBS = []


def job(name):
    def deco(f):
        JOBS.append((name, f))
        return f
    return deco


def dev(seed=1):
    return Sprite(64, 64, ppu=8, seed=seed)


def save(s, name, **kw):
    s.render(os.path.join(OUT, name + '.webp'), out_scale=0.5, **kw)


# palette
STEEL, LSTEEL, DSTEEL, IRON = c('8a949e'), c('c8ced4'), c('5e6873'), c('3a3f47')
BRASS, COPPER, GOLD = c('d9a441'), c('c1703a'), c('e8b23a')
WOOD, LWOOD, DWOOD = c('8b5e3c'), c('b98b5e'), c('5e3b22')
RED, BLUE, GREEN, CREAM = c('d64545'), c('3b7fc4'), c('3f9e5a'), c('f2ece0')
COAL = c('17191c')


def coal_heap(s, x0, y0, x1, y1, z, n=26, seed=3):
    import numpy as np
    r = np.random.default_rng(seed)
    for _ in range(n):
        x = r.uniform(x0, x1); y = r.uniform(y0, y1); rr = r.uniform(2.2, 4.2)
        s.add(s.dome(x, y, rr, z, rr * r.uniform(0.7, 1.1)), COAL, 'rough', 0.6)


def crate_at(s, x, y, w, z0=0, tone=LWOOD):
    h = z0 + w * 0.55
    s.add(s.box(x, y, x + w, y + w, z0, h, r=0.6, bevel=0.8), tone, 'wood', 0.1)
    b = w * 0.14
    for k in (0, 1):
        s.add(s.box(x + (w - b) * k, y, x + (w - b) * k + b, y + w, h, h + 0.8, r=0.3, bevel=0.4), DWOOD, 'wood', 0.1)
        s.add(s.box(x, y + (w - b) * k, x + w, y + (w - b) * k + b, h, h + 0.8, r=0.3, bevel=0.4), DWOOD, 'wood', 0.1)
    s.add(s.tube(x + b, y + b, x + w - b, y + w - b, b * 0.8, h, h + 0.7), DWOOD, 'wood', 0.1)


# =============================== devices ===============================

@job('dev_snow_chains')
def _():
    s = dev(2)
    s.add(s.ring(32, 32, 20, 16, 0, 10), c('26292e'), 'rubber', 0.1)
    s.add(s.cyl(32, 32, 12.5, 0, 7), STEEL, 'metal', 0.8)
    for i in range(16):
        a = i * 2 * math.pi / 16
        s.add(s.ring(32 + math.cos(a) * 24.5, 32 + math.sin(a) * 24.5, 3.0, 1.5, 8, 11.5), LSTEEL, 'metal', 1.0)
    for i in range(6):
        a = i * math.pi / 3
        s.add(s.dome(32 + math.cos(a) * 8, 32 + math.sin(a) * 8, 1.4, 7, 1.2), DSTEEL, 'metal', 0.9)
    s.add(s.dome(32, 32, 4.5, 7, 3), DSTEEL, 'metal', 0.9)
    save(s, 'dev_snow_chains')


@job('dev_water_tank')
def _():
    s = dev(3)
    s.add(s.cyl(30, 32, 25, 0, 18, bevel=3), c('6b8fb3'), 'metalv', 0.6)
    for r in (25, 17):
        s.add(s.ring(30, 32, r, 1.6, 17, 19), DSTEEL, 'metal', 0.7)
    s.add(s.cyl(30, 32, 7, 18, 21), STEEL, 'metal', 0.8)
    s.add(s.cyl(30, 32, 2, 21, 23), IRON, 'metal', 0.8)
    s.add(s.tube(52, 32, 62, 32, 5, 4, 9), DSTEEL, 'metal', 0.7)
    s.add(s.box(52, 12, 57, 26, 0, 14, r=2), c('9fd8f0'), 'glass', 1.0, emit=0.25)
    save(s, 'dev_water_tank')


@job('dev_coal_tender')
def _():
    s = dev(4)
    s.add(s.box(6, 10, 58, 54, 0, 12, r=3, bevel=1.5), c('5b4636'), 'rough', 0.2)
    s.carve(s.box(10, 14, 54, 50, 2, 2, r=2, bevel=0.5))
    coal_heap(s, 13, 17, 51, 47, 2, n=46)
    s.add(s.tube(20, 4, 46, 44, 2.2, 8, 9), LWOOD, 'woodv', 0.2)
    s.add(s.box(42, 40, 52, 50, 9, 10.5, r=1.5), DSTEEL, 'metal', 0.7)
    save(s, 'dev_coal_tender')


@job('dev_pressure_gauge')
def _():
    s = dev(5)
    s.add(s.tube(4, 46, 60, 46, 8, 0, 8), BRASS, 'metal', 0.9)
    s.add(s.cyl(32, 46, 6, 0, 10), COPPER, 'metal', 0.8)
    s.add(s.tube(32, 40, 32, 30, 4, 6, 9), BRASS, 'metal', 0.9)
    s.add(s.cyl(32, 26, 15, 6, 12, bevel=2), DSTEEL, 'metal', 0.9)
    s.add(s.cyl(32, 26, 12, 10, 12.5, bevel=0.5), CREAM, 'paint', 0.3)
    for x in (12, 52):
        s.add(s.cyl(x, 46, 5, 0, 11), RED, 'paint', 0.5)
        s.add(s.cyl(x, 46, 2, 11, 12.5), LSTEEL, 'metal', 0.8)
    save(s, 'dev_pressure_gauge')


@job('dev_brass_pistons')
def _():
    s = dev(6)
    s.add(s.box(4, 50, 60, 58, 0, 5, r=2), DSTEEL, 'metal', 0.6)
    for x in (10, 36):
        s.add(s.box(x, 6, x + 18, 34, 0, 14, r=4, bevel=4), BRASS, 'metalv', 1.0)
        s.add(s.box(x + 2, 4, x + 16, 8, 0, 15, r=2), c('a8771f'), 'metal', 0.8)
        s.add(s.tube(x + 9, 34, x + 9, 48, 3.5, 4, 7), LSTEEL, 'metal', 1.0)
        s.add(s.cyl(x + 9, 50, 5.5, 3, 9), IRON, 'metal', 0.8)
    save(s, 'dev_brass_pistons')


@job('dev_ticket_office')
def _():
    s = dev(7)
    s.add(s.box(8, 16, 56, 58, 0, 16, r=2, bevel=1), WOOD, 'woodv', 0.2)
    for i in range(6):
        col = RED if i % 2 == 0 else c('f4f4f4')
        s.add(s.box(6 + i * 8.67, 6, 6 + (i + 1) * 8.67, 22, 18 - 2, 18 + 1, r=0.3, bevel=2), col, 'fabric', 0.2)
    s.add(s.box(14, 34, 50, 44, 13, 17, r=1), c('9fc4e0'), 'glass', 1.0)
    s.add(s.box(22, 48, 42, 56, 16, 18, r=1), BRASS, 'metal', 0.9)
    s.add(s.dome(46, 52, 3, 17, 3), GOLD, 'metal', 1.0)
    save(s, 'dev_ticket_office')


@job('dev_samovar')
def _():
    s = dev(8)
    s.add(s.cyl(30, 32, 22, 0, 4), c('5a3a22'), 'metal', 0.3)
    s.add(s.dome(30, 32, 19, 4, 16), COPPER, 'metal', 0.9)
    s.add(s.ring(30, 32, 19, 2, 8, 10), c('8a4f2a'), 'metal', 0.8)
    s.add(s.cyl(30, 32, 7, 18, 21), c('8a4f2a'), 'metal', 0.6)
    s.add(s.dome(30, 32, 4, 21, 3), IRON, 'metal', 0.5)
    s.add(s.tube(46, 32, 56, 32, 4, 10, 14), c('8a4f2a'), 'metal', 0.7)
    s.add(s.cyl(57, 32, 3, 12, 16), RED, 'paint', 0.4)
    save(s, 'dev_samovar')


@job('dev_frost_shield')
def _():
    s = dev(9)
    pts = [(32 + math.cos(i * math.pi / 3) * 27, 32 + math.sin(i * math.pi / 3) * 27) for i in range(6)]
    s.add(s.poly(pts, 0, 5, bevel=2), DSTEEL, 'metal', 0.7)
    for i in range(3):
        a = i * 2 * math.pi / 3 - math.pi / 2
        s.add(s.cyl(32 + math.cos(a) * 20, 32 + math.sin(a) * 20, 4, 4, 12), LSTEEL, 'metal', 0.9)
        s.add(s.dome(32 + math.cos(a) * 20, 32 + math.sin(a) * 20, 2.5, 12, 2.5), c('8fe3ff'), 'glass', 1.0, emit=0.7)
    s.add(s.dome(32, 32, 12, 4, 13), c('9fdcff'), 'glass', 1.0, emit=0.35)
    save(s, 'dev_frost_shield')


@job('dev_spark_coil')
def _():
    s = dev(10)
    s.add(s.cyl(32, 32, 24, 0, 6, bevel=2), IRON, 'metal', 0.6)
    for k, r in enumerate((19, 16, 13, 10)):
        s.add(s.ring(32, 32, r, 2.2, 6 + k * 3, 9 + k * 3), COPPER, 'metal', 1.0)
    s.add(s.ring(32, 32, 9, 5, 18, 24), LSTEEL, 'metal', 1.0)
    s.add(s.dome(32, 32, 5, 20, 7), LSTEEL, 'metal', 1.0)
    save(s, 'dev_spark_coil')


@job('dev_flywheel')
def _():
    s = dev(11)
    s.add(s.box(8, 26, 56, 38, 0, 6, r=2), DSTEEL, 'metal', 0.6)
    s.add(s.cyl(32, 32, 6, 0, 8), IRON, 'metal', 0.7)
    save(s, 'dev_flywheel')
    r = dev(12)
    r.add(r.ring(32, 32, 24, 7, 5, 13), IRON, 'rough', 0.6)
    for i in range(5):
        a = i * 2 * math.pi / 5
        r.add(r.tube(32, 32, 32 + math.cos(a) * 22, 32 + math.sin(a) * 22, 4, 7, 10), STEEL, 'metal', 0.9)
    r.add(r.cyl(32, 32, 6, 8, 13), LSTEEL, 'metal', 1.0)
    r.add(r.dome(32, 9, 2, 12, 2), RED, 'paint', 0.6)
    save(r, 'rot_flywheel', shadow=False, ground_shadow=0)


@job('dev_steam_dome')
def _():
    s = dev(13)
    s.add(s.cyl(30, 34, 24, 0, 6, bevel=2), c('2f4a3c'), 'metal', 0.5)
    s.add(s.dome(30, 34, 21, 6, 14), c('3f6b52'), 'metal', 0.8)
    s.add(s.ring(30, 34, 21, 2, 8, 10), BRASS, 'metal', 1.0)
    s.add(s.cyl(30, 34, 6, 18, 22), BRASS, 'metal', 1.0)
    s.add(s.tube(36, 30, 52, 14, 3, 14, 16), BRASS, 'metal', 0.9)
    s.add(s.cyl(53, 13, 4, 12, 18), RED, 'paint', 0.5)
    save(s, 'dev_steam_dome')


@job('dev_spare_crates')
def _():
    s = dev(14)
    crate_at(s, 6, 6, 24, 0, c('b98b5e'))
    crate_at(s, 34, 8, 23, 0, c('d0a675'))
    crate_at(s, 8, 34, 23, 0, c('a9774a'))
    crate_at(s, 34, 34, 24, 0, c('c49a6c'))
    crate_at(s, 22, 20, 20, 14, c('d9b27c'))
    save(s, 'dev_spare_crates')


@job('dev_wind_deflector')
def _():
    s = dev(15)
    s.add(s.box(4, 6, 10, 58, 0, 5, r=2), DSTEEL, 'metal', 0.6)
    for i in range(4):
        y = 6 + i * 13
        s.add(s.poly([(8, y + 12), (50, y), (56, y + 3), (14, y + 16)], 2, 9, bevel=1.5), c('a9d6ec'), 'metalv', 1.0)
    save(s, 'dev_wind_deflector')


@job('dev_insulation')
def _():
    s = dev(16)
    cols = [c('f2a6b8'), c('f5d060'), c('f5d060'), c('f2a6b8')]
    for i, (x, y) in enumerate([(18, 18), (46, 18), (18, 46), (46, 46)]):
        s.add(s.cyl(x, y, 13, 0, 12, bevel=4), cols[i], 'fabric', 0.1)
        for r in (9, 5):
            s.add(s.ring(x, y, r, 1.0, 11, 11.6), tuple(v * 0.75 for v in cols[i]), 'fabric', 0.1)
        s.add(s.cyl(x, y, 2.5, 0, 9), c('8a6a4a'), 'paint', 0.1)
    save(s, 'dev_insulation')


@job('dev_stove_pipe')
def _():
    s = dev(17)
    s.add(s.cyl(26, 34, 21, 0, 4), IRON, 'rough', 0.3)
    s.add(s.dome(26, 34, 18, 4, 14), c('2b2e33'), 'rough', 0.7)
    s.add(s.box(14, 44, 38, 54, 6, 12, r=2), c('ff8a2b'), 'glass', 0.2, emit=0.9)
    s.add(s.tube(32, 28, 60, 28, 8, 14, 22), IRON, 'metal', 0.8)
    s.add(s.ring(46, 28, 4.5, 2, 16, 23), DSTEEL, 'metal', 0.8)
    save(s, 'dev_stove_pipe')


@job('dev_cargo_net')
def _():
    s = dev(18)
    crate_at(s, 8, 10, 22, 0); crate_at(s, 33, 12, 22, 0); crate_at(s, 18, 33, 24, 0)
    for i in range(7):
        t = 6 + i * 8.5
        s.add(s.tube(t, 4, t, 60, 1.6, 15, 16), c('e8ddb5'), 'fabric', 0.1)
        s.add(s.tube(4, t, 60, t, 1.6, 15, 16), c('e8ddb5'), 'fabric', 0.1)
    save(s, 'dev_cargo_net')


@job('dev_cooling_fan')
def _():
    s = dev(19)
    s.add(s.box(4, 4, 60, 60, 0, 10, r=5, bevel=2), DSTEEL, 'metal', 0.7)
    s.carve(s.cyl(32, 32, 24, 3, 3))
    for (x, y) in [(9, 9), (55, 9), (9, 55), (55, 55)]:
        s.add(s.dome(x, y, 2, 10, 1.5), LSTEEL, 'metal', 1.0)
    save(s, 'dev_cooling_fan')
    r = dev(20)
    for i in range(3):
        a = i * 2 * math.pi / 3
        r.add(r.dome(32 + math.cos(a) * 12, 32 + math.sin(a) * 12, 10, 4, 3, ry=6), c('6fd3e8'), 'metal', 0.9)
    r.add(r.cyl(32, 32, 5, 4, 9), LSTEEL, 'metal', 1.0)
    for k in range(4):
        r.add(r.ring(32, 32, 8 + k * 5, 1, 10, 11), IRON, 'metal', 0.6)
    save(r, 'rot_cooling_fan', shadow=False, ground_shadow=0)


@job('dev_boiler_rivets')
def _():
    s = dev(21)
    s.add(s.box(4, 8, 60, 56, 0, 6, r=3, bevel=2), DSTEEL, 'metal', 0.8)
    for i in range(5):
        for j in range(4):
            s.add(s.dome(10 + i * 11, 14 + j * 12, 2.3, 6, 2), LSTEEL, 'metal', 1.0)
    s.add(s.tube(44, 2, 56, 22, 3, 8, 10), WOOD, 'wood', 0.2)
    s.add(s.box(50, 18, 62, 28, 7, 12, r=1.5), IRON, 'metal', 0.9)
    save(s, 'dev_boiler_rivets')


@job('dev_wool_blankets')
def _():
    s = dev(22)
    cols = [c('d1685d'), c('4a6fa5'), c('6fae5a'), c('e0a21b')]
    for i in range(4):
        z = i * 4
        s.add(s.box(6 + i * 1.5, 10 + i * 2, 58 - i * 1.5, 54 - i * 2, z, z + 4, r=3, bevel=2), cols[i], 'fabric', 0.1)
    for j in range(5):
        s.add(s.box(14 + j * 8, 18, 15.5 + j * 8, 46, 15.5, 16.3, r=0.4), (0.95, 0.85, 0.5), 'fabric', 0.1)
    save(s, 'dev_wool_blankets')


@job('dev_tea_brewer')
def _():
    s = dev(23)
    s.add(s.dome(26, 34, 17, 0, 15), c('3d6fb0'), 'paint', 0.9)
    s.add(s.ring(26, 34, 10, 2, 13, 15), c('f2f2f2'), 'paint', 0.8)
    s.add(s.dome(26, 34, 5, 14, 3), c('f2f2f2'), 'paint', 0.8)
    s.add(s.tube(40, 30, 54, 22, 4, 6, 10), c('3d6fb0'), 'paint', 0.9)
    s.add(s.ring(8, 34, 6, 3, 6, 11), c('3d6fb0'), 'paint', 0.9)
    for (x, y) in [(50, 50), (52, 40)]:
        s.add(s.cyl(x, y, 6, 0, 7), c('f2f2f2'), 'paint', 0.8)
        s.carve(s.cyl(x, y, 4.5, 5, 5))
    save(s, 'dev_tea_brewer')


@job('dev_gold_coupler')
def _():
    s = dev(24)
    s.add(s.box(2, 26, 14, 38, 0, 8, r=2), DSTEEL, 'metal', 0.7)
    s.add(s.box(50, 26, 62, 38, 0, 8, r=2), DSTEEL, 'metal', 0.7)
    s.add(s.ring(23, 32, 12, 5, 3, 10), GOLD, 'metal', 1.0)
    s.add(s.ring(41, 32, 12, 5, 5, 12), c('f1cf7a'), 'metal', 1.0)
    save(s, 'dev_gold_coupler')


@job('dev_turbo_valve')
def _():
    s = dev(25)
    s.add(s.tube(2, 32, 62, 32, 10, 0, 10), DSTEEL, 'metal', 0.8)
    s.add(s.tube(32, 2, 32, 62, 10, 0, 10), DSTEEL, 'metal', 0.8)
    s.add(s.cyl(32, 32, 12, 0, 14), IRON, 'metal', 0.8)
    for (x, y) in [(6, 32), (58, 32), (32, 6), (32, 58)]:
        s.add(s.cyl(x, y, 6.5, 0, 11.5), STEEL, 'metal', 0.9)
    save(s, 'dev_turbo_valve')
    r = dev(26)
    r.add(r.ring(32, 32, 14, 3.5, 15, 19), RED, 'paint', 0.8)
    for i in range(3):
        a = i * 2 * math.pi / 3
        r.add(r.tube(32, 32, 32 + math.cos(a) * 14, 32 + math.sin(a) * 14, 2.5, 15, 18), RED, 'paint', 0.8)
    r.add(r.cyl(32, 32, 3.5, 15, 20), LSTEEL, 'metal', 1.0)
    save(r, 'rot_turbo_valve', shadow=False, ground_shadow=0)


@job('dev_ice_cutter')
def _():
    s = dev(27)
    s.add(s.box(6, 6, 58, 58, 0, 5, r=4), c('4a5560'), 'metal', 0.6)
    s.add(s.box(2, 48, 20, 62, 0, 8, r=2), c('cfefff'), 'glass', 1.0)
    save(s, 'dev_ice_cutter')
    r = dev(28)
    r.add(r.cyl(32, 32, 21, 5, 8), LSTEEL, 'metal', 1.0)
    for i in range(18):
        a = i * 2 * math.pi / 18
        r.add(r.poly([(32 + math.cos(a) * 20, 32 + math.sin(a) * 20), (32 + math.cos(a + 0.2) * 26, 32 + math.sin(a + 0.2) * 26),
                      (32 + math.cos(a + 0.3) * 20, 32 + math.sin(a + 0.3) * 20)], 5, 7, bevel=0.3), LSTEEL, 'metal', 1.0)
    r.add(r.cyl(32, 32, 7, 6, 10), DSTEEL, 'metal', 0.9)
    r.add(r.dome(32, 32, 3, 10, 2), IRON, 'metal', 0.9)
    save(r, 'rot_ice_cutter', shadow=False, ground_shadow=0)


@job('dev_grease_pump')
def _():
    s = dev(29)
    s.add(s.cyl(26, 38, 20, 0, 16, bevel=3), c('a8691e'), 'metalv', 0.7)
    for r in (20, 13):
        s.add(s.ring(26, 38, r, 2, 15, 17), c('6a4412'), 'metal', 0.7)
    s.add(s.cyl(26, 38, 5, 15, 19), DSTEEL, 'metal', 0.9)
    s.add(s.tube(26, 38, 54, 10, 3.5, 19, 22), LSTEEL, 'metal', 1.0)
    s.add(s.dome(55, 9, 4.5, 20, 4), RED, 'paint', 0.8)
    s.add(s.dome(54, 52, 4, 0, 1.5), c('e0a21b'), 'glass', 1.0)
    save(s, 'dev_grease_pump')


@job('dev_fuel_injector')
def _():
    s = dev(30)
    s.add(s.cyl(24, 34, 19, 0, 18, bevel=3), c('2f7a52'), 'metalv', 0.8)
    for r in (19, 12):
        s.add(s.ring(24, 34, r, 2, 17, 19), c('1f5a3a'), 'metal', 0.8)
    s.add(s.cyl(24, 34, 4, 18, 21), LSTEEL, 'metal', 1.0)
    for i in range(12):
        a = 0.3 + i * 0.4
        s.add(s.tube(46 + math.cos(a) * 8, 46 + math.sin(a) * 8, 46 + math.cos(a + 0.4) * 8, 46 + math.sin(a + 0.4) * 8, 3, 2, 5), IRON, 'rubber', 0.3)
    s.add(s.box(48, 8, 62, 20, 0, 10, r=2), DSTEEL, 'metal', 0.8)
    s.add(s.tube(40, 30, 52, 18, 3, 6, 9), IRON, 'rubber', 0.3)
    save(s, 'dev_fuel_injector')


@job('dev_cabin_heater')
def _():
    s = dev(31)
    s.add(s.box(6, 12, 58, 52, 0, 6, r=3), c('6e1f1f'), 'metal', 0.5)
    for i in range(8):
        s.add(s.box(9 + i * 6.2, 14, 13 + i * 6.2, 50, 4, 14, r=1.6, bevel=1.8), c('b83a3a'), 'paint', 0.8)
    s.add(s.cyl(54, 54, 3, 0, 9), CREAM, 'paint', 0.6)
    save(s, 'dev_cabin_heater')


@job('dev_cargo_crane')
def _():
    s = dev(32)
    s.add(s.cyl(20, 40, 15, 0, 6, bevel=2), DSTEEL, 'metal', 0.7)
    s.add(s.cyl(20, 40, 9, 6, 10), IRON, 'metal', 0.8)
    crate_at(s, 42, 44, 14, 0)
    save(s, 'dev_cargo_crane')
    r = dev(33)
    r.add(r.box(16, 36, 58, 44, 12, 18, r=2, bevel=1.5), c('e0a21b'), 'paint', 0.8)
    for k in range(5):
        r.add(r.tube(20 + k * 8, 37, 24 + k * 8, 43, 1.2, 18, 18.6), c('a87a10'), 'paint', 0.4)
    r.add(r.cyl(20, 40, 6, 12, 20), c('c48a10'), 'paint', 0.8)
    r.add(r.box(51, 35, 59, 45, 10, 16, r=1.5), IRON, 'metal', 0.9)
    save(r, 'rot_cargo_crane', shadow=False, ground_shadow=0)


@job('dev_lounge')
def _():
    s = dev(34)
    s.add(s.box(2, 4, 62, 60, 0, 1.2, r=3, bevel=1), c('a8443b'), 'fabric', 0.05)
    s.add(s.box(6, 8, 36, 40, 1, 10, r=5, bevel=3), c('7a4b3a'), 'fabric', 0.2)
    s.add(s.box(10, 14, 32, 38, 8, 13, r=4, bevel=3), c('d1856b'), 'fabric', 0.15)
    s.add(s.box(6, 8, 36, 14, 8, 16, r=3, bevel=2), c('6e4234'), 'fabric', 0.2)
    s.add(s.cyl(46, 46, 11, 1, 10, bevel=1), WOOD, 'wood', 0.5)
    s.add(s.cyl(43, 44, 3.5, 10, 11), c('f2f2f2'), 'paint', 0.8)
    s.add(s.cyl(43, 44, 2.2, 10.5, 11.2), c('6b3a12'), 'paint', 0.8)
    save(s, 'dev_lounge')


@job('dev_bank_safe')
def _():
    s = dev(35)
    s.add(s.box(6, 6, 58, 58, 0, 20, r=4, bevel=3), c('5e6873'), 'metal', 0.8)
    s.add(s.box(10, 10, 54, 54, 19, 21, r=3, bevel=1), STEEL, 'metal', 0.9)
    s.add(s.cyl(30, 32, 9, 20, 24), IRON, 'metal', 0.9)
    s.add(s.ring(30, 32, 7, 1.4, 24, 25), LSTEEL, 'metal', 1.0)
    s.add(s.box(46, 24, 50, 40, 20, 24, r=1.5), BRASS, 'metal', 1.0)
    for i in range(4):
        s.add(s.cyl(14, 50, 4, 21 + i * 1.3, 22 + i * 1.3), GOLD, 'metal', 1.0)
    save(s, 'dev_bank_safe')


@job('dev_compass')
def _():
    s = dev(36)
    s.add(s.cyl(32, 32, 27, 0, 7, bevel=2), c('6a4412'), 'wood', 0.6)
    s.add(s.ring(32, 32, 24, 3, 6, 9), BRASS, 'metal', 1.0)
    s.add(s.cyl(32, 32, 22, 4, 6), CREAM, 'paint', 0.3)
    for i in range(4):
        a = i * math.pi / 2
        s.add(s.tube(32 + math.cos(a) * 14, 32 + math.sin(a) * 14, 32 + math.cos(a) * 20, 32 + math.sin(a) * 20, 2, 6, 6.5), IRON, 'paint', 0.3)
    save(s, 'dev_compass')
    r = dev(37)
    r.add(r.poly([(32, 12), (36, 32), (28, 32)], 7, 8.5, bevel=0.6), RED, 'paint', 0.8)
    r.add(r.poly([(32, 52), (36, 32), (28, 32)], 7, 8.5, bevel=0.6), LSTEEL, 'metal', 1.0)
    r.add(r.dome(32, 32, 2.5, 8, 2), BRASS, 'metal', 1.0)
    save(r, 'rot_compass', shadow=False, ground_shadow=0)


@job('dev_governor')
def _():
    s = dev(38)
    s.add(s.cyl(32, 32, 14, 0, 6, bevel=2), DSTEEL, 'metal', 0.8)
    s.add(s.cyl(32, 32, 7, 6, 18), IRON, 'metal', 0.9)
    save(s, 'dev_governor')
    r = dev(39)
    for sgn in (-1, 1):
        r.add(r.tube(32, 32, 32 + sgn * 20, 32, 2.6, 18, 20), STEEL, 'metal', 1.0)
        r.add(r.dome(32 + sgn * 22, 32, 6.5, 14, 9), RED, 'paint', 1.0)
    r.add(r.dome(32, 32, 4, 18, 4), BRASS, 'metal', 1.0)
    save(r, 'rot_governor', shadow=False, ground_shadow=0)


@job('dev_magnet_coupler')
def _():
    s = dev(40)
    pts = []
    for i in range(25):
        a = math.pi * i / 24
        pts.append((32 + math.cos(a) * 18, 34 + math.sin(a) * 18))
    for i in range(len(pts) - 1):
        s.add(s.tube(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], 11, 0, 11), c('b8343a'), 'paint', 0.9)
    for x in (14, 50):
        s.add(s.tube(x, 34, x, 14, 11, 0, 11), c('b8343a'), 'paint', 0.9)
        s.add(s.box(x - 5.5, 4, x + 5.5, 14, 0, 11, r=1.2), LSTEEL, 'metal', 1.0)
    save(s, 'dev_magnet_coupler')


@job('dev_aurora_prism')
def _():
    s = dev(41)
    s.add(s.cyl(30, 38, 22, 0, 3), DSTEEL, 'metal', 0.7)
    s.add(s.poly([(30, 10), (52, 50), (8, 50)], 3, 16, bevel=5), c('cfefff'), 'glass', 1.0, emit=0.35)
    save(s, 'dev_aurora_prism')


@job('dev_traction_sand')
def _():
    s = dev(42)
    s.add(s.box(4, 8, 60, 56, 0, 12, r=3, bevel=1.5), c('7a5a3c'), 'wood', 0.2)
    s.carve(s.box(9, 13, 55, 51, 4, 4, r=2))
    s.add(s.dome(32, 32, 20, 4, 9, ry=16), c('e6d09a'), 'rough', 0.1)
    s.add(s.tube(44, 12, 58, 48, 2.5, 13, 14), WOOD, 'woodv', 0.3)
    s.add(s.box(50, 44, 62, 56, 10, 14, r=2), DSTEEL, 'metal', 0.9)
    save(s, 'dev_traction_sand')


@job('dev_chefs_oven')
def _():
    s = dev(43)
    s.add(s.box(4, 4, 60, 60, 0, 14, r=3, bevel=2), c('25282d'), 'metal', 0.8)
    for i in range(4):
        x = 19 + (i % 2) * 26; y = 19 + (i // 2) * 24
        s.add(s.ring(x, y, 8, 2.2, 14, 15.5), DSTEEL, 'metal', 0.9)
        s.add(s.cyl(x, y, 3, 14, 15), IRON, 'metal', 0.6)
    for k in range(4):
        s.add(s.cyl(12 + k * 13, 58, 2.3, 10, 14), LSTEEL, 'metal', 1.0)
    save(s, 'dev_chefs_oven')


@job('dev_bogie_springs')
def _():
    s = dev(44)
    s.add(s.box(4, 50, 60, 60, 0, 5, r=2), DSTEEL, 'metal', 0.6)
    for x in (18, 46):
        s.add(s.cyl(x, 28, 14, 0, 4), IRON, 'metal', 0.6)
        for k in range(5):
            s.add(s.ring(x, 28, 11 - k * 0.6, 3, 3 + k * 2.4, 5.5 + k * 2.4), LSTEEL if k % 2 else STEEL, 'metal', 1.0)
        s.add(s.cyl(x, 28, 4, 4, 15), IRON, 'metal', 0.9)
    save(s, 'dev_bogie_springs')


# ---------------- overhead devices ----------------

@job('dev_steam_whistle')
def _():
    s = dev(45)
    s.add(s.tube(4, 32, 40, 32, 7, 0, 7), BRASS, 'metal', 1.0)
    s.add(s.cyl(44, 32, 13, 0, 12, bevel=3), c('f1cf7a'), 'metal', 1.0)
    s.add(s.ring(44, 32, 13, 2.5, 10, 13), COPPER, 'metal', 1.0)
    s.add(s.cyl(44, 32, 4, 12, 15), COPPER, 'metal', 1.0)
    s.add(s.tube(30, 32, 30, 14, 2.4, 7, 9), DSTEEL, 'metal', 0.9)
    s.add(s.dome(30, 13, 2.5, 8, 2), RED, 'paint', 0.8)
    save(s, 'dev_steam_whistle')


@job('dev_headlamp')
def _():
    s = dev(46)
    s.add(s.tube(32, 32, 32, 60, 5, 0, 5), DSTEEL, 'metal', 0.7)
    s.add(s.cyl(32, 32, 20, 2, 12, bevel=3), DSTEEL, 'metal', 0.9)
    s.add(s.ring(32, 32, 17, 3, 11, 14), LSTEEL, 'metal', 1.0)
    s.add(s.dome(32, 32, 15, 11, 4), c('fff1a8'), 'glass', 1.0, emit=0.85)
    save(s, 'dev_headlamp')


@job('dev_roof_fan')
def _():
    s = dev(47)
    s.add(s.cyl(32, 32, 9, 0, 6), BRASS, 'metal', 1.0)
    save(s, 'dev_roof_fan')
    r = dev(48)
    for i in range(4):
        a = i * math.pi / 2 + 0.3
        r.add(r.dome(32 + math.cos(a) * 15, 32 + math.sin(a) * 15, 11, 7, 2, ry=5.5), LWOOD, 'woodv', 0.4)
    r.add(r.cyl(32, 32, 6, 7, 11), BRASS, 'metal', 1.0)
    r.add(r.dome(32, 32, 3.5, 11, 3), c('fff1a8'), 'glass', 1.0, emit=0.7)
    save(r, 'rot_roof_fan', shadow=False, ground_shadow=0)


@job('dev_radio_mast')
def _():
    s = dev(49)
    s.add(s.box(18, 18, 46, 46, 0, 5, r=2), DSTEEL, 'metal', 0.7)
    for (a, b) in [((18, 18), (46, 46)), ((46, 18), (18, 46)), ((32, 4), (32, 60)), ((4, 32), (60, 32))]:
        s.add(s.tube(a[0], a[1], b[0], b[1], 2.4, 5, 8), LSTEEL, 'metal', 1.0)
    s.add(s.cyl(32, 32, 6, 5, 16), STEEL, 'metal', 1.0)
    s.add(s.dome(32, 32, 4.5, 16, 4), RED, 'glass', 1.0, emit=0.6)
    save(s, 'dev_radio_mast')


@job('dev_pocket_watch')
def _():
    s = dev(50)
    for i in range(8):
        s.add(s.ring(32 + i * 3.4, 6 - i * 0.2, 1.8, 1.0, 0, 2), GOLD, 'metal', 1.0)
    s.add(s.cyl(32, 9, 3, 2, 6), GOLD, 'metal', 1.0)
    s.add(s.cyl(32, 35, 24, 0, 8, bevel=3), GOLD, 'metal', 1.0)
    s.add(s.cyl(32, 35, 20, 6, 8.5, bevel=0.5), CREAM, 'paint', 0.4)
    for i in range(12):
        a = i * math.pi / 6
        s.add(s.dome(32 + math.cos(a) * 17, 35 + math.sin(a) * 17, 1.0, 8.5, 0.6), IRON, 'paint', 0.3)
    save(s, 'dev_pocket_watch')


@job('dev_lantern_rack')
def _():
    s = dev(51)
    s.add(s.box(2, 28, 62, 36, 0, 5, r=2), WOOD, 'wood', 0.3)
    for x in (13, 32, 51):
        s.add(s.cyl(x, 32, 9, 4, 12, bevel=2), IRON, 'metal', 0.8)
        s.add(s.cyl(x, 32, 6, 11, 13), c('ffc857'), 'glass', 1.0, emit=0.9)
        s.add(s.dome(x, 32, 2.5, 13, 2.5), IRON, 'metal', 0.8)
    save(s, 'dev_lantern_rack')


@job('dev_signal_bell')
def _():
    s = dev(52)
    s.add(s.box(4, 29, 60, 35, 0, 4, r=2), DSTEEL, 'metal', 0.7)
    s.add(s.dome(32, 32, 22, 2, 16), c('b0782a'), 'metal', 1.0)
    s.add(s.ring(32, 32, 21, 3, 3, 7), c('8a5a1a'), 'metal', 1.0)
    s.add(s.cyl(32, 32, 5, 16, 20), c('7a4f14'), 'metal', 1.0)
    save(s, 'dev_signal_bell')


@job('dev_frost_radar')
def _():
    s = dev(53)
    s.add(s.cyl(32, 32, 26, 0, 4, bevel=2), c('14323a'), 'metal', 0.5)
    s.add(s.ring(32, 32, 25, 2, 3, 5), c('3fae9a'), 'metal', 0.6)
    s.add(s.cyl(32, 32, 5, 4, 10), DSTEEL, 'metal', 0.9)
    save(s, 'dev_frost_radar')
    r = dev(54)
    r.add(r.tube(32, 32, 32, 14, 3, 10, 12), LSTEEL, 'metal', 1.0)
    r.add(r.dome(32, 12, 13, 10, 4, ry=6), c('dfe7ee'), 'metal', 1.0)
    r.add(r.dome(32, 12, 2, 13, 2), RED, 'paint', 0.8)
    save(r, 'rot_frost_radar', shadow=False, ground_shadow=0)


@job('dev_night_watch')
def _():
    s = dev(55)
    s.add(s.cyl(32, 32, 26, 0, 7, bevel=2), c('1c2541'), 'paint', 0.8)
    s.add(s.ring(32, 32, 25, 2.5, 6, 9), c('5bc0be'), 'metal', 1.0)
    moon = s.cyl(30, 30, 11, 7, 9)
    s.add(moon, c('ffe680'), 'glass', 0.6, emit=0.8)
    s.add(s.cyl(36, 26, 9, 7, 7.8), c('1c2541'), 'paint', 0.8)
    for (x, y) in [(14, 20), (46, 40), (20, 46), (48, 18), (40, 50)]:
        s.add(s.dome(x, y, 1.3, 7, 0.8), c('ffffff'), 'glass', 1.0, emit=1.0)
    save(s, 'dev_night_watch')


@job('dev_timetable')
def _():
    s = dev(56)
    s.add(s.box(2, 10, 62, 54, 0, 6, r=3, bevel=1.5), c('23262b'), 'paint', 0.6)
    s.add(s.box(2, 8, 62, 13, 5, 8, r=1), BRASS, 'metal', 1.0)
    for i in range(4):
        y = 19 + i * 9
        col = c('ffe680') if i % 2 == 0 else c('8fd0ff')
        s.add(s.box(8, y, 44 - (i % 3) * 6, y + 4, 6, 6.6, r=0.6), col, 'glass', 0.5, emit=0.9)
        s.add(s.dome(54, y + 2, 2, 6, 1), c('3fae5a') if i % 2 == 0 else RED, 'glass', 1.0, emit=0.9)
    save(s, 'dev_timetable')


@job('dev_polar_beacon')
def _():
    s = dev(57)
    s.add(s.cyl(32, 32, 20, 0, 6, bevel=2), DSTEEL, 'metal', 0.8)
    s.add(s.cyl(32, 32, 14, 5, 9), c('1b6a86'), 'glass', 1.0, emit=0.4)
    save(s, 'dev_polar_beacon')
    r = dev(58)
    r.add(r.cyl(32, 32, 7, 9, 15), LSTEEL, 'metal', 1.0)
    for sgn in (-1, 1):
        r.add(r.box(32 + sgn * 6 - 3, 28, 32 + sgn * 6 + 3, 36, 10, 15, r=1), c('9ff0ff'), 'glass', 1.0, emit=0.9)
    save(r, 'rot_polar_beacon', shadow=False, ground_shadow=0)


# ---------------- the core eight, for the upgrade pop-up ----------------

@job('dev_engine')
def _():
    s = dev(60)
    s.add(s.box(36, 8, 58, 56, 0, 20, r=3, bevel=2), c('7a2a1f'), 'paint', 0.6)
    s.add(s.box(39, 11, 55, 53, 19, 21, r=2), c('3a1410'), 'paint', 0.4)
    s.add(s.tube(8, 32, 38, 32, 30, 0, 22), c('2d3a33'), 'metalv', 0.9)
    for x in (14, 24, 34):
        s.add(s.box(x, 17, x + 2.5, 47, 0, 23, r=1), BRASS, 'metal', 1.0)
    s.add(s.cyl(12, 32, 6, 20, 30), IRON, 'metal', 0.8)
    s.carve(s.cyl(12, 32, 3.8, 26, 26))
    s.add(s.dome(26, 32, 5, 20, 6), BRASS, 'metal', 1.0)
    save(s, 'dev_engine')


@job('dev_wagon')
def _():
    s = dev(61)
    crate_at(s, 6, 8, 25, 0); crate_at(s, 34, 10, 24, 0); crate_at(s, 18, 34, 26, 0); crate_at(s, 22, 18, 20, 14)
    save(s, 'dev_wagon')


@job('dev_heat')
def _():
    s = dev(62)
    s.add(s.box(6, 6, 58, 58, 0, 18, r=6, bevel=3), c('4a6fa5'), 'metal', 0.8)
    s.add(s.cyl(30, 32, 16, 17, 19), c('3a5782'), 'metal', 0.8)
    for r in (13, 9, 5):
        s.add(s.ring(30, 32, r, 1.6, 18, 20), c('ffb347'), 'glass', 0.5, emit=0.6)
    s.add(s.ring(52, 32, 8, 2.6, 18, 23), LSTEEL, 'metal', 1.0)
    for i in range(4):
        a = i * math.pi / 2
        s.add(s.tube(52, 32, 52 + math.cos(a) * 8, 32 + math.sin(a) * 8, 2, 19, 21), LSTEEL, 'metal', 1.0)
    save(s, 'dev_heat')


@job('dev_food')
def _():
    s = dev(63)
    s.add(s.box(4, 6, 60, 58, 0, 16, r=3, bevel=1.5), c('d9d2c3'), 'paint', 0.5)
    for (x, y) in [(18, 20), (18, 44)]:
        s.add(s.ring(x, y, 8, 2.5, 16, 17.5), IRON, 'metal', 0.8)
    s.add(s.cyl(18, 20, 9, 16, 26, bevel=1.5), LSTEEL, 'metal', 1.0)
    s.carve(s.cyl(18, 20, 7.5, 23, 23))
    s.add(s.cyl(18, 20, 7.3, 22, 23), c('e0a21b'), 'paint', 0.6)
    s.add(s.box(36, 12, 54, 34, 16, 18, r=2), c('c49a6c'), 'wood', 0.3)
    s.add(s.dome(45, 22, 4, 18, 3), RED, 'paint', 0.8)
    s.add(s.cyl(46, 46, 7, 16, 18), c('f2f2f2'), 'paint', 0.9)
    save(s, 'dev_food')


@job('dev_plow')
def _():
    s = dev(64)
    s.add(s.poly([(6, 54), (58, 54), (32, 8)], 0, 14, bevel=6), c('8fc9e6'), 'metalv', 1.0)
    s.add(s.tube(32, 18, 32, 50, 3, 12, 15), LSTEEL, 'metal', 1.0)
    s.add(s.box(10, 54, 54, 60, 0, 6, r=2), DSTEEL, 'metal', 0.7)
    save(s, 'dev_plow')


@job('dev_coal')
def _():
    s = dev(65)
    s.add(s.box(6, 10, 58, 54, 0, 14, r=4, bevel=2), c('5b4636'), 'rough', 0.3)
    s.carve(s.box(11, 15, 53, 49, 3, 3, r=2))
    coal_heap(s, 14, 18, 50, 46, 3, n=50, seed=9)
    save(s, 'dev_coal')


@job('dev_booster')
def _():
    s = dev(66)
    s.add(s.cyl(32, 32, 24, 0, 8, bevel=2), DSTEEL, 'metal', 0.9)
    s.add(s.ring(32, 32, 19, 4, 7, 12), BRASS, 'metal', 1.0)
    s.add(s.dome(32, 32, 14, 7, 12), c('ffb347'), 'glass', 1.0, emit=0.75)
    s.add(s.dome(28, 28, 4, 17, 2), c('fff4d6'), 'glass', 1.0, emit=1.0)
    save(s, 'dev_booster')


@job('dev_conductor')
def _():
    s = dev(67)
    s.add(s.box(4, 12, 60, 52, 0, 8, r=3), c('8b5e3c'), 'wood', 0.4)
    s.add(s.box(7, 15, 57, 49, 7, 12, r=4, bevel=3), c('e9e4d8'), 'fabric', 0.1)
    s.add(s.box(22, 15, 57, 49, 10, 14, r=4, bevel=3), c('4a6fa5'), 'fabric', 0.1)
    s.add(s.dome(14, 32, 6, 11, 4, ry=10), c('fafafa'), 'fabric', 0.1)
    save(s, 'dev_conductor')


# =============================== train furniture (1 unit = 1 dp) ===============================

def furn(w, h, seed):
    return Sprite(w + 8, h + 8, ppu=8, seed=seed)  # 4 dp margin for shadows


@job('core_boiler')
def _():
    s = furn(60, 54, 70); o = 4
    s.add(s.box(o, o, o + 60, o + 54, 0, 8, r=8, bevel=2), IRON, 'rough', 0.4)
    s.add(s.box(o + 4, o + 4, o + 46, o + 50, 6, 20, r=10, bevel=8), c('4b525c'), 'metalv', 0.9)
    for y in (14, 30, 46):
        s.add(s.box(o + 4, o + y - 1.2, o + 46, o + y + 1.2, 6, 21, r=1, bevel=6), BRASS, 'metal', 1.0)
    s.add(s.box(o + 48, o + 10, o + 60, o + 38, 6, 12, r=3), DSTEEL, 'metal', 0.8)
    s.add(s.cyl(o + 18, o + 28, 8.5, 18, 23), DSTEEL, 'metal', 1.0)
    s.add(s.cyl(o + 18, o + 28, 6.8, 21, 23.5, bevel=0.4), CREAM, 'paint', 0.4)
    save(s, 'core_boiler')


@job('core_coalbin')
def _():
    s = furn(54, 40, 71); o = 4
    s.add(s.box(o, o, o + 54, o + 40, 0, 12, r=6, bevel=1.5), c('5b4636'), 'rough', 0.3)
    s.carve(s.box(o + 4, o + 4, o + 50, o + 36, 3, 3, r=4))
    coal_heap(s, o + 7, o + 7, o + 47, o + 33, 3, n=40, seed=11)
    save(s, 'core_coalbin')


@job('core_console')
def _():
    s = furn(62, 26, 72); o = 4
    s.add(s.box(o, o, o + 62, o + 26, 0, 10, r=5, bevel=2), c('50606e'), 'metal', 0.8)
    s.carve(s.box(o + 4, o + 3, o + 28, o + 12, 8, 8, r=2))
    s.add(s.box(o + 4, o + 3, o + 28, o + 12, 7, 8, r=2), c('22303b'), 'glass', 1.0, emit=0.2)
    s.add(s.box(o + 6, o + 15, o + 16, o + 23, 9, 10.5, r=1), IRON, 'metal', 0.9)
    s.add(s.ring(o + 48, o + 13, 7.5, 2.5, 9, 12), BRASS, 'metal', 1.0)
    for k in range(3):
        s.add(s.dome(o + 34 + k * 5, o + 6, 1.6, 10, 1.2), [RED, c('3fae5a'), c('ffd43b')][k], 'glass', 1.0, emit=0.8)
    save(s, 'core_console')


@job('core_bunk')
def _():
    s = furn(50, 28, 73); o = 4
    s.add(s.box(o, o, o + 50, o + 28, 0, 7, r=4), c('8b5e3c'), 'wood', 0.4)
    s.add(s.box(o + 2, o + 2, o + 48, o + 26, 6, 10, r=4, bevel=2), c('e9e4d8'), 'fabric', 0.1)
    s.add(s.box(o + 18, o + 2, o + 48, o + 26, 9, 12.5, r=4, bevel=3), c('4a6fa5'), 'fabric', 0.1)
    s.add(s.dome(o + 9, o + 14, 6, 9, 4, ry=9), c('fafafa'), 'fabric', 0.1)
    save(s, 'core_bunk')


@job('core_locker')
def _():
    s = furn(50, 28, 74); o = 4
    s.add(s.box(o, o, o + 50, o + 28, 0, 22, r=3, bevel=1.5), c('7b8794'), 'metalv', 0.8)
    for k in (1, 2):
        s.add(s.box(o + k * 16.6 - 0.6, o + 1, o + k * 16.6 + 0.6, o + 27, 20, 22.5, r=0.3), c('5e6873'), 'metal', 0.5)
    for k in range(3):
        s.add(s.box(o + 6 + k * 16.6, o + 12, o + 9 + k * 16.6, o + 16, 22, 23, r=0.6), LSTEEL, 'metal', 1.0)
    save(s, 'core_locker')


@job('core_heater')
def _():
    s = furn(54, 54, 75); o = 4
    s.add(s.box(o, o, o + 54, o + 54, 0, 18, r=8, bevel=3), c('4a6fa5'), 'metal', 0.8)
    s.add(s.box(o + 4, o + 4, o + 50, o + 50, 17, 19, r=6), c('3a5782'), 'metal', 0.8)
    s.add(s.cyl(o + 25, o + 27, 14, 18, 20), c('2b4b7a'), 'metal', 0.9)
    for r in (11, 7, 3.5):
        s.add(s.ring(o + 25, o + 27, r, 1.4, 19, 21), c('ffb347'), 'glass', 0.4, emit=0.55)
    s.add(s.tube(o + 46, o + 10, o + 54, o + 10, 4, 10, 14), LSTEEL, 'metal', 1.0)
    save(s, 'core_heater')


@job('core_kitchen')
def _():
    s = furn(54, 66, 76); o = 4
    s.add(s.box(o, o, o + 54, o + 66, 0, 14, r=4, bevel=1.5), c('bdb6a6'), 'paint', 0.5)
    s.add(s.box(o + 3, o + 3, o + 51, o + 63, 13, 15, r=3), c('e2dccd'), 'paint', 0.6)
    for y in (22, 48):
        s.add(s.cyl(o + 10, o + y, 9.5, 14, 16), c('2b2f36'), 'metal', 0.8)
        s.add(s.ring(o + 10, o + y, 6, 1.6, 15.5, 17), c('50606e'), 'metal', 1.0)
    s.add(s.box(o + 30, o + 8, o + 48, o + 34, 15, 17, r=2), c('c49a6c'), 'wood', 0.3)
    s.add(s.dome(o + 40, o + 18, 3, 17, 2.5), RED, 'paint', 0.9)
    s.add(s.cyl(o + 40, o + 50, 7, 15, 17), c('f2f2f2'), 'paint', 0.9)
    save(s, 'core_kitchen')


@job('core_table')
def _():
    s = furn(62, 32, 77); o = 4
    for x in (o + 2, o + 52):
        s.add(s.box(x, o + 2, x + 8, o + 30, 0, 9, r=3, bevel=2), c('6e4a2e'), 'wood', 0.3)
    s.add(s.box(o + 10, o, o + 50, o + 32, 0, 13, r=5, bevel=1.5), c('8b5e3c'), 'wood', 0.6)
    s.add(s.cyl(o + 44, o + 16, 6.5, 13, 14.5), c('f2f2f2'), 'paint', 0.9)
    s.add(s.dome(o + 44, o + 16, 3.4, 14, 1.8), c('e0a21b'), 'paint', 0.8)
    s.add(s.cyl(o + 18, o + 12, 3.2, 13, 18), c('cfe9ff'), 'glass', 1.0)
    save(s, 'core_table')


@job('core_crate')
def _():
    s = furn(20, 20, 78)
    crate_at(s, 4, 4, 20, 0)
    save(s, 'core_crate')


# ---------------- car walls and the locomotive nose (tinted with the train colour in game) ----------------

@job('car_walls')
def _():
    W, H, m = 190, 116, 8
    s = Sprite(W + 2 * m, H + 2 * m, ppu=8, seed=80)
    grey = (0.86, 0.86, 0.86)
    s.add(s.box(m, m, m + W, m + H, 0, 16, r=12, bevel=4), grey, 'paint', 0.6)
    s.carve(s.box(m + 7, m + 7, m + W - 7, m + H - 7, -50, -50, r=4))
    for k in range(9):
        x = m + 18 + k * 20
        for y in (m + 2.8, m + H - 2.8):
            s.add(s.dome(x, y, 1.0, 15, 0.9), (0.95, 0.95, 0.95), 'metal', 1.0)
    # remove the carved floor so the textured floor shows through
    s.Z[s.Z < -40] = -1e9
    save(s, 'car_walls', ground_shadow=0.55)


@job('car_glass')
def _():
    W, H, m = 190, 116, 8
    s = Sprite(W + 2 * m, H + 2 * m, ppu=8, seed=81)
    wy = 14
    while wy < H - 20:
        for wx in (1.5, W - 5.5):
            s.add(s.box(m + wx, m + wy, m + wx + 4, m + wy + 14, 15, 17, r=1.5, bevel=1), c('9cc3e0'), 'glass', 1.0, emit=0.15)
        wy += 24
    save(s, 'car_glass', shadow=False, ground_shadow=0)


@job('loco_nose')
def _():
    m = 6
    s = Sprite(190 + 2 * m, 30 + 2 * m, ppu=8, seed=82)
    pts = [(m + 16, m + 28), (m + 46, m + 4), (m + 144, m + 4), (m + 174, m + 28)]
    s.add(s.poly(pts, 0, 14, bevel=6), (0.86, 0.86, 0.86), 'paint', 0.8)
    for x in (56, 134):
        s.add(s.cyl(m + x, m + 15, 7, 12, 16), (0.35, 0.35, 0.35), 'metal', 1.0)
        s.add(s.dome(m + x, m + 15, 5, 15, 2.5), c('fff1a8'), 'glass', 1.0, emit=0.6)
    save(s, 'loco_nose', ground_shadow=0.5)


# ---------------- the worker (tinted jacket and hat) ----------------

@job('npc_body')
def _():
    s = Sprite(28, 20, ppu=12, seed=90)
    grey = (0.9, 0.9, 0.9)
    s.add(s.dome(14, 10, 11, 0, 7, ry=6.5), grey, 'fabric', 0.3)
    s.add(s.box(3, 9, 25, 11.5, 5, 7.2, r=1.2, bevel=1), (0.7, 0.7, 0.7), 'fabric', 0.2)
    s.add(s.dome(14, 14.5, 6, 3, 4, ry=3.5), (0.75, 0.75, 0.75), 'fabric', 0.3)
    save(s, 'npc_body', ground_shadow=0.45)


@job('npc_hat')
def _():
    s = Sprite(18, 18, ppu=12, seed=91)
    s.add(s.ring(9, 9.5, 6.3, 2.2, 0, 2.5), (0.75, 0.75, 0.75), 'fabric', 0.2)
    s.add(s.dome(9, 9.5, 6.3, 0, 6.5), (0.92, 0.92, 0.92), 'fabric', 0.3)
    s.add(s.dome(9, 10, 2.2, 6, 2.4), (1, 1, 1), 'fabric', 0.2)
    save(s, 'npc_hat', ground_shadow=0.35)


@job('station_roof')
def _():
    s = Sprite(26, 88, ppu=6, seed=95)
    s.add(s.box(4, 4, 22, 84, 0, 18, r=3, bevel=2), c('8a5f3a'), 'woodv', 0.4)
    s.add(s.box(5, 5, 21, 40, 18, 21, r=3, bevel=2.5), c('f4f9fc'), 'rough', 0.2)
    s.add(s.dome(13, 52, 6, 18, 2.5, ry=10), c('f4f9fc'), 'rough', 0.2)
    save(s, 'station_roof', ground_shadow=0.5)


if __name__ == '__main__':
    only = sys.argv[1] if len(sys.argv) > 1 else ''
    for name, f in JOBS:
        if only in name:
            f()
            print('rendered', name)
