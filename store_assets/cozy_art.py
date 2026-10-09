"""Cozy pastel art for Frost Express: soft colours, thick warm-brown outlines, rounded shapes.

Everything is drawn as SVG here and rasterised with headless Chromium (Playwright):
    python3 store_assets/cozy_art.py            # everything
    python3 store_assets/cozy_art.py dev_samovar  # only names containing this text
Output: app/src/main/res/drawable-nodpi/*.webp and app/src/main/java/.../DeviceArt.kt
(live effects such as steam, glow and needles, and the spinning parts, are placed from the anchors below).

Device sprites: 64 x 64 units, the object stands on the ground line at y = 57.
Furniture is drawn in 3/4 view: a lighter top face and a darker front face.
"""
import asyncio, io, math, os, sys
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), '..')
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'res', 'drawable-nodpi')
KT = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'chetna', 'frostexpress', 'DeviceArt.kt')

OL = '#6B4A3A'      # outline
SW = 1.6            # outline width (device units)
CREAM, WHITE = '#FFF6E9', '#FFFFFF'
WOOD_T, WOOD_F, WOOD_D = '#E9BC8C', '#D9A576', '#B9876A'
STEEL_T, STEEL_F, STEEL_D = '#B9C8D8', '#9DB0C6', '#7C8FA8'
COP_T, COP_F = '#F7B98A', '#D98457'
RED_T, RED_F = '#F7A3A3', '#E46A6A'
BLUE_T, BLUE_F = '#B8D8F2', '#8FBCE6'
MINT_T, MINT_F = '#BFE0D2', '#8FC2AE'
GREEN_T, GREEN_F = '#A8DCB9', '#7FC29A'
YEL_T, YEL_F = '#FFE49A', '#FFC85C'
PINK_T, PINK_F = '#FFC9D6', '#F49AB2'
LAV_T, LAV_F = '#DCCBFA', '#B9A2EE'
NAVY = '#4C6A86'
COAL = '#4A4550'
GLOW = 'url(#glow)'

DEFS = '''<defs>
<linearGradient id="glow" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFF0B8"/><stop offset="1" stop-color="#FFB86B"/></linearGradient>
<linearGradient id="ice" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F2FBFF"/><stop offset="1" stop-color="#9FDCF5"/></linearGradient>
<linearGradient id="gold" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFE7A0"/><stop offset="1" stop-color="#F2B84B"/></linearGradient>
<linearGradient id="copper" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FAC8A0"/><stop offset="1" stop-color="#D98457"/></linearGradient>
</defs>'''


# ---------------- shape helpers (all return SVG strings) ----------------
def o(sw=None):
    return f'stroke="{OL}" stroke-width="{sw or SW}" stroke-linejoin="round" stroke-linecap="round"'


def shadow(rx=22, cx=32, cy=57, ry=3.6):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#000" opacity=".10"/>'


def rect(x, y, w, h, fill, r=3, sw=None, extra=''):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" {o(sw)} {extra}/>'


def rect_n(x, y, w, h, fill, r=2, op=1):  # no outline
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}" opacity="{op}"/>'


def circ(cx, cy, r, fill, sw=None):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" {o(sw)}/>'


def circ_n(cx, cy, r, fill, op=1):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}" opacity="{op}"/>'


def ell(cx, cy, rx, ry, fill, sw=None):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{fill}" {o(sw)}/>'


def ell_n(cx, cy, rx, ry, fill, op=1):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{fill}" opacity="{op}"/>'


def line(x1, y1, x2, y2, col=OL, w=SW):
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{col}" stroke-width="{w}" stroke-linecap="round"/>'


def path(d, fill, sw=None):
    return f'<path d="{d}" fill="{fill}" {o(sw)}/>'


def path_n(d, fill, op=1):
    return f'<path d="{d}" fill="{fill}" opacity="{op}"/>'


def stroke_path(d, col, w):
    return f'<path d="{d}" fill="none" stroke="{col}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/>'


def box3(x, y, w, h, d, top, front, r=4):
    """A box in 3/4 view: front face, then the lighter top face over it."""
    return rect(x, y + d * 0.55, w, h - d * 0.55, front, r) + rect(x, y, w, d, top, r)


def cyl3(cx, y, rx, h, top, side, ry=None):
    """An upright cylinder in 3/4 view: body, then its top ellipse."""
    ry = ry or rx * 0.32
    body = f'M{cx - rx},{y} L{cx - rx},{y + h} A{rx},{ry} 0 0 0 {cx + rx},{y + h} L{cx + rx},{y} Z'
    return path(body, side) + ell(cx, y, rx, ry, top)


def shine(x, y, w=3, h=1.4):
    return ell_n(x, y, w, h, WHITE, .65)


def crate(x, y, s, tone_t=WOOD_T, tone_f=WOOD_F):
    d = s * 0.42
    out = box3(x, y, s, s, d, tone_t, tone_f, r=2)
    fy = y + d * 0.55
    out += line(x + 2, fy + 3, x + s - 2, y + s - 2, WOOD_D, 1.4) + line(x + s - 2, fy + 3, x + 2, y + s - 2, WOOD_D, 1.4)
    out += line(x + 2, y + d / 2, x + s - 2, y + d / 2, WOOD_D, 1.0)
    return out


def coal_pile(x, y, w, n=7):
    out = ''
    for i in range(n):
        cx = x + w * (0.12 + 0.76 * ((i * 0.61) % 1))
        cy = y + (i % 3) * 1.8 - (1 if i % 2 else 0)
        out += circ(cx, cy, 3.4, COAL, 1.0)
    return out + circ_n(x + w * 0.4, y - 1.5, 0.9, WHITE, .6)


# ---------------- devices ----------------
DEV = {}      # name -> svg body
FX = {}       # UpgradeType name -> list of (kind, x, y, a, colour)
ROT = {}      # UpgradeType name -> (svg body, px, py, speed, swing)

STEAM, GLOWFX, RIPPLE, SPARKS, RAINBOW, NEEDLE, HANDS, BLINK, SPARKLE, ZZZ, BUBBLES, WIND, DRIP = range(13)


def device(t, body, fx=(), rotor=None):
    DEV[t] = body
    if fx:
        FX[t] = list(fx)
    if rotor:
        ROT[t] = rotor


# --- the core eight ---
device('ENGINE', shadow(26) + box3(8, 18, 48, 38, 18, STEEL_T, STEEL_F, 7) + rect(24, 37, 16, 14, GLOW, 4)
       + path('M28,48 C29,44 33,45 32,41 C35,44 36,47 34,49 Z', '#FF8F5A', 0.8) + circ(17, 25, 5, CREAM, 1.3)
       + line(17, 25, 20, 22, RED_F, 1.3) + rect(42, 7, 9, 15, STEEL_F, 3) + shine(26, 22, 6, 1.6),
       fx=[(STEAM, 46.5, 5, 0, 0), (GLOWFX, 32, 44, 14, 0xFFFF8A2B)])
device('WAGON', shadow(26) + crate(8, 30, 22) + crate(32, 32, 21, '#F2CB98', '#E0AE74') + crate(20, 13, 20, '#EED0A6', '#D9A576'))
device('HEAT', shadow(24) + box3(10, 12, 44, 44, 14, BLUE_T, BLUE_F, 8) + rect(19, 30, 26, 20, GLOW, 6)
       + stroke_path('M23,40 C26,35 29,45 32,40 C35,35 38,45 41,40', '#FF8F5A', 1.8) + circ(48, 19, 4.5, CREAM, 1.3) + shine(20, 15, 6, 1.6),
       fx=[(GLOWFX, 32, 40, 18, 0xFFFFB347)])
device('FOOD', shadow(25) + box3(8, 22, 48, 34, 16, '#F5B094', '#E98E6E', 7) + rect(20, 40, 24, 11, GLOW, 4)
       + path('M17,15 L17,25 C17,31 47,31 47,25 L47,15 Z', 'url(#copper)') + ell(32, 15, 15, 4.5, '#FFCF7A')
       + ell_n(32, 15, 11, 2.6, '#FFB04D') + circ_n(28, 14.5, 1.5, '#F28C5A') + circ_n(36, 15.5, 1.3, '#8FCB7A')
       + line(38, 14, 46, 4, WOOD_D, 2.2), fx=[(STEAM, 30, 9, 0, 0)])
device('PLOW', shadow(27) + path('M6,52 L32,14 L58,52 Z', '#A9DDF2') + stroke_path('M14,47 L32,22 L50,47', '#DDF3FC', 2.4)
       + rect(10, 50, 44, 6, STEEL_F, 3) + line(32, 22, 32, 50, OL, 1.2))
device('COAL', shadow(24) + box3(9, 20, 46, 36, 15, WOOD_T, WOOD_F, 6) + coal_pile(12, 22, 40, 9))
device('BOOSTER', shadow(18) + cyl3(32, 42, 14, 12, STEEL_T, STEEL_F) + circ(32, 28, 12, GLOW)
       + circ_n(28, 24, 3.4, WHITE, .8) + path('M30,22 L35,28 L31,28 L34,34 L28,27 L32,27 Z', '#FFF6E9', 0.7),
       fx=[(GLOWFX, 32, 28, 22, 0xFFFFB347)])
device('CONDUCTOR', shadow(26) + box3(6, 26, 52, 30, 14, WOOD_T, WOOD_F, 7) + rect(9, 22, 46, 18, CREAM, 6)
       + rect(24, 23, 31, 17, BLUE_F, 6) + line(34, 26, 34, 37, '#7FA9D6', 1.4) + line(44, 26, 44, 37, '#7FA9D6', 1.4)
       + rect(11, 25, 11, 12, WHITE, 4))

# --- extra devices (car order) ---
device('STEAM_WHISTLE', shadow(14) + rect(28, 32, 8, 24, STEEL_F, 3) + cyl3(32, 14, 9, 18, YEL_T, '#F2B84B')
       + rect(26, 30, 12, 4, '#F2B84B', 2) + line(38, 22, 48, 16, OL, 1.8) + circ(49, 15, 2.6, RED_F, 1.2) + shine(29, 18, 3, 1.2),
       fx=[(STEAM, 32, 10, 0, 0), (RIPPLE, 32, 22, 16, 0xFFFFFFFF)])
device('HEADLAMP', shadow(14) + rect(29, 40, 6, 16, STEEL_D, 2.5) + rect(22, 50, 20, 6, STEEL_F, 3)
       + circ(32, 28, 15, STEEL_F) + circ(32, 28, 11, '#FFF3B0') + circ_n(28, 24, 3.5, WHITE, .9),
       fx=[(GLOWFX, 32, 28, 30, 0xFFFFF3B0)])
device('SNOW_CHAINS', shadow(24),
       rotor=(circ(32, 32, 22, '#5C5763') + circ(32, 32, 13, STEEL_T) + circ(32, 32, 4.5, STEEL_D)
              + ''.join(circ(32 + 22 * math.cos(i * math.pi / 6), 32 + 22 * math.sin(i * math.pi / 6), 2.6, '#DCE3EA', 1.1) for i in range(12))
              + ''.join(line(32 + 6 * math.cos(i * 1.047), 32 + 6 * math.sin(i * 1.047), 32 + 12 * math.cos(i * 1.047), 32 + 12 * math.sin(i * 1.047), STEEL_D, 1.4) for i in range(6)),
              32, 32, 90, 0))
device('WATER_TANK', shadow(22) + cyl3(30, 14, 18, 40, BLUE_T, BLUE_F, 6) + rect(44, 22, 6, 18, '#DDF3FC', 3)
       + rect_n(45.5, 30, 3, 9, '#7FC8EE') + rect(10, 42, 6, 5, STEEL_F, 2) + line(30, 8, 30, 3, OL, 2) + shine(20, 22, 2.4, 6))
device('COAL_TENDER', shadow(26) + box3(8, 22, 48, 26, 12, '#C7A07A', '#A87E58', 5) + coal_pile(12, 23, 40, 9)
       + circ(17, 50, 5, STEEL_D) + circ(47, 50, 5, STEEL_D) + circ_n(17, 50, 1.6, CREAM) + circ_n(47, 50, 1.6, CREAM))
device('PRESSURE_GAUGE', shadow(22) + rect(6, 44, 52, 9, '#F2B84B', 4.5) + rect(28, 34, 8, 12, '#F2B84B', 2)
       + circ(32, 24, 14, STEEL_F) + circ(32, 24, 10.5, CREAM)
       + ''.join(line(32 + 8.5 * math.cos(a), 24 + 8.5 * math.sin(a), 32 + 10 * math.cos(a), 24 + 10 * math.sin(a), OL, 1) for a in [math.pi * (0.75 + 1.5 * i / 6) for i in range(7)])
       + circ(12, 48, 4, RED_F, 1.2) + circ(52, 48, 4, RED_F, 1.2),
       fx=[(NEEDLE, 32, 24, 8, 0xFFE46A6A)])
device('ROOF_FAN', circ(32, 32, 6, '#F2B84B'),
       rotor=(''.join(ell(32 + 15 * math.cos(a), 32 + 15 * math.sin(a), 11, 5.5, WOOD_T, 1.4).replace('<ellipse', f'<ellipse transform="rotate({math.degrees(a)} {32 + 15 * math.cos(a)} {32 + 15 * math.sin(a)})"')
                      for a in [i * math.pi / 2 + 0.4 for i in range(4)]) + circ(32, 32, 6.5, '#F2B84B') + circ(32, 32, 3, '#FFF3B0', 1.1),
              32, 32, 280, 0))
device('BRASS_PISTONS', shadow(24) + rect(8, 48, 48, 8, STEEL_F, 3)
       + cyl3(20, 14, 8, 26, YEL_T, '#F2B84B') + cyl3(44, 18, 8, 22, YEL_T, '#F2B84B')
       + rect(18.5, 40, 3, 9, STEEL_T, 1.5) + rect(42.5, 40, 3, 9, STEEL_T, 1.5) + shine(16, 22, 1.6, 4) + shine(40, 26, 1.6, 4))
device('TICKET_OFFICE', shadow(25) + box3(10, 24, 44, 32, 10, WOOD_T, WOOD_F, 4) + rect(16, 34, 32, 10, '#DDF3FC', 3)
       + ''.join(path(f'M{8 + i * 8},14 L{16 + i * 8},14 L{16 + i * 8},22 C{14 + i * 8},25 {10 + i * 8},25 {8 + i * 8},22 Z', RED_F if i % 2 == 0 else WHITE, 1.2) for i in range(6))
       + rect(8, 11, 48, 4, WOOD_D, 2) + rect(24, 47, 16, 4, '#F2B84B', 1.5) + circ(45, 46, 2.2, '#F2B84B', 1))
device('SAMOVAR', shadow(18) + rect(24, 50, 16, 6, '#B9764A', 2.5) + path('M16,30 C16,18 48,18 48,30 L46,48 C46,53 18,53 18,48 Z', 'url(#copper)')
       + ell(32, 20, 9, 3, COP_T) + cyl3(32, 9, 6, 9, '#9FC5EA', '#7FB8E6') + rect(46, 36, 9, 3.6, '#B9764A', 1.8)
       + circ(56, 37.8, 2.2, RED_F, 1) + shine(22, 32, 2, 5),
       fx=[(STEAM, 32, 6, 0, 0)])
device('FROST_SHIELD', shadow(20) + box3(14, 44, 36, 12, 6, STEEL_T, STEEL_F, 3)
       + path('M32,6 L44,22 L32,46 L20,22 Z', 'url(#ice)') + line(32, 6, 32, 46, '#FFFFFF', 1.2) + line(20, 22, 44, 22, '#DDF3FC', 1.2),
       fx=[(GLOWFX, 32, 26, 26, 0xFF9FDCFF), (RIPPLE, 32, 26, 26, 0xFFDFF3FF)])
device('RADIO_MAST', shadow(18) + path('M24,56 L32,10 L40,56 Z', 'none') + line(24, 56, 32, 12, STEEL_D, 2.2) + line(40, 56, 32, 12, STEEL_D, 2.2)
       + line(26, 46, 38, 46, STEEL_D, 1.6) + line(28, 34, 36, 34, STEEL_D, 1.6) + line(26, 46, 36, 34, STEEL_D, 1.2) + line(38, 46, 28, 34, STEEL_D, 1.2)
       + circ(32, 10, 4, RED_F, 1.3) + rect(20, 52, 24, 5, STEEL_F, 2),
       fx=[(RIPPLE, 32, 10, 20, 0xFF8FDBA8), (BLINK, 32, 10, 8, 0xFFFF6B6B)])
device('SPARK_COIL', shadow(18) + rect(18, 48, 28, 8, STEEL_F, 3)
       + ''.join(rect(22, 22 + i * 6, 20, 5, '#E9A06A' if i % 2 else '#D98457', 2.5) for i in range(5))
       + circ(32, 15, 9, STEEL_T) + shine(29, 12, 2.4, 1.4),
       fx=[(SPARKS, 32, 15, 22, 0xFFFFF0A0), (GLOWFX, 32, 15, 20, 0xFFD9B4F5)])
device('FLYWHEEL', shadow(22) + path('M22,56 L28,34 L36,34 L42,56 Z', STEEL_F),
       rotor=(circ(32, 30, 20, '#5C6B7D') + circ(32, 30, 15, STEEL_T)
              + ''.join(line(32, 30, 32 + 15 * math.cos(i * 1.2566), 30 + 15 * math.sin(i * 1.2566), STEEL_D, 3) for i in range(5))
              + circ(32, 30, 4.5, '#F2B84B') + circ(32, 12, 1.8, RED_F, 1), 32, 30, 110, 0))
device('STEAM_DOME', shadow(22) + path('M12,52 C12,24 52,24 52,52 Z', GREEN_F) + path('M16,48 C17,30 47,30 48,48 Z', GREEN_T, 0)
       + rect(10, 50, 44, 6, '#5E9A78', 3) + rect(28, 16, 8, 10, '#F2B84B', 2) + ell(32, 16, 6, 2, YEL_T)
       + line(40, 30, 50, 22, '#F2B84B', 2.6) + circ(51, 21, 2.6, RED_F, 1.1) + shine(22, 34, 3.5, 1.6),
       fx=[(STEAM, 32, 12, 0, 0)])
device('SPARE_CRATES', shadow(27) + crate(6, 32, 20) + crate(30, 33, 19, '#F2CB98', '#E0AE74') + crate(18, 14, 19, '#EBC594', '#D49E68')
       + crate(40, 16, 15, '#F5D4A8', '#E2B27C'))
device('POCKET_WATCH', shadow(18) + rect(26, 48, 12, 8, WOOD_F, 3) + circ(32, 8, 3, '#F2B84B', 1.2)
       + circ(32, 30, 18, 'url(#gold)') + circ(32, 30, 14, CREAM)
       + ''.join(circ_n(32 + 11.5 * math.cos(i * math.pi / 6), 30 + 11.5 * math.sin(i * math.pi / 6), 0.9, OL) for i in range(12)),
       fx=[(HANDS, 32, 30, 11, 0xFF6B4A3A)])
device('WIND_DEFLECTOR', shadow(24) + rect(8, 50, 48, 6, STEEL_F, 3)
       + ''.join(path(f'M{12 + i * 11},50 L{20 + i * 11},14 L{26 + i * 11},16 L{19 + i * 11},50 Z', '#BFE7F7' if i % 2 else '#A9DDF2', 1.3) for i in range(4)))
device('INSULATION', shadow(25) + cyl3(20, 30, 11, 24, PINK_T, PINK_F, 4) + cyl3(44, 30, 11, 24, YEL_T, YEL_F, 4)
       + cyl3(32, 14, 10, 16, '#FFE0E8', PINK_T, 3.6)
       + ''.join(stroke_path(f'M{x - 5},{y} a5,1.8 0 1,0 10,0 a3,1 0 1,0 -6,0', '#00000022', 1) for x, y in [(20, 30), (44, 30), (32, 14)]))
device('STOVE_PIPE', shadow(20) + path('M14,30 C14,18 44,18 44,30 L42,50 C42,55 16,55 16,50 Z', '#5C5763')
       + rect(21, 34, 16, 12, GLOW, 5) + rect(38, 6, 7, 18, '#7A7584', 3) + rect(12, 52, 34, 4, '#7A7584', 2)
       + shine(20, 26, 3, 1.4),
       fx=[(GLOWFX, 29, 40, 14, 0xFFFF8A2B), (STEAM, 41.5, 4, 0, 0)])
device('CARGO_NET', shadow(26) + crate(8, 30, 21) + crate(32, 31, 21, '#F2CB98', '#E0AE74') + crate(20, 13, 20)
       + ''.join(stroke_path(f'M{6 + i * 9},12 L{4 + i * 9},56', '#F5E7C8', 1.3) for i in range(7))
       + ''.join(stroke_path(f'M4,{16 + j * 9} L60,{14 + j * 9}', '#F5E7C8', 1.3) for j in range(5)))
device('LANTERN_RACK', rect(4, 10, 56, 5, WOOD_F, 2.5)
       + ''.join(line(x, 15, x, 22, OL, 1.2) + rect(x - 6, 22, 12, 16, GLOW, 4) + rect(x - 7, 20, 14, 4, '#7A7584', 2) + rect(x - 5, 38, 10, 3, '#7A7584', 1.5)
                 for x in (14, 32, 50)),
       fx=[(GLOWFX, 14, 30, 14, 0xFFFFB347), (GLOWFX, 32, 30, 14, 0xFFFFB347), (GLOWFX, 50, 30, 14, 0xFFFFB347)])
device('COOLING_FAN', shadow(22) + rect(10, 8, 44, 44, MINT_F, 8) + circ(32, 30, 18, '#E8F6F0') + rect(26, 50, 12, 6, MINT_F, 2),
       rotor=(''.join(ell(32 + 9 * math.cos(a), 30 + 9 * math.sin(a), 9, 4.6, MINT_T, 1.3).replace('<ellipse', f'<ellipse transform="rotate({math.degrees(a)} {32 + 9 * math.cos(a)} {30 + 9 * math.sin(a)})"')
                      for a in [i * 2 * math.pi / 3 for i in range(3)]) + circ(32, 30, 4, MINT_F)
              + circ(32, 30, 18, 'none', 1.4).replace('fill="none"', 'fill="none" opacity=".6"'), 32, 30, 400, 0))
device('BOILER_RIVETS', shadow(22) + path('M18,56 L24,22 M46,56 L40,22', 'none') + line(18, 56, 24, 30, WOOD_D, 2.4) + line(46, 56, 40, 30, WOOD_D, 2.4)
       + rect(12, 10, 40, 30, STEEL_F, 4) + ''.join(circ(18 + i * 7, 16 + j * 9, 1.6, STEEL_T, 0.9) for i in range(5) for j in range(3))
       + line(48, 36, 56, 22, WOOD_D, 2.2) + rect(52, 18, 9, 6, STEEL_D, 2))
device('WOOL_BLANKETS', shadow(25) + ''.join(rect(8 + i, 44 - i * 9, 48 - 2 * i, 11, c, 5) + line(12 + i, 49.5 - i * 9, 52 - i, 49.5 - i * 9, '#FFFFFF88', 1.2)
                                             for i, c in enumerate([RED_F, BLUE_F, GREEN_F, YEL_F])))
device('TEA_BREWER', shadow(25) + path('M14,34 C14,20 40,20 40,34 C40,46 14,46 14,34 Z', BLUE_F) + ell(27, 23, 9, 2.6, BLUE_T)
       + circ(27, 19, 2.6, CREAM, 1.2) + stroke_path('M40,30 C46,28 48,22 50,18', BLUE_F, 3.4) + stroke_path('M40,30 C46,28 48,22 50,18', OL, 0.0)
       + stroke_path('M14,28 C8,28 8,38 14,38', OL, 1.6) + rect(12, 44, 30, 4, CREAM, 2)
       + cyl3(50, 42, 5.5, 9, '#B9876A', CREAM, 1.8) + shine(20, 28, 2.6, 1.6),
       fx=[(STEAM, 50, 15, 0, 0)])
device('SIGNAL_BELL', rect(30, 4, 4, 8, WOOD_D, 1.5) + rect(16, 4, 32, 4, WOOD_F, 2)
       + path('M18,40 C18,22 22,12 32,12 C42,12 46,22 46,40 Z', 'url(#gold)') + rect(14, 38, 36, 5, '#E0A33A', 2.5)
       + circ(32, 46, 3.2, '#B07A2A', 1.2) + shine(25, 22, 2, 4),
       fx=[(RIPPLE, 32, 30, 24, 0xFFFFE680)])
device('GOLD_COUPLER', shadow(24) + rect(4, 34, 12, 12, STEEL_F, 3) + rect(48, 34, 12, 12, STEEL_F, 3)
       + circ(24, 34, 10, 'none', 0).replace('fill="none"', 'fill="none"') + f'<circle cx="24" cy="36" r="9" fill="none" stroke="{OL}" stroke-width="6"/><circle cx="24" cy="36" r="9" fill="none" stroke="#F2C35A" stroke-width="3.4"/>'
       + f'<circle cx="40" cy="36" r="9" fill="none" stroke="{OL}" stroke-width="6"/><circle cx="40" cy="36" r="9" fill="none" stroke="#FFE08A" stroke-width="3.4"/>'
       + shine(20, 28, 2, 1))
device('TURBO_VALVE', shadow(24) + rect(4, 40, 56, 10, STEEL_F, 5) + rect(26, 30, 12, 22, STEEL_D, 3) + rect(4, 38, 6, 14, STEEL_T, 2) + rect(54, 38, 6, 14, STEEL_T, 2),
       rotor=(f'<circle cx="32" cy="22" r="13" fill="none" stroke="{OL}" stroke-width="6"/><circle cx="32" cy="22" r="13" fill="none" stroke="{RED_F}" stroke-width="3.4"/>'
              + ''.join(line(32, 22, 32 + 13 * math.cos(i * 2.094), 22 + 13 * math.sin(i * 2.094), OL, 3.2) + line(32, 22, 32 + 13 * math.cos(i * 2.094), 22 + 13 * math.sin(i * 2.094), RED_T, 1.6) for i in range(3))
              + circ(32, 22, 3.5, STEEL_T), 32, 22, 60, 0))
device('ICE_CUTTER', shadow(24) + box3(20, 40, 40, 16, 7, STEEL_T, STEEL_F, 3) + box3(2, 44, 16, 12, 6, '#F2FBFF', '#BFE7F7', 2),
       rotor=(''.join(path(f'M{32 + 16 * math.cos(a)},{28 + 16 * math.sin(a)} L{32 + 21 * math.cos(a + 0.18)},{28 + 21 * math.sin(a + 0.18)} L{32 + 16 * math.cos(a + 0.36)},{28 + 16 * math.sin(a + 0.36)} Z', '#E8EEF4', 1.0) for a in [i * 2 * math.pi / 16 for i in range(16)])
              + circ(32, 28, 17, '#DCE3EA') + circ(32, 28, 5, STEEL_D), 32, 28, 480, 0))
device('GREASE_PUMP', shadow(20) + cyl3(26, 20, 15, 34, '#E2A35E', '#C9853D', 5) + line(12, 32, 40, 32, '#9A6424', 1.6) + line(12, 44, 40, 44, '#9A6424', 1.6)
       + line(26, 20, 46, 8, STEEL_D, 2.6) + circ(47, 7, 3, RED_F, 1.2) + ell(50, 54, 4, 1.6, '#FFC85C', 1))
device('FROST_RADAR', shadow(18) + rect(28, 34, 8, 20, STEEL_D, 3) + rect(20, 52, 24, 5, STEEL_F, 2.5)
       + path('M10,26 C14,8 50,8 54,26 C44,32 20,32 10,26 Z', '#E8EEF4') + line(32, 22, 32, 10, STEEL_D, 1.6) + circ(32, 9, 2.4, RED_F, 1.1),
       fx=[(BLINK, 32, 9, 6, 0xFFFF6B6B), (RIPPLE, 32, 18, 22, 0xFF8FDBA8)])
device('FUEL_INJECTOR', shadow(22) + cyl3(24, 18, 15, 36, GREEN_T, GREEN_F, 5) + line(10, 32, 38, 32, '#5E9A78', 1.6)
       + stroke_path('M38,36 C50,36 54,44 50,52', OL, 4.4) + stroke_path('M38,36 C50,36 54,44 50,52', '#4A4550', 2.4)
       + rect(44, 50, 10, 6, STEEL_F, 2) + shine(16, 26, 2.4, 5),
       fx=[(GLOWFX, 49, 53, 10, 0xFFFF8A2B)])
device('CABIN_HEATER', shadow(24) + ''.join(rect(10 + i * 7, 14, 7, 38, RED_F if i % 2 else RED_T, 3.4) for i in range(6))
       + rect(8, 50, 48, 5, '#C25555', 2.5) + rect(8, 12, 48, 5, '#C25555', 2.5),
       fx=[(GLOWFX, 32, 32, 26, 0xFFFF7B5C)])
device('CARGO_CRANE', shadow(24) + box3(10, 40, 22, 16, 8, YEL_T, YEL_F, 3) + crate(42, 40, 14),
       rotor=(rect(16, 14, 40, 6, YEL_F, 3) + line(20, 15, 24, 19, '#D8A23A', 1) + line(28, 15, 32, 19, '#D8A23A', 1) + line(36, 15, 40, 19, '#D8A23A', 1)
              + rect(18, 14, 6, 30, YEL_F, 3) + line(52, 20, 52, 32, OL, 1.2) + path('M50,32 L54,32 L54,36 C54,39 49,39 50,36', 'none', 1.4),
              21, 42, 0, 25))
device('LOUNGE', shadow(26) + rect(8, 28, 40, 26, '#D1856B', 9) + rect(8, 16, 40, 18, '#B86A55', 9) + rect(4, 28, 10, 24, '#B86A55', 5)
       + rect(42, 28, 10, 24, '#B86A55', 5) + rect(14, 34, 28, 10, '#E7A28A', 5)
       + cyl3(54, 44, 6, 10, WOOD_T, WOOD_F, 2) + circ_n(54, 43, 2, CREAM))
device('NIGHT_WATCH', shadow(18) + rect(26, 48, 12, 8, WOOD_F, 3) + circ(32, 28, 19, NAVY) + circ(32, 28, 15, '#2E4058', 0)
       + path('M28,16 C20,20 20,36 30,40 C22,40 16,32 18,24 C20,18 24,16 28,16 Z', '#FFE680', 1.1)
       + ''.join(circ_n(x, y, 1.1, WHITE) for x, y in [(38, 18), (42, 30), (36, 38), (44, 24)]),
       fx=[(GLOWFX, 26, 28, 16, 0xFFFFE680)])
device('BANK_SAFE', shadow(24) + box3(10, 12, 44, 44, 10, STEEL_T, STEEL_F, 5) + rect(14, 20, 36, 32, '#AFC0D2', 4)
       + circ(30, 36, 8, STEEL_D) + rect(42, 30, 3, 12, '#F2B84B', 1.5)
       + ''.join(ell(12 + i * 1, 54 - i * 2.4, 6, 2, '#FFD66B', 1) for i in range(3)),
       rotor=(circ(30, 36, 5, '#DCE3EA', 1.2) + line(30, 36, 30, 31.5, OL, 1.4) + line(30, 36, 34, 38, OL, 1.1), 30, 36, 0, 70))
device('COMPASS', shadow(18) + rect(26, 46, 12, 10, WOOD_F, 3) + circ(32, 28, 20, 'url(#gold)') + circ(32, 28, 16, CREAM)
       + ''.join(line(32 + 12 * math.cos(a), 28 + 12 * math.sin(a), 32 + 15 * math.cos(a), 28 + 15 * math.sin(a), OL, 1.3) for a in [i * math.pi / 2 for i in range(4)]),
       rotor=(path('M32,15 L35,28 L29,28 Z', RED_F, 1) + path('M32,41 L35,28 L29,28 Z', '#DCE3EA', 1) + circ(32, 28, 2, '#F2B84B', 0.9), 32, 28, 0, 40))
device('GOVERNOR', shadow(20) + rect(29, 16, 6, 38, STEEL_D, 2.5) + rect(18, 50, 28, 6, STEEL_F, 3)
       + line(32, 18, 16, 32, STEEL_D, 2) + line(32, 18, 48, 32, STEEL_D, 2) + circ(15, 33, 6, RED_F) + circ(49, 33, 6, RED_F)
       + circ(32, 14, 3.4, '#F2B84B', 1.2) + shine(13, 31, 1.6, 1.2) + shine(47, 31, 1.6, 1.2))
device('MAGNET_COUPLER', shadow(22) + f'<path d="M16,14 L16,34 C16,50 48,50 48,34 L48,14" fill="none" stroke="{OL}" stroke-width="14"/>'
       + f'<path d="M16,14 L16,34 C16,50 48,50 48,34 L48,14" fill="none" stroke="{RED_F}" stroke-width="10"/>'
       + rect(10, 6, 12, 9, '#DCE3EA', 2) + rect(42, 6, 12, 9, '#DCE3EA', 2),
       fx=[(RIPPLE, 32, 10, 18, 0xFF8FD0FF)])
device('TIMETABLE', rect(2, 10, 60, 40, '#3D4A5C', 5) + rect(2, 6, 60, 6, '#F2B84B', 3)
       + ''.join(rect_n(8, 18 + i * 8, 32 - (i % 3) * 5, 4, '#FFE680' if i % 2 == 0 else '#8FD0FF') + circ_n(54, 20 + i * 8, 2, '#8FDBA8' if i % 2 == 0 else '#F28C8C') for i in range(4))
       + line(16, 50, 16, 58, OL, 2) + line(48, 50, 48, 58, OL, 2),
       fx=[(GLOWFX, 32, 30, 22, 0xFFFFE680)])
device('AURORA_PRISM', shadow(20) + rect(18, 48, 28, 8, STEEL_F, 3) + path('M32,8 L48,46 L16,46 Z', 'url(#ice)')
       + path('M32,8 L38,46 L26,46 Z', '#FFFFFF', 0).replace('fill="#FFFFFF"', 'fill="#FFFFFF" opacity=".45"'),
       fx=[(RAINBOW, 38, 30, 24, 0)])
device('TRACTION_SAND', shadow(24) + box3(8, 24, 48, 32, 12, WOOD_T, WOOD_F, 5) + path('M14,30 C18,18 46,18 50,30 Z', '#F2DDA6', 1.3)
       + ''.join(circ_n(18 + i * 5, 27 - (i % 2), 0.8, '#C9A86A') for i in range(6)) + line(44, 10, 54, 34, WOOD_D, 2.2) + rect(50, 32, 8, 6, STEEL_F, 2))
device('CHEFS_OVEN', shadow(25) + box3(8, 14, 48, 42, 14, '#5C5763', '#46424F', 6) + rect(16, 34, 32, 16, GLOW, 5)
       + ell(20, 20, 6, 2.2, '#2E2B35', 1.1) + ell(44, 20, 6, 2.2, '#2E2B35', 1.1) + ''.join(circ(16 + i * 8, 30, 1.6, '#DCE3EA', 0.8) for i in range(5)),
       fx=[(GLOWFX, 32, 42, 16, 0xFFFF8A2B), (STEAM, 20, 16, 0, 0)])
device('BOGIE_SPRINGS', shadow(24) + rect(6, 50, 52, 6, STEEL_F, 3) + rect(6, 10, 52, 6, STEEL_F, 3)
       + ''.join(stroke_path(f'M{x - 7},{16 + k * 6} L{x + 7},{19 + k * 6}', OL, 3.6) + stroke_path(f'M{x - 7},{16 + k * 6} L{x + 7},{19 + k * 6}', '#DCE3EA', 1.8)
                 for x in (20, 44) for k in range(6)))
device('POLAR_BEACON', shadow(18) + path('M24,56 L27,24 L37,24 L40,56 Z', WHITE) + rect(25, 34, 14, 6, RED_F, 1.5)
       + rect(22, 14, 20, 12, '#BFEFFF', 4) + path('M20,14 C20,6 44,6 44,14 Z', RED_F) + circ(32, 20, 3.2, '#FFF3B0', 1),
       fx=[(GLOWFX, 32, 20, 26, 0xFF8FE9FF), (BLINK, 32, 20, 10, 0xFFBFF4FF)])



# a little life for every device that has no effect of its own yet
MORE_FX = {
    'WAGON': [(SPARKLE, 32, 24, 18, 0xFFFFF3B0)],
    'COAL': [(GLOWFX, 32, 26, 14, 0xFFFF8A2B), (SPARKLE, 30, 22, 14, 0xFFFFB347)],
    'PLOW': [(SPARKLE, 32, 30, 20, 0xFFFFFFFF), (WIND, 32, 34, 22, 0xFFFFFFFF)],
    'CONDUCTOR': [(ZZZ, 18, 22, 0, 0)],
    'WATER_TANK': [(BUBBLES, 47, 36, 10, 0xFFFFFFFF)],
    'COAL_TENDER': [(GLOWFX, 32, 26, 12, 0xFFFF8A2B), (SPARKLE, 32, 22, 14, 0xFFFFB347)],
    'BRASS_PISTONS': [(STEAM, 20, 12, 0, 0), (STEAM, 44, 16, 0, 0)],
    'TICKET_OFFICE': [(SPARKLE, 45, 44, 8, 0xFFFFE08A)],
    'SPARE_CRATES': [(SPARKLE, 30, 26, 18, 0xFFFFF3B0)],
    'WIND_DEFLECTOR': [(WIND, 34, 30, 24, 0xFFFFFFFF)],
    'INSULATION': [(SPARKLE, 32, 22, 18, 0xFFFFFFFF)],
    'CARGO_NET': [(SPARKLE, 32, 26, 18, 0xFFFFF3B0)],
    'BOILER_RIVETS': [(SPARKLE, 56, 22, 8, 0xFFFFC85C)],
    'WOOL_BLANKETS': [(ZZZ, 44, 14, 0, 0)],
    'GOLD_COUPLER': [(SPARKLE, 32, 34, 16, 0xFFFFE08A)],
    'ICE_CUTTER': [(SPARKLE, 10, 46, 10, 0xFFDFF6FF)],
    'GREASE_PUMP': [(DRIP, 50, 50, 0, 0xFFFFC85C)],
    'LOUNGE': [(STEAM, 54, 40, 0, 0)],
    'BANK_SAFE': [(SPARKLE, 16, 50, 10, 0xFFFFE08A)],
    'COMPASS': [(SPARKLE, 32, 28, 18, 0xFFFFF3B0)],
    'GOVERNOR': [(SPARKLE, 32, 30, 20, 0xFFFFFFFF)],
    'TRACTION_SAND': [(SPARKLE, 32, 24, 16, 0xFFFFF0C8)],
    'BOGIE_SPRINGS': [(SPARKLE, 32, 30, 20, 0xFFFFFFFF)],
    'SNOW_CHAINS': [(SPARKLE, 32, 32, 22, 0xFFFFFFFF)],
    'ROOF_FAN': [(WIND, 32, 50, 16, 0xFFFFFFFF)],
    'FLYWHEEL': [(SPARKLE, 32, 30, 20, 0xFFFFFFFF)],
    'COOLING_FAN': [(WIND, 32, 54, 18, 0xFFDFF6FF)],
    'TURBO_VALVE': [(STEAM, 8, 36, 0, 0)],
    'CARGO_CRANE': [(SPARKLE, 49, 44, 10, 0xFFFFF3B0)],
    'NIGHT_WATCH': [(SPARKLE, 40, 26, 12, 0xFFFFFFFF)],
}
for _t, _fx in MORE_FX.items():
    FX.setdefault(_t, []).extend(_fx)


# ---------------- furniture for the engine and utility cars (1 unit = 1 dp, 4 dp margin) ----------------
FURN = {}


def furn(name, w, h, body):
    FURN[name] = (w + 8, h + 8, f'<g transform="translate(4 4)">{body}</g>')


def fo(sw=2.2):
    return sw


furn('core_boiler', 60, 54, f'''<ellipse cx="30" cy="52" rx="30" ry="4" fill="#000" opacity=".09"/>
{rect(0, 14, 60, 40, STEEL_F, 10, 2.4)}{rect(0, 0, 60, 30, STEEL_T, 10, 2.4)}
{rect(22, 30, 18, 18, GLOW, 6, 2.2)}{circ(14, 14, 7, CREAM, 2)}{line(14, 14, 18, 10, RED_F, 2)}
{rect(44, 2, 10, 16, STEEL_F, 4, 2)}{shine(28, 7, 9, 2)}''')
furn('core_coalbin', 54, 40, f'''<ellipse cx="27" cy="38" rx="27" ry="4" fill="#000" opacity=".09"/>
{rect(0, 10, 54, 30, WOOD_F, 8, 2.4)}{rect(0, 0, 54, 20, WOOD_T, 8, 2.4)}
{''.join(circ(8 + i * 7.4, 9 + (i % 2) * 4, 5, COAL, 1.6) for i in range(6))}{circ_n(20, 6, 1.4, WHITE, .5)}''')
furn('core_console', 62, 26, f'''<ellipse cx="31" cy="25" rx="31" ry="3" fill="#000" opacity=".09"/>
{rect(0, 8, 62, 18, MINT_F, 7, 2.4)}{rect(0, 0, 62, 16, MINT_T, 7, 2.4)}
{rect(5, 3, 24, 10, NAVY, 3, 1.8)}{stroke_path('M8,10 l4,-3 l4,2 l5,-4 l4,3', '#9FF0D0', 1.6)}
{circ(36, 8, 2.6, RED_F, 1.4)}{circ(43, 8, 2.6, YEL_F, 1.4)}{circ(48, 13, 6.5, '#F2B84B', 1.8)}''')
furn('core_bunk', 50, 28, f'''<ellipse cx="25" cy="27" rx="25" ry="3" fill="#000" opacity=".09"/>
{rect(0, 8, 50, 20, WOOD_F, 7, 2.4)}{rect(2, 2, 46, 18, CREAM, 6, 2)}{rect(17, 3, 31, 17, BLUE_F, 6, 2)}
{line(28, 6, 28, 16, '#7FA9D6', 1.6)}{line(38, 6, 38, 16, '#7FA9D6', 1.6)}{rect(5, 5, 10, 12, WHITE, 4, 1.8)}''')
furn('core_locker', 50, 28, f'''<ellipse cx="25" cy="27" rx="25" ry="3" fill="#000" opacity=".09"/>
{rect(0, 4, 50, 24, '#B6C3D2', 5, 2.4)}{rect(0, 0, 50, 10, '#CFD9E4', 5, 2.4)}
{line(16.6, 10, 16.6, 26, OL, 1.4)}{line(33.3, 10, 33.3, 26, OL, 1.4)}
{''.join(rect(6 + k * 16.6, 16, 4, 2, OL, 1, 1) for k in range(3))}''')
furn('core_heater', 54, 54, f'''<ellipse cx="27" cy="52" rx="27" ry="4" fill="#000" opacity=".09"/>
{rect(0, 14, 54, 40, BLUE_F, 10, 2.4)}{rect(0, 0, 54, 28, BLUE_T, 10, 2.4)}
{rect(12, 28, 30, 20, GLOW, 7, 2.2)}{stroke_path('M16,38 C20,33 24,43 28,38 C32,33 36,43 40,38', '#FF8F5A', 2.4)}{shine(20, 6, 9, 2)}''')
furn('core_kitchen', 54, 66, f'''<ellipse cx="27" cy="64" rx="27" ry="4" fill="#000" opacity=".09"/>
{rect(0, 10, 54, 56, '#E98E6E', 9, 2.4)}{rect(0, 0, 54, 60, '#F5B094', 9, 2.4)}
{circ(10, 22, 8, '#5C5763', 2)}{circ(10, 48, 8, '#5C5763', 2)}{circ(10, 48, 4, '#7A7584', 1.4)}
{rect(28, 8, 20, 26, WOOD_T, 4, 2)}{circ(38, 18, 4, RED_F, 1.6)}{circ(40, 50, 7, WHITE, 2)}''')
furn('core_table', 62, 32, f'''<ellipse cx="31" cy="31" rx="31" ry="3" fill="#000" opacity=".09"/>
{rect(0, 6, 9, 24, WOOD_D, 4, 2)}{rect(53, 6, 9, 24, WOOD_D, 4, 2)}
{rect(9, 6, 44, 26, WOOD_F, 8, 2.4)}{rect(9, 0, 44, 24, WOOD_T, 8, 2.4)}
{circ(41, 12, 6, WHITE, 1.8)}{circ(41, 12, 3, '#FFC85C', 1.2)}{rect(17, 6, 7, 9, BLUE_F, 3, 1.6)}''')
furn('core_crate', 20, 20, crate(0, 0, 20))


# ---------------- scenery, worker, train body ----------------
MISC = {}


def misc(name, w, h, body, scale=4):
    MISC[name] = (w, h, body, scale)


def misc_body(name):
    return MISC[name][2]


misc('spr_pine_a', 64, 64, f'''<ellipse cx="34" cy="52" rx="22" ry="6" fill="#C9DCEC"/>
<rect x="29" y="44" width="6" height="9" rx="2.5" fill="#B9876A" {o(2)}/>
<path d="M32,6 C40,14 45,22 41,25 C49,29 53,37 48,40 C56,44 57,50 51,51 L13,51 C7,50 8,44 16,40 C11,37 15,29 23,25 C19,22 24,14 32,6 Z" fill="#8FCBA6" {o(2.4)}/>
<path d="M20,44 C28,48 40,47 46,43" fill="none" stroke="#6FB08B" stroke-width="2.6" stroke-linecap="round"/>
<path d="M32,6 C36,10 39,14 37,17 C33,19 28,18 26,16 C26,12 29,9 32,6 Z" fill="#fff"/>
<path d="M23,25 C27,28 37,28 41,25 C37,29 27,29 23,25 Z" fill="#fff"/><path d="M16,40 C24,44 40,44 48,40 C44,45 20,45 16,40 Z" fill="#fff"/>''')
misc('spr_pine_b', 64, 64, f'''<ellipse cx="34" cy="53" rx="20" ry="5" fill="#C9DCEC"/>
<rect x="29" y="46" width="6" height="8" rx="2.5" fill="#B9876A" {o(2)}/>
<path d="M32,4 C39,12 46,24 43,27 C50,32 54,42 47,46 L17,46 C10,42 14,32 21,27 C18,24 25,12 32,4 Z" fill="#7FC29A" {o(2.4)}/>
<path d="M32,4 C35,8 38,12 36,15 C32,16 29,15 28,13 C28,10 30,7 32,4 Z" fill="#fff"/><path d="M21,27 C26,31 38,31 43,27 C38,32 26,32 21,27 Z" fill="#fff"/>''')
misc('spr_pine_c', 64, 64, f'''<ellipse cx="33" cy="54" rx="24" ry="6" fill="#C9DCEC"/>
<rect x="29" y="46" width="6" height="9" rx="2.5" fill="#B9876A" {o(2)}/>
<circle cx="32" cy="30" r="20" fill="#9AD0AF" {o(2.4)}/><circle cx="24" cy="24" r="6" fill="#fff"/><circle cx="38" cy="18" r="4.5" fill="#fff"/><circle cx="40" cy="32" r="3" fill="#F28C8C" {o(1.2)}/><circle cx="26" cy="36" r="3" fill="#F28C8C" {o(1.2)}/>''')
misc('spr_rock_a', 48, 48, f'''<ellipse cx="25" cy="40" rx="18" ry="4.5" fill="#C9DCEC"/>
<path d="M8,38 C6,24 16,12 26,14 C36,12 44,24 41,38 Z" fill="#B7C1CC" {o(2.2)}/><path d="M14,22 C20,13 34,12 38,22 C32,20 22,20 14,22 Z" fill="#fff"/>''')
misc('spr_rock_b', 48, 48, f'''<ellipse cx="24" cy="40" rx="17" ry="4" fill="#C9DCEC"/>
<path d="M9,38 C8,30 12,22 20,22 C24,16 34,18 36,26 C42,28 42,36 40,38 Z" fill="#C3CBD5" {o(2.2)}/><path d="M16,26 C20,20 30,19 34,25 C28,24 22,24 16,26 Z" fill="#fff"/>''')
misc('spr_shrub', 48, 48, f'''<ellipse cx="24" cy="40" rx="18" ry="4.5" fill="#C9DCEC"/>
<path d="M8,38 C3,28 10,20 18,22 C20,14 30,14 32,22 C40,20 46,30 40,38 Z" fill="#9AD0AF" {o(2.2)}/><path d="M16,23 C20,19 28,19 31,23 C26,25 20,25 16,23 Z" fill="#fff"/>
<circle cx="18" cy="31" r="2.4" fill="#F28C8C" {o(1)}/><circle cx="30" cy="33" r="2.4" fill="#F28C8C" {o(1)}/>''')

# the worker, front facing (head, hat and scarf); the jacket colour comes from code as a tint of a light layer
WORKER = f'''<ellipse cx="16" cy="38" rx="11" ry="3" fill="#000" opacity=".12"/>
<path d="M6,36 C5,26 9,21 16,21 C23,21 27,26 26,36 Z" fill="#FFB86B" {o(1.7)}/>
<path d="M8,24 C12,27 20,27 24,24" fill="none" stroke="{RED_F}" stroke-width="3.6" stroke-linecap="round"/>
<path d="M21,26 L23,32" stroke="{RED_F}" stroke-width="3" stroke-linecap="round"/>
<circle cx="16" cy="13" r="10.5" fill="#FFE2C6" {o(1.7)}/>
<path d="M5.4,12 C5,1.6 27,1.6 26.6,12 C21.6,9.6 10.4,9.6 5.4,12 Z" fill="#7FB8E6" {o(1.7)}/>
<rect x="5" y="9.4" width="22" height="4" rx="2" fill="#A9D3F2" {o(1.4)}/>
<circle cx="16" cy="2.2" r="2.8" fill="#fff" {o(1.4)}/>
<ellipse cx="12.4" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/><ellipse cx="19.6" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/>
<circle cx="12.8" cy="15.8" r=".5" fill="#fff"/><circle cx="20" cy="15.8" r=".5" fill="#fff"/>
<ellipse cx="9.6" cy="19.2" rx="2" ry="1.2" fill="#F7A1A1" opacity=".85"/><ellipse cx="22.4" cy="19.2" rx="2" ry="1.2" fill="#F7A1A1" opacity=".85"/>
<path d="M14.4,19.6 C15.2,20.8 16.8,20.8 17.6,19.6" fill="none" stroke="{OL}" stroke-width="1" stroke-linecap="round"/>'''
misc('npc_front', 32, 40, WORKER, 6)
misc('npc_sleep', 32, 40, WORKER.replace('<ellipse cx="12.4" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/><ellipse cx="19.6" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/>',
                                         f'<path d="M10.8,16.6 C11.8,17.6 13,17.6 14,16.6 M18,16.6 C19,17.6 20.2,17.6 21.2,16.6" fill="none" stroke="{OL}" stroke-width="1" stroke-linecap="round"/>')
     .replace('<circle cx="12.8" cy="15.8" r=".5" fill="#fff"/><circle cx="20" cy="15.8" r=".5" fill="#fff"/>', ''), 6)

# train walls: a light fill (tinted with the train colour in code) and a line layer on top
WALL_W, WALL_H, M = 190, 116, 8
misc('car_fill', WALL_W + 2 * M, WALL_H + 2 * M, f'''<rect x="{M + 3}" y="{M + 6}" width="{WALL_W}" height="{WALL_H}" rx="16" fill="#000" opacity=".10"/>
<path fill-rule="evenodd" d="M{M + 14},{M} H{M + WALL_W - 14} A14,14 0 0 1 {M + WALL_W},{M + 14} V{M + WALL_H - 14} A14,14 0 0 1 {M + WALL_W - 14},{M + WALL_H} H{M + 14} A14,14 0 0 1 {M},{M + WALL_H - 14} V{M + 14} A14,14 0 0 1 {M + 14},{M} Z
M{M + 16},{M + 8} H{M + WALL_W - 16} A8,8 0 0 1 {M + WALL_W - 8},{M + 16} V{M + WALL_H - 16} A8,8 0 0 1 {M + WALL_W - 16},{M + WALL_H - 8} H{M + 16} A8,8 0 0 1 {M + 8},{M + WALL_H - 16} V{M + 16} A8,8 0 0 1 {M + 16},{M + 8} Z" fill="#FFFFFF"/>''', 4)
misc('car_line', WALL_W + 2 * M, WALL_H + 2 * M, f'''<rect x="{M}" y="{M}" width="{WALL_W}" height="{WALL_H}" rx="14" fill="none" stroke="{OL}" stroke-width="2.4"/>
<rect x="{M + 8}" y="{M + 8}" width="{WALL_W - 16}" height="{WALL_H - 16}" rx="8" fill="none" stroke="{OL}" stroke-width="1.8"/>
<rect x="{M + 6}" y="{M + 2}" width="{WALL_W - 12}" height="3" rx="1.5" fill="#FFFFFF" opacity=".45"/>
''' + ''.join(f'<rect x="{M - 2}" y="{M + wy}" width="6" height="16" rx="3" fill="#BEE6F7" stroke="{OL}" stroke-width="1.4"/><rect x="{M + WALL_W - 4}" y="{M + wy}" width="6" height="16" rx="3" fill="#BEE6F7" stroke="{OL}" stroke-width="1.4"/>'
              for wy in range(16, WALL_H - 20, 24)), 4)
NW = 190
misc('nose_fill', NW + 12, 44, f'''<ellipse cx="{6 + NW / 2}" cy="38" rx="{NW / 2}" ry="5" fill="#000" opacity=".10"/>
<path d="M{6 + 14},36 C{6 + 14},14 {6 + 34},6 {6 + 54},6 L{6 + NW - 54},6 C{6 + NW - 34},6 {6 + NW - 14},14 {6 + NW - 14},36 Z" fill="#FFFFFF"/>''', 4)
misc('nose_line', NW + 12, 44, f'''<path d="M{6 + 14},36 C{6 + 14},14 {6 + 34},6 {6 + 54},6 L{6 + NW - 54},6 C{6 + NW - 34},6 {6 + NW - 14},14 {6 + NW - 14},36" fill="none" stroke="{OL}" stroke-width="2.4" stroke-linejoin="round"/>
<path d="M{6 + 24},33 C{6 + 25},19 {6 + 38},12 {6 + 54},12 L{6 + NW - 54},12 C{6 + NW - 38},12 {6 + NW - 25},19 {6 + NW - 24},33" fill="none" stroke="#FFFFFF" stroke-width="3" opacity=".45" stroke-linecap="round"/>
<circle cx="{6 + 56}" cy="20" r="6" fill="#FFF3C4" stroke="{OL}" stroke-width="2"/><circle cx="{6 + NW - 56}" cy="20" r="6" fill="#FFF3C4" stroke="{OL}" stroke-width="2"/>
<circle cx="{6 + 54.5}" cy="18.5" r="1.8" fill="#fff"/><circle cx="{6 + NW - 57.5}" cy="18.5" r="1.8" fill="#fff"/>''', 4)
misc('station_roof', 26, 88, f'''<rect x="5" y="6" width="18" height="80" rx="5" fill="#000" opacity=".10"/>
<rect x="3" y="3" width="18" height="80" rx="5" fill="#D9A576" {o(2)}/>{''.join(f'<line x1="4" y1="{12 + k * 8}" x2="20" y2="{12 + k * 8}" stroke="#B9876A" stroke-width="1.2"/>' for k in range(9))}
<path d="M3,8 C3,4 21,4 21,8 L21,30 C17,33 7,33 3,30 Z" fill="#fff" {o(1.8)}/>''', 6)


# ---------------- item and menu icons (48 x 48 units) ----------------
icon = lambda name, body: misc(name, 48, 48, body, 4)
icon('item_cocoa', f'''{ell_n(24, 42, 15, 3, '#000', .1)}
<path d="M10,16 L38,16 L35,38 C35,42 13,42 13,38 Z" fill="#F28C8C" {o(2)}/>
<path d="M37,20 C46,20 46,32 36,32" fill="none" stroke="{OL}" stroke-width="5" stroke-linecap="round"/><path d="M37,20 C46,20 46,32 36,32" fill="none" stroke="#F28C8C" stroke-width="2.4" stroke-linecap="round"/>
<ellipse cx="24" cy="16" rx="14" ry="4" fill="#8B5E3C" {o(2)}/><circle cx="19" cy="15" r="3" fill="#fff" {o(1.2)}/><circle cx="26" cy="14" r="2.6" fill="#fff" {o(1.2)}/>
<path d="M18,10 C15,6 21,4 18,0 M28,10 C25,6 31,4 28,0" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/>
<path d="M18,26 l3,-3 l3,3 l-3,3 Z" fill="#fff" opacity=".7"/>''')
icon('item_turbo', f'''{ell_n(24, 42, 15, 3, '#000', .1)}
<path d="M24,4 C34,12 38,20 36,28 C35,36 30,40 24,40 C18,40 13,36 12,28 C11,22 15,18 18,14 C19,20 22,22 24,20 C22,14 22,8 24,4 Z" fill="url(#glow)" {o(2)}/>
<path d="M24,22 C29,26 30,32 26,36 C22,37 19,34 20,30 C21,27 23,26 24,22 Z" fill="#FF8F5A"/>
<ellipse cx="24" cy="40" rx="12" ry="4.5" fill="#4A4550" {o(2)}/><circle cx="18" cy="39" r="1.4" fill="#fff" opacity=".6"/>''')
icon('item_ticket', f'''{ell_n(24, 42, 16, 3, '#000', .1)}
<path d="M5,14 L43,14 L43,20 C40,20 40,28 43,28 L43,34 L5,34 L5,28 C8,28 8,20 5,20 Z" fill="url(#gold)" {o(2)}/>
<line x1="33" y1="16" x2="33" y2="32" stroke="{OL}" stroke-width="1.4" stroke-dasharray="2 2"/>
<path d="M18,18 l2.2,4.4 l4.8,.6 l-3.5,3.3 l.9,4.7 l-4.4,-2.3 l-4.4,2.3 l.9,-4.7 l-3.5,-3.3 l4.8,-.6 Z" fill="#fff" {o(1.2)}/>''')
icon('item_tank', f'''{ell_n(24, 42, 14, 3, '#000', .1)}
<rect x="11" y="10" width="26" height="31" rx="7" fill="#8FC2AE" {o(2)}/><rect x="19" y="5" width="10" height="7" rx="2.5" fill="#B9C8D8" {o(1.8)}/>
<rect x="16" y="16" width="16" height="20" rx="4" fill="#E8F6F0" {o(1.4)}/><rect x="16" y="24" width="16" height="12" rx="4" fill="#FFC85C"/>
<path d="M25,17 L20,27 L24,27 L22,35 L28,24 L24,24 Z" fill="#fff" {o(1)}/>''')
icon('item_clear', f'''{ell_n(24, 42, 16, 3, '#000', .1)}
<circle cx="30" cy="16" r="9" fill="#FFE49A" {o(2)}/>
{''.join(f'<line x1="{30 + 12 * math.cos(a)}" y1="{16 + 12 * math.sin(a)}" x2="{30 + 15 * math.cos(a)}" y2="{16 + 15 * math.sin(a)}" stroke="{OL}" stroke-width="2" stroke-linecap="round"/>' for a in [i * math.pi / 4 for i in range(8)])}
<path d="M8,36 C3,36 3,28 9,28 C9,22 18,20 21,25 C25,21 33,24 32,30 C37,30 37,36 32,36 Z" fill="#FFFFFF" {o(2)}/>
<path d="M14,40 l-2,4 M22,40 l-2,4" stroke="#8FBCE6" stroke-width="2" stroke-linecap="round"/>''')
icon('icon_shop', f'''<path d="M9,18 L39,18 L36,41 C36,43 12,43 12,41 Z" fill="#FFC9A0" {o(2.2)}/>
<path d="M17,18 C17,6 31,6 31,18" fill="none" stroke="{OL}" stroke-width="2.6" stroke-linecap="round"/>
<rect x="9" y="16" width="30" height="6" rx="3" fill="#F08A5D" {o(1.8)}/><circle cx="24" cy="31" r="4" fill="#FFE49A" {o(1.4)}/>''')
icon('icon_gems', f'''<path d="M13,10 L35,10 L43,20 L24,42 L5,20 Z" fill="#F49AB2" {o(2.2)}/>
<path d="M13,10 L35,10 L29,20 L19,20 Z" fill="#FFD6E2"/><path d="M5,20 L43,20 M19,20 L24,42 L29,20" fill="none" stroke="{OL}" stroke-width="1.4"/>
<circle cx="12" cy="8" r="2" fill="#fff"/><path d="M40,6 l1.2,2.4 l2.4,1.2 l-2.4,1.2 l-1.2,2.4 l-1.2,-2.4 l-2.4,-1.2 l2.4,-1.2 Z" fill="#fff" {o(1)}/>''')
icon('icon_bag', f'''<path d="M10,20 C10,13 38,13 38,20 L38,38 C38,42 10,42 10,38 Z" fill="#B8D8F2" {o(2.2)}/>
<path d="M17,15 C17,6 31,6 31,15" fill="none" stroke="{OL}" stroke-width="2.6" stroke-linecap="round"/>
<rect x="16" y="24" width="16" height="10" rx="3" fill="#8FBCE6" {o(1.6)}/><line x1="24" y1="24" x2="24" y2="28" stroke="{OL}" stroke-width="1.6"/>
<path d="M10,22 C16,24 32,24 38,22" fill="none" stroke="{OL}" stroke-width="1.4"/>''')


icon('icon_quest', f'''<rect x="10" y="6" width="28" height="36" rx="5" fill="#FFF6E9" {o(2.2)}/><rect x="17" y="3" width="14" height="7" rx="3" fill="#D9A576" {o(1.8)}/>
<path d="M15,18 l3,3 l5,-6" fill="none" stroke="#3F9E74" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/><line x1="26" y1="18" x2="33" y2="18" stroke="{OL}" stroke-width="2" stroke-linecap="round"/>
<path d="M15,28 l3,3 l5,-6" fill="none" stroke="#3F9E74" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/><line x1="26" y1="28" x2="33" y2="28" stroke="{OL}" stroke-width="2" stroke-linecap="round"/>
<circle cx="18" cy="37" r="2" fill="#F28C8C"/><line x1="26" y1="37" x2="31" y2="37" stroke="{OL}" stroke-width="2" stroke-linecap="round"/>''')
icon('icon_trophy', f'''<path d="M14,8 L34,8 L33,22 C32,30 16,30 15,22 Z" fill="url(#gold)" {o(2.2)}/>
<path d="M14,11 C6,11 6,22 15,22 M34,11 C42,11 42,22 33,22" fill="none" stroke="{OL}" stroke-width="2.2"/>
<rect x="21" y="29" width="6" height="7" fill="#F2B84B" {o(1.6)}/><rect x="14" y="35" width="20" height="7" rx="2.5" fill="#D9A576" {o(2)}/>
<path d="M24,12 l1.6,3.2 l3.5,.5 l-2.5,2.4 l.6,3.5 l-3.2,-1.7 l-3.2,1.7 l.6,-3.5 l-2.5,-2.4 l3.5,-.5 Z" fill="#fff" {o(1)}/>''')
icon('icon_gift', f'''<rect x="8" y="20" width="32" height="22" rx="4" fill="#F7A3A3" {o(2.2)}/><rect x="6" y="14" width="36" height="9" rx="3.5" fill="#F28C8C" {o(2.2)}/>
<rect x="21" y="14" width="6" height="28" fill="#FFE49A" {o(1.6)}/>
<path d="M24,14 C18,4 10,8 16,13 Z M24,14 C30,4 38,8 32,13 Z" fill="#FFE49A" {o(1.8)}/>''')
icon('icon_evolve', f'''<circle cx="24" cy="24" r="17" fill="#DCCBFA" {o(2.2)}/>
<path d="M24,10 L33,22 L28,22 L28,34 L20,34 L20,22 L15,22 Z" fill="#B9A2EE" {o(2)}/><circle cx="35" cy="12" r="2" fill="#fff"/><circle cx="12" cy="34" r="1.6" fill="#fff"/>''')


# ---------------- the opening screen illustration (portrait, 360 x 640 units) ----------------
def side_car(x, y, w, body, roof, windows=3):
    out = f'<rect x="{x}" y="{y}" width="{w}" height="46" rx="10" fill="{body}" {o(3)}/>'
    out += f'<rect x="{x - 4}" y="{y - 8}" width="{w + 8}" height="14" rx="7" fill="{roof}" {o(3)}/>'
    out += f'<path d="M{x + 2},{y - 8} C{x + w * 0.3},{y - 16} {x + w * 0.7},{y - 16} {x + w - 2},{y - 8}" fill="#FFFFFF" {o(2.4)}/>'
    for i in range(windows):
        wx = x + 10 + i * (w - 20) / windows
        out += f'<rect x="{wx}" y="{y + 10}" width="{(w - 20) / windows - 8}" height="16" rx="5" fill="#FFE9A8" {o(2.4)}/>'
    for wx in (x + 14, x + w - 14):
        out += f'<circle cx="{wx}" cy="{y + 48}" r="8" fill="#7C8FA8" {o(3)}/><circle cx="{wx}" cy="{y + 48}" r="2.6" fill="#FFF6E9"/>'
    return out


SPLASH = f'''<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#BFD9F2"/><stop offset=".55" stop-color="#F8D9E3"/><stop offset="1" stop-color="#FFE9D6"/></linearGradient></defs>
<rect width="360" height="640" fill="url(#sky)"/>
<circle cx="270" cy="250" r="38" fill="#FFF3C4" opacity=".9"/>
<g fill="#FFFFFF" opacity=".85"><ellipse cx="70" cy="120" rx="40" ry="12"/><ellipse cx="100" cy="112" rx="26" ry="12"/><ellipse cx="290" cy="160" rx="36" ry="10"/><ellipse cx="312" cy="152" rx="20" ry="10"/></g>
<path d="M-20,380 L60,250 L110,320 L170,220 L240,330 L300,260 L380,380 Z" fill="#B8CCE6" {o(3)}/>
<path d="M60,250 L78,280 L66,276 L56,290 L44,276 Z M170,220 L194,256 L178,252 L168,268 L156,250 L146,258 Z M300,260 L318,288 L306,284 L296,296 L284,282 Z" fill="#FFFFFF"/>
<path d="M-20,420 C60,380 140,400 200,385 C260,370 320,390 380,380 L380,640 L-20,640 Z" fill="#F4F9FD" {o(3)}/>
<g transform="translate(40 360) scale(.9)">{misc_body('spr_pine_a')}</g><g transform="translate(270 350) scale(1.1)">{misc_body('spr_pine_b')}</g>
<g transform="translate(300 400) scale(.7)">{misc_body('spr_pine_c')}</g><g transform="translate(-6 400) scale(.75)">{misc_body('spr_pine_b')}</g>
<rect x="-10" y="476" width="380" height="8" rx="4" fill="#B98A68" {o(2.6)}/>
{''.join(f'<rect x="{-6 + i * 26}" y="482" width="16" height="7" rx="3" fill="#C7976E" {o(2)}/>' for i in range(15))}
<g transform="translate(0 0)">
  <path d="M232,470 L232,420 C232,406 242,398 256,398 L300,398 C318,398 330,412 330,430 L330,470 Z" fill="#F4A7A0" {o(3)}/>
  <rect x="236" y="384" width="40" height="22" rx="6" fill="#E8897F" {o(3)}/>
  <rect x="300" y="380" width="14" height="22" rx="4" fill="#7C8FA8" {o(3)}/>
  <rect x="242" y="408" width="26" height="20" rx="5" fill="#FFE9A8" {o(2.4)}/>
  <circle cx="326" cy="446" r="7" fill="#FFF3C4" {o(2.4)}/>
  <path d="M330,462 L346,474 L330,474 Z" fill="#A9DDF2" {o(2.4)}/>
  <circle cx="250" cy="472" r="10" fill="#7C8FA8" {o(3)}/><circle cx="250" cy="472" r="3" fill="#FFF6E9"/>
  <circle cx="300" cy="472" r="10" fill="#7C8FA8" {o(3)}/><circle cx="300" cy="472" r="3" fill="#FFF6E9"/>
  <g fill="#FFFFFF" {o(2)}><circle cx="310" cy="366" r="8"/><circle cx="296" cy="346" r="11"/><circle cx="312" cy="322" r="14"/></g>
  {side_car(128, 424, 96, '#9FD3C7', '#F4A7A0')}
  {side_car(20, 424, 100, '#FFD6A5', '#9FC5EA')}
  <rect x="220" y="450" width="12" height="8" rx="3" fill="#A9B4BE" {o(2)}/><rect x="116" y="450" width="12" height="8" rx="3" fill="#A9B4BE" {o(2)}/>
</g>
<g fill="#FFFFFF" stroke="#DCE9F4" stroke-width="1"><circle cx="30" cy="200" r="3"/><circle cx="120" cy="60" r="2.4"/><circle cx="210" cy="140" r="3"/><circle cx="330" cy="90" r="2.4"/><circle cx="90" cy="300" r="2.6"/><circle cx="200" cy="320" r="2"/><circle cx="340" cy="220" r="3"/><circle cx="160" cy="180" r="2"/></g>
<g font-family="DejaVu Sans, sans-serif" font-weight="bold" text-anchor="middle">
  <text x="180" y="132" font-size="46" fill="#FFF6E9" stroke="{OL}" stroke-width="9" paint-order="stroke" stroke-linejoin="round">Frost</text>
  <text x="180" y="184" font-size="46" fill="#FFB86B" stroke="{OL}" stroke-width="9" paint-order="stroke" stroke-linejoin="round">Express</text>
  <text x="180" y="212" font-size="14" fill="{OL}">a cozy idle train adventure</text>
</g>'''

misc('splash_art', 360, 640, SPLASH, 3)

# ---------------- launcher icon (adaptive: background + foreground, 108 x 108 units) ----------------
ICON_BG = f'''<defs><linearGradient id="isky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#BFD9F2"/><stop offset=".6" stop-color="#F8D9E3"/><stop offset="1" stop-color="#FFE9D6"/></linearGradient></defs>
<rect width="108" height="108" fill="url(#isky)"/>
<circle cx="80" cy="34" r="11" fill="#FFF3C4"/>
<path d="M-6,74 L22,40 L38,58 L56,34 L78,60 L94,46 L114,74 Z" fill="#B8CCE6" stroke="{OL}" stroke-width="2"/>
<path d="M22,40 L28,48 L24,47 L20,52 L16,47 Z M56,34 L63,44 L58,43 L55,48 L50,43 L47,45 Z M94,46 L99,53 L96,52 L93,56 L89,52 Z" fill="#FFFFFF"/>
<path d="M-6,80 C20,72 50,76 70,73 C88,70 100,74 114,72 L114,114 L-6,114 Z" fill="#F4F9FD" stroke="{OL}" stroke-width="2"/>
<g fill="#FFFFFF"><circle cx="14" cy="22" r="1.4"/><circle cx="44" cy="16" r="1.2"/><circle cx="96" cy="18" r="1.2"/><circle cx="34" cy="64" r="1"/></g>'''
ICON_FG = f'''<g transform="translate(54 64) scale(.5) translate(-282 -432)">
  <rect x="196" y="476" width="166" height="8" rx="4" fill="#B98A68" {o(3)}/>
  <path d="M232,470 L232,420 C232,406 242,398 256,398 L300,398 C318,398 330,412 330,430 L330,470 Z" fill="#F4A7A0" {o(3.4)}/>
  <rect x="236" y="384" width="40" height="22" rx="6" fill="#E8897F" {o(3.4)}/>
  <rect x="300" y="380" width="14" height="22" rx="4" fill="#7C8FA8" {o(3.4)}/>
  <rect x="242" y="408" width="26" height="20" rx="5" fill="#FFE9A8" {o(2.8)}/>
  <circle cx="326" cy="446" r="7" fill="#FFF3C4" {o(2.8)}/>
  <path d="M330,462 L346,474 L330,474 Z" fill="#A9DDF2" {o(2.8)}/>
  <circle cx="250" cy="472" r="10" fill="#7C8FA8" {o(3.4)}/><circle cx="250" cy="472" r="3" fill="#FFF6E9"/>
  <circle cx="300" cy="472" r="10" fill="#7C8FA8" {o(3.4)}/><circle cx="300" cy="472" r="3" fill="#FFF6E9"/>
  <rect x="204" y="430" width="26" height="40" rx="8" fill="#9FD3C7" {o(3.4)}/>
  <g fill="#FFFFFF" {o(2.4)}><circle cx="310" cy="373" r="6"/><circle cx="300" cy="362" r="7.5"/><circle cx="313" cy="350" r="9"/></g>
</g>'''
misc('ic_launcher_bg', 108, 108, ICON_BG, 4)
misc('ic_launcher_fg', 108, 108, ICON_FG, 4)


# ---------------- tiles (textures) ----------------

TILES = {}


def tile(name, w, h, body, scale):
    TILES[name] = (w, h, body, scale)


# ---------------- survivors: rescued travellers who join the crew (same size as the worker) ----------------
def chibi(coat, coat_dark, hat_svg, hair_svg='', extra='', skin='#FFE2C6', beard=''):
    return f'''<ellipse cx="16" cy="38" rx="11" ry="3" fill="#000" opacity=".12"/>
{hair_svg}
<path d="M6,36 C5,26 9,21 16,21 C23,21 27,26 26,36 Z" fill="{coat}" {o(1.7)}/>
<path d="M16,22 L16,35" stroke="{coat_dark}" stroke-width="1.4"/><circle cx="16" cy="27" r=".9" fill="{OL}"/><circle cx="16" cy="31" r=".9" fill="{OL}"/>
<circle cx="16" cy="13" r="10.5" fill="{skin}" {o(1.7)}/>
{beard}
<ellipse cx="12.4" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/><ellipse cx="19.6" cy="16.4" rx="1.2" ry="1.6" fill="#4A3A34"/>
<circle cx="12.8" cy="15.8" r=".5" fill="#fff"/><circle cx="20" cy="15.8" r=".5" fill="#fff"/>
<ellipse cx="9.6" cy="19.2" rx="2" ry="1.2" fill="#F7A1A1" opacity=".85"/><ellipse cx="22.4" cy="19.2" rx="2" ry="1.2" fill="#F7A1A1" opacity=".85"/>
<path d="M14.4,19.6 C15.2,20.8 16.8,20.8 17.6,19.6" fill="none" stroke="{OL}" stroke-width="1" stroke-linecap="round"/>
{hat_svg}{extra}'''


SURV = [
    # Mira: pink earmuffs, lavender coat, brown pigtails
    chibi('#C9B6F2', '#A893E0',
          f'<path d="M6,10 C6,1 26,1 26,10 C22,6 10,6 6,10 Z" fill="#8B5E3C" {o(1.5)}/><circle cx="5.6" cy="13" r="3.4" fill="#F49AB2" {o(1.4)}/><circle cx="26.4" cy="13" r="3.4" fill="#F49AB2" {o(1.4)}/><path d="M5.6,10 C6,0 26,0 26.4,10" fill="none" stroke="{OL}" stroke-width="1.4"/>',
          f'<path d="M4,16 C0,20 1,27 4,29 C6,25 6,20 6,17 Z M28,16 C32,20 31,27 28,29 C26,25 26,20 26,17 Z" fill="#8B5E3C" {o(1.3)}/>'),
    # Grandpa: green flat cap, white beard, brown coat
    chibi('#B98A68', '#9A6C4C', f'<path d="M5,10 C5,2 27,2 27,10 Z" fill="#7FC29A" {o(1.5)}/><path d="M17,9.6 C22,9 29,10 30,11.6 C26,12.4 20,12 17,11.4 Z" fill="#6FB08B" {o(1.3)}/>',
          beard=f'<path d="M8,17 C9,26 23,26 24,17 C21,21 11,21 8,17 Z" fill="#FFFFFF" {o(1.2)}/><path d="M12.6,19.6 C14,18.6 18,18.6 19.4,19.6" fill="none" stroke="{OL}" stroke-width="1"/>'),
    # Pip: yellow raincoat with a hood
    chibi('#FFD66B', '#F2B84B', f'<path d="M4.6,15 C3,2 29,2 27.4,15 C25,9 7,9 4.6,15 Z" fill="#FFD66B" {o(1.6)}/>',
          f'<path d="M3.4,14 C2,26 6,26 7,20" fill="#FFD66B" {o(1.4)}/><path d="M28.6,14 C30,26 26,26 25,20" fill="#FFD66B" {o(1.4)}/>'),
    # Nadia: striped red beanie, teal coat, long dark hair
    chibi('#8FC2AE', '#6FAF96', f'<path d="M5.4,11 C5,1 27,1 26.6,11 Z" fill="#E46A6A" {o(1.5)}/><path d="M6,7.4 L26,7.4" stroke="#fff" stroke-width="1.6"/><circle cx="16" cy="1.6" r="2.4" fill="#fff" {o(1.2)}/>',
          f'<path d="M5,12 C2,22 3,32 7,34 L9,22 Z M27,12 C30,22 29,32 25,34 L23,22 Z" fill="#4A3A34" {o(1.2)}/>'),
]
for _i, _svg in enumerate(SURV):
    misc(f'npc_surv{_i + 1}', 32, 40, _svg, 6)
# the group, used as the upgrade icon
device('SURVIVORS', shadow(26) + f'<g transform="translate(2 18) scale(.9)">{SURV[1]}</g><g transform="translate(32 18) scale(.9)">{SURV[3]}</g><g transform="translate(16 22) scale(.9)">{SURV[0]}</g>',
       fx=[(SPARKLE, 32, 22, 22, 0xFFFFF3B0)])

# ---------------- floors: a different one in every car (tiles, 4 px per dp) ----------------
FLOORS = [
    # 0 engine car: light steel tread plate
    (32, 32, '<rect width="32" height="32" fill="#DCE3EA"/>' + ''.join(f'<path d="M{x},{y + 4} l4,-4" stroke="#B9C6D2" stroke-width="1.6" stroke-linecap="round"/>' for x in (2, 18) for y in (2, 18)) + ''.join(f'<path d="M{x},{y + 4} l4,-4" stroke="#B9C6D2" stroke-width="1.6" stroke-linecap="round"/>' for x in (10, 26) for y in (10, 26))),
    # 1 kitchen car: peach and cream checker
    (40, 40, '<rect width="40" height="40" fill="#FFF1DE"/><rect width="20" height="20" fill="#F8D7C0"/><rect x="20" y="20" width="20" height="20" fill="#F8D7C0"/>'),
    # 2 light wood planks
    (64, 60, '<rect width="64" height="60" fill="#F2D4A9"/><g fill="#ECCB9C"><rect y="12" width="64" height="12"/><rect y="36" width="64" height="12"/></g><g stroke="#D9B384" stroke-width="1.2"><line x1="0" y1="12" x2="64" y2="12"/><line x1="0" y1="24" x2="64" y2="24"/><line x1="0" y1="36" x2="64" y2="36"/><line x1="0" y1="48" x2="64" y2="48"/><line x1="0" y1="0" x2="64" y2="0"/><line x1="22" y1="0" x2="22" y2="12"/><line x1="50" y1="12" x2="50" y2="24"/><line x1="12" y1="24" x2="12" y2="36"/><line x1="40" y1="36" x2="40" y2="48"/><line x1="28" y1="48" x2="28" y2="60"/></g>'),
    # 3 walnut herringbone
    (32, 32, '<rect width="32" height="32" fill="#C99B74"/>' + ''.join(f'<path d="M{x},{y} l8,8 l-4,4 l-8,-8 Z" fill="{c}" stroke="#A97D58" stroke-width=".8"/>' for (x, y, c) in [(4, 0, "#D6AB84"), (20, 0, "#D3A57D"), (4, 16, "#D3A57D"), (20, 16, "#D6AB84")]) + ''.join(f'<path d="M{x},{y} l-8,8 l4,4 l8,-8 Z" fill="{c}" stroke="#A97D58" stroke-width=".8"/>' for (x, y, c) in [(12, 4, "#CDA07A"), (28, 4, "#D8AE88"), (12, 20, "#D8AE88"), (28, 20, "#CDA07A")])),
    # 4 mint hex tiles
    (36, 31.2, '<rect width="36" height="31.2" fill="#D6EFE4"/>' + ''.join(f'<path d="M{cx - 6},{cy} L{cx - 3},{cy - 5.2} L{cx + 3},{cy - 5.2} L{cx + 6},{cy} L{cx + 3},{cy + 5.2} L{cx - 3},{cy + 5.2} Z" fill="#BFE0D2" stroke="#A9D3C1" stroke-width="1"/>' for cx, cy in [(9, 5.2), (27, 5.2), (0, 15.6), (18, 15.6), (36, 15.6), (9, 26), (27, 26)])),
    # 5 pink carpet with little flowers
    (32, 32, '<rect width="32" height="32" fill="#F9D3DD"/>' + ''.join(f'<g transform="translate({x} {y})"><circle r="1.6" fill="#fff"/><circle cx="2.6" r="1.4" fill="#F49AB2"/><circle cx="-2.6" r="1.4" fill="#F49AB2"/><circle cy="2.6" r="1.4" fill="#F49AB2"/><circle cy="-2.6" r="1.4" fill="#F49AB2"/></g>' for x, y in [(8, 8), (24, 24)])),
    # 6 blue and white tiles
    (32, 32, '<rect width="32" height="32" fill="#F4FAFF"/><g fill="none" stroke="#9FC5EA" stroke-width="1.2"><rect x=".6" y=".6" width="14.8" height="14.8" rx="2"/><rect x="16.6" y=".6" width="14.8" height="14.8" rx="2"/><rect x=".6" y="16.6" width="14.8" height="14.8" rx="2"/><rect x="16.6" y="16.6" width="14.8" height="14.8" rx="2"/></g><g fill="#B8D8F2"><circle cx="8" cy="8" r="2.4"/><circle cx="24" cy="24" r="2.4"/><path d="M24,4 l4,4 l-4,4 l-4,-4 Z"/><path d="M8,20 l4,4 l-4,4 l-4,-4 Z"/></g>'),
    # 7 lavender striped rug
    (32, 24, '<rect width="32" height="24" fill="#E6DBFB"/><rect y="4" width="32" height="4" fill="#D5C4F6"/><rect y="16" width="32" height="4" fill="#D5C4F6"/><g fill="#fff" opacity=".7"><circle cx="8" cy="12" r="1"/><circle cx="24" cy="12" r="1"/></g>'),
    # 8 honey parquet squares
    (32, 32, '<rect width="32" height="32" fill="#EFC98F"/>' + ''.join(f'<line x1="{x}" y1="{y0}" x2="{x}" y2="{y0 + 16}" stroke="#D9AE70" stroke-width="1"/>' for y0 in (0,) for x in (4, 8, 12)) + ''.join(f'<line x1="{x0}" y1="{y}" x2="{x0 + 16}" y2="{y}" stroke="#D9AE70" stroke-width="1"/>' for x0 in (16,) for y in (4, 8, 12)) + ''.join(f'<line x1="{x0}" y1="{y}" x2="{x0 + 16}" y2="{y}" stroke="#D9AE70" stroke-width="1"/>' for x0 in (0,) for y in (20, 24, 28)) + ''.join(f'<line x1="{x}" y1="16" x2="{x}" y2="32" stroke="#D9AE70" stroke-width="1"/>' for x in (20, 24, 28)) + '<rect width="32" height="32" fill="none" stroke="#C99A5C" stroke-width="1"/><line x1="16" y1="0" x2="16" y2="32" stroke="#C99A5C"/><line x1="0" y1="16" x2="32" y2="16" stroke="#C99A5C"/>'),
    # 9 stone pavers
    (40, 40, '<rect width="40" height="40" fill="#D9D6CF"/><g fill="#E7E4DD" stroke="#C4BFB5" stroke-width="1.2"><rect x="1" y="1" width="22" height="16" rx="4"/><rect x="25" y="1" width="14" height="16" rx="4"/><rect x="1" y="19" width="12" height="20" rx="4"/><rect x="15" y="19" width="24" height="20" rx="4"/></g>'),
    # 10 green mats
    (32, 32, '<rect width="32" height="32" fill="#D8ECCB"/><rect x="1" y="1" width="30" height="14" rx="2" fill="#CDE6BE" stroke="#A9CF95" stroke-width="1"/><rect x="1" y="17" width="14" height="14" rx="2" fill="#CDE6BE" stroke="#A9CF95" stroke-width="1"/><rect x="17" y="17" width="14" height="14" rx="2" fill="#CDE6BE" stroke="#A9CF95" stroke-width="1"/>'),
]
for _i, (_w, _h, _body) in enumerate(FLOORS):
    tile(f'floor_{_i}', _w, _h, _body, 4)


tile('tex_snow', 128, 128, '''<rect width="128" height="128" fill="#EEF5FB"/>
<g fill="#E1ECF6"><ellipse cx="20" cy="30" rx="26" ry="7"/><ellipse cx="90" cy="70" rx="30" ry="8"/><ellipse cx="40" cy="110" rx="22" ry="6"/><ellipse cx="148" cy="30" rx="26" ry="7"/><ellipse cx="-38" cy="70" rx="30" ry="8"/></g>
<g fill="#FFFFFF"><circle cx="10" cy="10" r="1.4"/><circle cx="70" cy="22" r="1.1"/><circle cx="110" cy="100" r="1.4"/><circle cx="60" cy="80" r="1"/><circle cx="30" cy="66" r="1.2"/></g>''', 4)
tile('tex_track', 66, 28, f'''<rect width="66" height="28" fill="#DDD3C8"/>
<g fill="#CFC3B6"><circle cx="8" cy="6" r="2"/><circle cx="58" cy="20" r="2.4"/><circle cx="30" cy="22" r="1.6"/><circle cx="44" cy="5" r="1.8"/></g>
<rect x="5" y="2" width="56" height="7" rx="3" fill="#C7976E" stroke="{OL}" stroke-width="1.3"/>
<rect x="5" y="16" width="56" height="7" rx="3" fill="#C7976E" stroke="{OL}" stroke-width="1.3"/>
<rect x="17" y="-2" width="5" height="32" fill="#B9C6D2" stroke="{OL}" stroke-width="1.3"/>
<rect x="45" y="-2" width="5" height="32" fill="#B9C6D2" stroke="{OL}" stroke-width="1.3"/>
<rect x="18.4" y="-2" width="1.6" height="32" fill="#E8EEF4"/><rect x="46.4" y="-2" width="1.6" height="32" fill="#E8EEF4"/>''', 4)
tile('tex_wood', 64, 60, '''<rect width="64" height="60" fill="#F2D4A9"/>
<g fill="#ECCB9C"><rect x="0" y="12" width="64" height="12"/><rect x="0" y="36" width="64" height="12"/></g>
<g stroke="#D9B384" stroke-width="1.2"><line x1="0" y1="12" x2="64" y2="12"/><line x1="0" y1="24" x2="64" y2="24"/><line x1="0" y1="36" x2="64" y2="36"/><line x1="0" y1="48" x2="64" y2="48"/><line x1="0" y1="60" x2="64" y2="60"/><line x1="0" y1="0" x2="64" y2="0"/>
<line x1="22" y1="0" x2="22" y2="12"/><line x1="50" y1="12" x2="50" y2="24"/><line x1="12" y1="24" x2="12" y2="36"/><line x1="40" y1="36" x2="40" y2="48"/><line x1="28" y1="48" x2="28" y2="60"/></g>''', 4)
tile('tex_metal', 40, 40, '''<rect width="40" height="40" fill="#F7E6CC"/><rect width="20" height="20" rx="2" fill="#EED3AE"/><rect x="20" y="20" width="20" height="20" rx="2" fill="#EED3AE"/>''', 4)
tile('tex_concrete', 32, 32, '''<rect width="32" height="32" fill="#E4E8EC"/><rect x="0" y="15.4" width="32" height="1.2" fill="#CDD3D9"/><circle cx="8" cy="6" r=".8" fill="#CDD3D9"/><circle cx="24" cy="26" r=".8" fill="#CDD3D9"/>''', 4)


# ---------------- rendering ----------------
def svg(w, h, body, px_w, px_h):
    return (f'<!doctype html><html><head><style>html,body{{margin:0;background:transparent}}svg{{display:block}}</style></head><body>'
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{px_w}" height="{px_h}" viewBox="0 0 {w} {h}">{DEFS}{body}</svg></body></html>')


async def render_all(only):
    from playwright.async_api import async_playwright
    jobs = []
    for t, body in DEV.items():
        jobs.append(('dev_' + t.lower(), 64, 64, body, 4))
    for t, r in ROT.items():
        jobs.append(('rot_' + t.lower(), 64, 64, r[0], 4))
    for name, (w, h, body) in FURN.items():
        jobs.append((name, w, h, body, 4))
    for name, (w, h, body, sc) in MISC.items():
        jobs.append((name, w, h, body, sc))
    for name, (w, h, body, sc) in TILES.items():
        jobs.append((name, w, h, body, sc))
    jobs = [j for j in jobs if only in j[0]]
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page(device_scale_factor=1)
        for name, w, h, body, sc in jobs:
            pw, ph = int(w * sc), int(h * sc)
            await pg.set_viewport_size({'width': pw, 'height': ph})
            await pg.set_content(svg(w, h, body, pw, ph))
            png = await pg.screenshot(omit_background=True, clip={'x': 0, 'y': 0, 'width': pw, 'height': ph})
            img = Image.open(io.BytesIO(png)).convert('RGBA')
            opaque = (name.startswith('tex_') and name not in ('tex_track',)) or name.startswith('floor_')
            if opaque:
                img = img.convert('RGB')
            img.save(os.path.join(OUT, name + '.webp'), 'WEBP', quality=92, method=6)
            print('rendered', name, img.size)
        await b.close()


def write_kotlin():
    lines = ['package com.chetna.frostexpress', '',
             '// Generated by store_assets/cozy_art.py. Do not edit by hand: change the art script and re-run it.', '',
             '/** A live effect on a device sprite. Positions are in sprite units (0..64). */',
             'internal class DevFx(val kind: Int, val x: Float, val y: Float, val a: Float, val color: Long)', '',
             'internal object FxKind { const val STEAM = 0; const val GLOW = 1; const val RIPPLE = 2; const val SPARKS = 3; const val RAINBOW = 4; const val NEEDLE = 5; const val HANDS = 6; const val BLINK = 7; const val SPARKLE = 8; const val ZZZ = 9; const val BUBBLES = 10; const val WIND = 11; const val DRIP = 12 }', '',
             'internal val DEVICE_FX: Map<UpgradeType, List<DevFx>> = mapOf(']
    for t, fx in FX.items():
        items = ', '.join(f'DevFx({k}, {x}f, {y}f, {a}f, 0x{c:08X})' for k, x, y, a, c in fx)
        lines.append(f'    UpgradeType.{t} to listOf({items}),')
    lines.append(')')
    lines.append('')
    lines.append('/** Spinning parts: pivot x, pivot y (sprite units), degrees per second, swing (+- degrees, 0 = full turns). */')
    lines.append('internal val DEVICE_ROTORS: Map<UpgradeType, FloatArray> = mapOf(')
    for t, (_, px, py, sp, sw) in ROT.items():
        lines.append(f'    UpgradeType.{t} to floatArrayOf({px}f, {py}f, {sp}f, {sw}f),')
    lines.append(')')
    open(KT, 'w').write('\n'.join(lines) + '\n')
    print('wrote', KT)


if __name__ == '__main__':
    only = sys.argv[1] if len(sys.argv) > 1 else ''
    os.makedirs(OUT, exist_ok=True)
    asyncio.run(render_all(only))
    write_kotlin()
