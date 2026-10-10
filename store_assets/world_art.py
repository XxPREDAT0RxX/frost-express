"""The frozen world outside the train: the ruins of a world that froze over, the bigger stations,
a different nose and trim for every train tier. Same cozy style as cozy_art.py (and uses its helpers).

    python3 store_assets/world_art.py           # everything in this file
    python3 store_assets/world_art.py spr_      # only names containing this text
    python3 store_assets/world_art.py --sheet   # also writes a preview contact sheet
"""
import asyncio, io, math, os, sys
from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import cozy_art as C
from cozy_art import OL, o

OUT = C.OUT
ART = {}


def art(name, w, h, body, scale=4):
    ART[name] = (w, h, body, scale)


SNOW_SH = '#C9D6E2'
WOOD, WOOD_D = '#9C8A80', '#7E6E66'
ICE, ICE_D = '#D8F3FF', '#9FD3EE'
RUST, RUST_D = '#C49480', '#A27462'
CONC, CONC_D = '#C3C9D1', '#A3ABB5'
FROST = '#DCEEF7'


def stroke_paths(paths, col, extra=2.4):
    """Branches: a brown outline pass, then the wood colour on top."""
    a = ''.join(f'<path d="{d}" fill="none" stroke="{OL}" stroke-width="{w + extra}" stroke-linecap="round" stroke-linejoin="round"/>' for d, w in paths)
    b = ''.join(f'<path d="{d}" fill="none" stroke="{col}" stroke-width="{w}" stroke-linecap="round" stroke-linejoin="round"/>' for d, w in paths)
    return a + b


def icicles(xs, y, l=4.0):
    return ''.join(f'<path d="M{x - 1.2},{y} L{x + 1.2},{y} L{x},{y + l + (i % 2) * 2} Z" fill="{ICE}" stroke="{ICE_D}" stroke-width=".6"/>' for i, x in enumerate(xs))


def snowcap(d):
    return f'<path d="{d}" fill="#FFFFFF" {o(1.4)}/>'


# ---------------- frosty versions of the old trees ----------------
for nm in ('spr_pine_a', 'spr_pine_b', 'spr_pine_c', 'spr_shrub'):
    w, h, body, sc = C.MISC[nm]
    body = (body.replace('#8FCBA6', '#A6BCC8').replace('#7FC29A', '#94AEBD').replace('#9AD0AF', '#AFC4CF')
                .replace('#6FB08B', '#8AA3B2').replace('#F28C8C', ICE).replace('#C9DCEC', SNOW_SH))
    art(nm.replace('spr_', 'spr_frost_'), w, h, body, sc)

# ---------------- dead trees ----------------
art('spr_dead_a', 64, 64, f'''<ellipse cx="32" cy="55" rx="18" ry="4.5" fill="{SNOW_SH}"/>
{stroke_paths([("M32,55 C31,44 33,36 31,26", 5), ("M31,34 C26,30 22,28 16,22", 2.6), ("M32,30 C37,25 42,23 48,16", 2.6),
                ("M31,26 C30,19 32,14 30,8", 2.2), ("M20,25 C18,20 19,17 17,13", 1.6), ("M44,19 C47,15 50,13 53,12", 1.6),
                ("M31,42 C36,40 40,38 44,34", 2)], WOOD)}
{snowcap("M14,22 C16,19 20,21 21,24 C18,24 16,24 14,22 Z")}{snowcap("M44,18 C47,14 51,15 50,18 C48,19 46,19 44,18 Z")}
{snowcap("M28,9 C30,5 33,6 32,10 C31,11 29,11 28,9 Z")}{snowcap("M40,36 C43,33 46,34 45,36 C43,37 41,37 40,36 Z")}
{icicles([17, 20, 47, 50], 24, 3)}
<path d="M22,55 C26,51 38,51 42,55 Z" fill="#FFFFFF" {o(1.4)}/>''')
art('spr_dead_b', 64, 64, f'''<ellipse cx="32" cy="55" rx="24" ry="5" fill="{SNOW_SH}"/>
<path d="M18,52 L46,40 L49,46 L21,57 Z" fill="{WOOD}" {o(2)}/><path d="M20,52 L46,41" stroke="{WOOD_D}" stroke-width="1.2"/>
<ellipse cx="47.5" cy="43" rx="2.6" ry="3.4" fill="#D8C2A8" {o(1.4)} transform="rotate(-22 47.5 43)"/>
<path d="M14,54 L14,34 L17,30 L19,35 L21,29 L23,34 L24,54 Z" fill="{WOOD}" {o(2)}/>
<path d="M17,36 L17,52 M21,37 L21,52" stroke="{WOOD_D}" stroke-width="1"/>
{snowcap("M13,35 C15,31 21,30 25,34 C21,36 16,36 13,35 Z")}{snowcap("M24,48 C30,43 40,40 46,40 C40,44 30,48 24,48 Z")}
<path d="M8,56 C14,51 28,52 32,57 Z" fill="#FFFFFF" {o(1.4)}/>''')

# ---------------- ruins of the old world ----------------
art('spr_ruin_a', 64, 64, f'''<ellipse cx="32" cy="56" rx="29" ry="5" fill="{SNOW_SH}"/>
<rect x="42" y="14" width="7" height="16" fill="#A97C6A" {o(1.8)}/><path d="M41,14 L50,14 L49,11 L45,13 L42,10 Z" fill="#8E6656" {o(1.4)}/>
<path d="M4,50 L32,18 L60,50 Z" fill="#B98375" {o(2.2)}/>
{''.join(f'<path d="M{32 - k * 4.6},{18 + k * 5.2} L{32 + k * 4.6},{18 + k * 5.2}" stroke="#9C6A5E" stroke-width="1.1"/>' for k in range(1, 7))}
<path d="M26,40 L38,40 L38,50 L26,50 Z" fill="{FROST}" {o(1.6)}/><path d="M32,40 L32,50 M26,45 L38,45" stroke="{OL}" stroke-width="1.2"/>
<path d="M28,42 L31,44" stroke="#fff" stroke-width="1.2" stroke-linecap="round"/>
{snowcap("M18,34 C24,26 30,19 32,18 C35,20 41,26 46,33 C40,31 36,29 32,28 C27,30 22,32 18,34 Z")}
<path d="M0,58 C2,48 14,45 22,50 C30,46 40,46 46,50 C54,45 63,48 64,58 Z" fill="#FFFFFF" {o(1.6)}/>
{icicles([10, 14, 50, 54], 47, 3)}''')
art('spr_ruin_b', 64, 64, f'''<ellipse cx="32" cy="56" rx="27" ry="5" fill="{SNOW_SH}"/>
<path d="M10,54 L10,20 L16,16 L20,22 L26,12 L32,18 L38,10 L44,20 L50,15 L54,22 L54,54 Z" fill="{CONC}" {o(2.2)}/>
<path d="M54,22 L58,26 L58,54 L54,54 Z" fill="{CONC_D}" {o(1.8)}/>
{''.join(f'<rect x="{x}" y="{y}" width="8" height="9" rx="1" fill="#6F7E8C" {o(1.4)}/><path d="M{x + 1.5},{y + 2} L{x + 4},{y + 4.5}" stroke="{ICE}" stroke-width="1.2" stroke-linecap="round"/>' for x in (15, 28, 41) for y in (26, 40))}
<path d="M20,22 L18,12 M38,10 L39,4 M44,20 L47,11" stroke="#8C5E4C" stroke-width="1.6" stroke-linecap="round"/>
{snowcap("M9,22 C12,18 15,16 16,16 L20,22 C24,16 26,12 26,12 L32,18 C35,13 38,10 38,10 L44,20 C47,17 50,15 50,15 L54,22 C50,25 14,25 9,22 Z")}
<path d="M2,58 C4,49 16,47 24,51 C32,47 44,47 50,51 C56,47 63,50 62,58 Z" fill="#FFFFFF" {o(1.6)}/>''')
art('spr_car', 64, 64, f'''<ellipse cx="32" cy="54" rx="28" ry="5" fill="{SNOW_SH}"/>
<path d="M6,48 L6,38 C6,35 8,34 11,33 L18,32 L24,23 C25,22 26,21 28,21 L42,21 C44,21 45,22 46,23 L52,32 L56,33 C58,34 59,36 59,38 L59,48 Z" fill="#9DB0C2" {o(2.2)}/>
<path d="M22,32 L27,24 L34,24 L34,32 Z M37,32 L37,24 L43,24 L48,32 Z" fill="{FROST}" {o(1.4)}/>
<path d="M28,26 L31,28 M39,26 L41,28" stroke="#fff" stroke-width="1.2" stroke-linecap="round"/>
<rect x="6" y="40" width="53" height="3" fill="{RUST}" opacity=".7"/>
<circle cx="18" cy="48" r="6" fill="#4A4550" {o(1.8)}/><circle cx="18" cy="48" r="2.4" fill="#A3ABB5"/>
{snowcap("M23,22 C26,16 44,15 47,22 C40,22 30,22 23,22 Z")}{snowcap("M5,37 C7,32 14,31 18,32 C13,34 9,35 5,37 Z")}
{icicles([10, 14, 50, 54], 43, 3)}
<path d="M30,58 C34,47 48,43 62,46 L64,58 Z" fill="#FFFFFF" {o(1.6)}/>''')
art('spr_bus', 64, 64, f'''<ellipse cx="32" cy="55" rx="30" ry="5" fill="{SNOW_SH}"/>
<g transform="rotate(-6 32 38)">
<rect x="4" y="22" width="56" height="26" rx="6" fill="#E3C584" {o(2.2)}/>
<rect x="4" y="38" width="56" height="4" fill="#C9A35F"/>
{''.join(f'<rect x="{x}" y="26" width="8" height="8" rx="1.5" fill="{FROST}" {o(1.3)}/>' for x in (9, 20, 31, 42))}
<rect x="52" y="26" width="5" height="14" rx="1.5" fill="#6F7E8C" {o(1.3)}/>
{snowcap("M5,23 C10,16 54,15 59,23 C44,25 20,25 5,23 Z")}
{icicles([12, 16, 28, 32, 44, 48], 48, 3)}</g>
<path d="M0,58 C2,44 18,40 30,46 C40,42 56,44 64,52 L64,58 Z" fill="#FFFFFF" {o(1.6)}/>''')
art('spr_pole', 64, 64, f'''<ellipse cx="30" cy="56" rx="16" ry="4" fill="{SNOW_SH}"/>
<g transform="rotate(10 30 56)">
<rect x="28" y="6" width="5" height="50" rx="1.5" fill="{WOOD}" {o(1.8)}/>
<rect x="17" y="12" width="27" height="4" rx="1.2" fill="{WOOD}" {o(1.6)}/>
<circle cx="19" cy="11" r="1.6" fill="#E9EEF3" {o(1)}/><circle cx="42" cy="11" r="1.6" fill="#E9EEF3" {o(1)}/>
{snowcap("M16,12 C20,9 40,9 45,12 C38,13 22,13 16,12 Z")}{snowcap("M27,6 C29,3 32,3 34,6 C32,7 29,7 27,6 Z")}</g>
<path d="M20,13 C12,24 6,30 0,32 M44,17 C52,26 58,30 64,30" fill="none" stroke="{OL}" stroke-width="1.2"/>
{icicles([8, 12, 52, 57], 27, 3)}
<path d="M18,58 C22,51 36,50 42,58 Z" fill="#FFFFFF" {o(1.4)}/>''')
art('spr_ice', 64, 64, f'''<ellipse cx="32" cy="55" rx="24" ry="5" fill="{SNOW_SH}"/>
<path d="M14,55 L19,30 L24,55 Z" fill="{ICE}" {o(1.8)}/><path d="M19,30 L21,55" stroke="{ICE_D}" stroke-width="1.4"/>
<path d="M24,56 L32,8 L40,56 Z" fill="{ICE}" {o(2)}/><path d="M32,8 L35,56" stroke="{ICE_D}" stroke-width="2"/>
<path d="M38,56 L46,22 L52,56 Z" fill="{ICE}" {o(1.8)}/><path d="M46,22 L48,56" stroke="{ICE_D}" stroke-width="1.4"/>
<path d="M29,22 L31,30 M43,32 L44,38" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>
<path d="M10,58 C16,51 48,50 56,58 Z" fill="#FFFFFF" {o(1.4)}/>''')
art('spr_lamp', 64, 64, f'''<ellipse cx="30" cy="56" rx="12" ry="3.5" fill="{SNOW_SH}"/>
<path d="M30,56 L30,22 C30,12 36,8 44,10" fill="none" stroke="{OL}" stroke-width="5.2" stroke-linecap="round"/>
<path d="M30,56 L30,22 C30,12 36,8 44,10" fill="none" stroke="#7C8794" stroke-width="2.8" stroke-linecap="round"/>
<path d="M40,9 L50,9 L48,16 L42,16 Z" fill="#5A6470" {o(1.6)}/><rect x="42.5" y="15.5" width="5" height="2.4" rx="1" fill="#A9B8C6" {o(1)}/>
{snowcap("M39,9 C42,5 48,5 51,9 C47,10 43,10 39,9 Z")}
{icicles([43, 46, 49], 18, 3)}
<path d="M20,58 C23,51 36,51 40,58 Z" fill="#FFFFFF" {o(1.4)}/>''')
art('spr_sign', 64, 64, f'''<ellipse cx="32" cy="56" rx="14" ry="3.5" fill="{SNOW_SH}"/>
<g transform="rotate(-14 32 56)">
<rect x="30" y="24" width="4" height="32" rx="1" fill="#8F99A4" {o(1.6)}/>
<path d="M32,6 L46,20 L32,34 L18,20 Z" fill="#E3C584" {o(2)}/><path d="M32,10 L42,20 L32,30 L22,20 Z" fill="none" stroke="#B9935A" stroke-width="1.2"/>
<path d="M30,15 L34,15 L33,23 L31,23 Z M31,25 L33,25 L33,27 L31,27 Z" fill="{OL}"/>
{snowcap("M24,14 C27,9 31,6 32,6 C33,6 37,9 40,14 C35,12 29,12 24,14 Z")}</g>
<path d="M18,58 C22,51 40,50 46,58 Z" fill="#FFFFFF" {o(1.4)}/>''')


# ---------------- the bigger station (seen from above) ----------------
# building roof (seen from above): 40 x 112 units, ridge along its length, snow, chimney
art('station_bldg', 44, 116, f'''<rect x="5" y="6" width="36" height="106" rx="5" fill="#000" opacity=".12"/>
<rect x="2" y="3" width="36" height="106" rx="5" fill="#C98F72" {o(2.2)}/>
<rect x="20" y="3" width="18" height="106" rx="5" fill="#B27A60"/>
<rect x="2" y="3" width="36" height="106" rx="5" fill="none" {o(2.2)}/>
<line x1="20" y1="4" x2="20" y2="108" stroke="{OL}" stroke-width="1.8"/>
{''.join(f'<line x1="3" y1="{10 + k * 7}" x2="37" y2="{10 + k * 7}" stroke="#A86F57" stroke-width="1"/>' for k in range(14))}
<path d="M3,8 C3,4 37,4 37,8 L37,30 C31,34 24,28 20,32 C14,36 8,30 3,34 Z" fill="#FFFFFF" {o(1.6)}/>
<path d="M3,80 C9,76 14,82 20,78 C26,74 32,80 37,77 L37,104 C37,108 3,108 3,104 Z" fill="#FFFFFF" {o(1.6)}/>
<rect x="24" y="48" width="9" height="9" rx="1.5" fill="#8C6A5A" {o(1.6)}/><rect x="25.5" y="49.5" width="6" height="6" rx="1" fill="#4A4550"/>
<path d="M4,56 C8,54 12,58 16,56" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round"/>''', 6)
# a lamp post seen from above with its glass head
art('station_lamp', 16, 16, f'''<circle cx="8.6" cy="8.8" r="5.6" fill="#000" opacity=".15"/>
<circle cx="8" cy="8" r="5.4" fill="#5A6470" {o(1.4)}/><circle cx="8" cy="8" r="3.4" fill="#FFF3C4" {o(1)}/><circle cx="7" cy="7" r="1.1" fill="#fff"/>''', 8)
# a wooden bench seen from above
art('station_bench', 14, 30, f'''<rect x="3" y="3" width="10" height="26" rx="2" fill="#000" opacity=".12"/>
<rect x="1.5" y="1.5" width="10" height="26" rx="2" fill="#D9A576" {o(1.4)}/>
<line x1="4.8" y1="2" x2="4.8" y2="27" stroke="#B9876A" stroke-width="1"/><line x1="8.2" y1="2" x2="8.2" y2="27" stroke="#B9876A" stroke-width="1"/>
<path d="M2,8 C4,6 8,7 11,6 L11,11 C8,12 4,10 2,12 Z" fill="#fff" opacity=".9"/>''', 8)


# ---------------- a different nose and wall trim for every train tier ----------------
NW = 190
X0 = 6           # left edge of the train in sprite units
CX = X0 + NW / 2


def nose(i, fill_d, line_extra):
    """fill_d: the nose outline (tinted with the train colour in code); line_extra: fixed-colour details."""
    art(f'nose{i}_fill', NW + 12, 64, f'<ellipse cx="{CX}" cy="58" rx="{NW / 2}" ry="5" fill="#000" opacity=".10"/><path d="{fill_d}" fill="#FFFFFF"/>', 4)
    art(f'nose{i}_line', NW + 12, 64, f'<path d="{fill_d}" fill="none" stroke="{OL}" stroke-width="2.4" stroke-linejoin="round"/>{line_extra}', 4)


def lamp(x, y, r=5.5):
    return f'<circle cx="{x}" cy="{y}" r="{r}" fill="#FFF3C4" stroke="{OL}" stroke-width="2"/><circle cx="{x - 1.5}" cy="{y - 1.5}" r="{r * 0.3}" fill="#fff"/>'


# 1 Frost Runner: a boxy diesel with a big windshield and hazard chevrons
d1 = f'M{X0 + 12},56 L{X0 + 12},26 C{X0 + 12},20 {X0 + 20},16 {X0 + 28},16 L{X0 + NW - 28},16 C{X0 + NW - 20},16 {X0 + NW - 12},20 {X0 + NW - 12},26 L{X0 + NW - 12},56 Z'
chev = ''.join(f'<path d="M{X0 + 16 + k * 14},56 L{X0 + 24 + k * 14},48 L{X0 + 30 + k * 14},48 L{X0 + 22 + k * 14},56 Z" fill="#3C3A40"/>' for k in range(12))
nose(1, d1, f'''<rect x="{X0 + 14}" y="46" width="{NW - 28}" height="10" fill="#FFD43B"/>{chev}<rect x="{X0 + 14}" y="46" width="{NW - 28}" height="10" fill="none" stroke="{OL}" stroke-width="1.6"/>
<rect x="{X0 + 40}" y="22" width="{NW - 80}" height="16" rx="5" fill="#BEE6F7" stroke="{OL}" stroke-width="2"/><line x1="{CX}" y1="22" x2="{CX}" y2="38" stroke="{OL}" stroke-width="2"/>
<path d="M{X0 + 46},26 L{X0 + 56},26" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
<rect x="{X0 + 18}" y="24" width="14" height="9" rx="2.5" fill="#FFF3C4" stroke="{OL}" stroke-width="1.8"/><rect x="{X0 + NW - 32}" y="24" width="14" height="9" rx="2.5" fill="#FFF3C4" stroke="{OL}" stroke-width="1.8"/>''')

# 2 Ice Breaker: a steel wedge prow with cutter teeth and rivets
d2 = f'M{X0 + 10},56 L{X0 + 44},18 L{CX},4 L{X0 + NW - 44},18 L{X0 + NW - 10},56 Z'
teeth = ''.join(f'<path d="M{x},{y} l-3,-6 l6,0 Z" fill="#DDE6EE" stroke="{OL}" stroke-width="1.2" transform="rotate({a} {x} {y})"/>'
                for x, y, a in [(X0 + 18 + k * 8.5, 48 - k * 9.5, -48) for k in range(4)] + [(X0 + NW - 18 - k * 8.5, 48 - k * 9.5, 48) for k in range(4)])
rivets = ''.join(f'<circle cx="{X0 + 30 + k * 13}" cy="50" r="1.4" fill="#fff" opacity=".7"/>' for k in range(11))
nose(2, d2, f'''<path d="M{X0 + 30},50 L{CX},14 L{X0 + NW - 30},50" fill="none" stroke="#DDE6EE" stroke-width="5" stroke-linejoin="round"/>
<path d="M{X0 + 30},50 L{CX},14 L{X0 + NW - 30},50" fill="none" stroke="{OL}" stroke-width="1.4" stroke-linejoin="round"/>{teeth}{rivets}
<line x1="{CX}" y1="6" x2="{CX}" y2="54" stroke="{OL}" stroke-width="1.6"/>{lamp(X0 + 62, 36)}{lamp(X0 + NW - 62, 36)}''')

# 3 Aurora Express: a smooth dome with a wide curved windshield and a glowing aurora stripe
d3 = f'M{X0 + 10},56 C{X0 + 10},26 {X0 + 50},6 {CX},6 C{X0 + NW - 50},6 {X0 + NW - 10},26 {X0 + NW - 10},56 Z'
nose(3, d3, f'''<defs><linearGradient id="aur" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#7CF2C8"/><stop offset=".5" stop-color="#B9A2EE"/><stop offset="1" stop-color="#7CF2C8"/></linearGradient></defs>
<path d="M{X0 + 16},52 C{X0 + 20},34 {X0 + 56},20 {CX},20 C{X0 + NW - 56},20 {X0 + NW - 20},34 {X0 + NW - 16},52" fill="none" stroke="url(#aur)" stroke-width="5" stroke-linecap="round"/>
<path d="M{X0 + 52},30 C{X0 + 66},14 {X0 + NW - 66},14 {X0 + NW - 52},30 C{X0 + NW - 70},26 {X0 + 70},26 {X0 + 52},30 Z" fill="#BEE6F7" stroke="{OL}" stroke-width="2"/>
<path d="M{X0 + 70},22 L{X0 + 84},19" stroke="#fff" stroke-width="2" stroke-linecap="round"/>{lamp(X0 + 36, 44, 5)}{lamp(X0 + NW - 36, 44, 5)}''')

# 4 Cryo Bullet: a long pointed bullet nose with racing stripes
d4 = f'M{X0 + 14},56 C{X0 + 18},30 {X0 + 70},4 {CX},1 C{X0 + NW - 70},4 {X0 + NW - 18},30 {X0 + NW - 14},56 Z'
nose(4, d4, f'''<path d="M{CX - 9},56 L{CX - 9},18 C{CX - 9},12 {CX - 4},6 {CX},4 M{CX + 9},56 L{CX + 9},18 C{CX + 9},12 {CX + 4},6 {CX},4" fill="none" stroke="#FFFFFF" stroke-width="4" stroke-linecap="round"/>
<path d="M{CX - 22},34 C{CX - 18},20 {CX + 18},20 {CX + 22},34 C{CX + 10},30 {CX - 10},30 {CX - 22},34 Z" fill="#BEE6F7" stroke="{OL}" stroke-width="2"/>
<path d="M{CX - 14},28 L{CX - 6},25" stroke="#fff" stroke-width="2" stroke-linecap="round"/>{lamp(X0 + 44, 46, 4.5)}{lamp(X0 + NW - 44, 46, 4.5)}''')

# 5 Eternal Engine: an armoured fortress front with gold trim and spikes
d5 = f'M{X0 + 8},56 L{X0 + 8},34 L{X0 + 26},22 L{X0 + 60},22 L{X0 + 72},10 L{X0 + NW - 72},10 L{X0 + NW - 60},22 L{X0 + NW - 26},22 L{X0 + NW - 8},34 L{X0 + NW - 8},56 Z'
spikes = ''.join(f'<path d="M{X0 + 20 + k * 21},56 L{X0 + 27 + k * 21},56 L{X0 + 23.5 + k * 21},62 Z" fill="#FFD166" stroke="{OL}" stroke-width="1.2"/>' for k in range(8))
nose(5, d5, f'''<path d="M{X0 + 8},34 L{X0 + 26},22 L{X0 + 60},22 L{X0 + 72},10 L{X0 + NW - 72},10 L{X0 + NW - 60},22 L{X0 + NW - 26},22 L{X0 + NW - 8},34" fill="none" stroke="#FFD166" stroke-width="3.6" stroke-linejoin="round"/>
<path d="M{X0 + 8},34 L{X0 + 26},22 L{X0 + 60},22 L{X0 + 72},10 L{X0 + NW - 72},10 L{X0 + NW - 60},22 L{X0 + NW - 26},22 L{X0 + NW - 8},34" fill="none" stroke="{OL}" stroke-width="1.2" stroke-linejoin="round"/>
{spikes}<circle cx="{CX}" cy="34" r="9" fill="#FFD166" stroke="{OL}" stroke-width="2"/><path d="M{CX},27 l2.2,4.6 l5,.6 l-3.7,3.4 l1,5 l-4.5,-2.5 l-4.5,2.5 l1,-5 l-3.7,-3.4 l5,-.6 Z" fill="#FFF3C4" stroke="{OL}" stroke-width="1"/>
{lamp(X0 + 40, 40, 6)}{lamp(X0 + NW - 40, 40, 6)}''')

# wall trims, drawn over the car walls (same frame as car_line: 206 x 132 units)
W, H, M = 190, 116, 8
side_ys = range(16, H - 20, 24)
trim1 = ''.join(f'<path d="M{M + x},{M + 1} l6,0 l-5,6 l-6,0 Z" fill="#FFD43B" stroke="{OL}" stroke-width="1"/>' for x in (20, 30, W - 34, W - 24)) + \
        ''.join(f'<path d="M{M + x},{M + H - 7} l6,0 l-5,6 l-6,0 Z" fill="#FFD43B" stroke="{OL}" stroke-width="1"/>' for x in (20, 30, W - 34, W - 24))
trim2 = ''.join(f'<rect x="{M - 1}" y="{M + y}" width="9" height="{18}" rx="2" fill="#C9D3DC" stroke="{OL}" stroke-width="1.2"/><circle cx="{M + 3.5}" cy="{M + y + 4}" r="1" fill="{OL}"/><circle cx="{M + 3.5}" cy="{M + y + 14}" r="1" fill="{OL}"/>'
                f'<rect x="{M + W - 8}" y="{M + y}" width="9" height="18" rx="2" fill="#C9D3DC" stroke="{OL}" stroke-width="1.2"/><circle cx="{M + W - 3.5}" cy="{M + y + 4}" r="1" fill="{OL}"/><circle cx="{M + W - 3.5}" cy="{M + y + 14}" r="1" fill="{OL}"/>'
                for y in (4, H - 22)) + f'<rect x="{M + 30}" y="{M - 1}" width="{W - 60}" height="5" rx="2" fill="#C9D3DC" stroke="{OL}" stroke-width="1.2"/>'
trim3 = f'''<defs><linearGradient id="aur2" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#7CF2C8"/><stop offset=".5" stop-color="#B9A2EE"/><stop offset="1" stop-color="#7CF2C8"/></linearGradient></defs>
<rect x="{M + 1}" y="{M + 14}" width="3" height="{H - 28}" rx="1.5" fill="url(#aur2)"/><rect x="{M + W - 4}" y="{M + 14}" width="3" height="{H - 28}" rx="1.5" fill="url(#aur2)"/>'''
trim4 = ''.join(f'<rect x="{M + x}" y="{y}" width="8" height="9" fill="#FFFFFF" stroke="{OL}" stroke-width="1"/>' for x in (70, W - 78) for y in (M - 1, M + H - 8))
trim5 = ''.join(f'<path d="M{M + x},{M + y} m-7,0 a7,7 0 0 1 14,0 a7,7 0 0 1 -14,0 Z" fill="#FFD166" stroke="{OL}" stroke-width="1.4"/>' for x in (6, W - 6) for y in (6, H - 6)) + \
        ''.join(f'<circle cx="{M + x}" cy="{M + 3}" r="1.6" fill="#FFD166" stroke="{OL}" stroke-width=".8"/><circle cx="{M + x}" cy="{M + H - 3}" r="1.6" fill="#FFD166" stroke="{OL}" stroke-width=".8"/>' for x in range(30, W - 20, 20))
for i, body in enumerate((trim1, trim2, trim3, trim4, trim5), start=1):
    art(f'car_trim{i}', W + 2 * M, H + 2 * M, body, 4)


# ---------------- tiers 6..24: the five nose shapes, each dressed differently ----------------
SHAPES_D = {1: d1, 2: d2, 3: d3, 4: d4, 5: d5}
EMBLEM_Y = {1: 34, 2: 32, 3: 36, 4: 42, 5: 36}
LAMP_POS = {1: (31, 28.5), 2: (68, 36), 3: (42, 44), 4: (50, 46), 5: (46, 40)}


def emblem(kind, cx, cy, col):
    if kind == 'star':
        pts = ' '.join(f'{cx + (8 if k % 2 == 0 else 3.4) * math.sin(k * math.pi / 5)},{cy - (8 if k % 2 == 0 else 3.4) * math.cos(k * math.pi / 5)}' for k in range(10))
        return f'<circle cx="{cx}" cy="{cy}" r="10" fill="{col}" stroke="{OL}" stroke-width="1.8"/><polygon points="{pts}" fill="#FFF6E9" stroke="{OL}" stroke-width="1"/>'
    if kind == 'flake':
        arms = ''.join(f'<line x1="{cx}" y1="{cy}" x2="{cx + 7 * math.cos(k * math.pi / 3)}" y2="{cy + 7 * math.sin(k * math.pi / 3)}" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>' for k in range(6))
        return f'<circle cx="{cx}" cy="{cy}" r="10" fill="{col}" stroke="{OL}" stroke-width="1.8"/>{arms}'
    if kind == 'sun':
        rays = ''.join(f'<line x1="{cx + 9 * math.cos(k * math.pi / 4)}" y1="{cy + 9 * math.sin(k * math.pi / 4)}" x2="{cx + 13 * math.cos(k * math.pi / 4)}" y2="{cy + 13 * math.sin(k * math.pi / 4)}" stroke="{col}" stroke-width="2.4" stroke-linecap="round"/>' for k in range(8))
        return f'{rays}<circle cx="{cx}" cy="{cy}" r="7.5" fill="{col}" stroke="{OL}" stroke-width="1.8"/>'
    if kind == 'moon':
        return f'<circle cx="{cx}" cy="{cy}" r="10" fill="#2B3A67" stroke="{OL}" stroke-width="1.8"/><path d="M{cx + 2},{cy - 7} A7,7 0 1 0 {cx + 2},{cy + 7} A5.5,5.5 0 1 1 {cx + 2},{cy - 7} Z" fill="{col}"/>'
    if kind == 'heart':
        return f'<path d="M{cx},{cy + 8} C{cx - 14},{cy - 2} {cx - 6},{cy - 12} {cx},{cy - 4} C{cx + 6},{cy - 12} {cx + 14},{cy - 2} {cx},{cy + 8} Z" fill="{col}" stroke="{OL}" stroke-width="1.8"/>'
    if kind == 'gem':
        return f'<path d="M{cx - 9},{cy - 3} L{cx - 5},{cy - 8} L{cx + 5},{cy - 8} L{cx + 9},{cy - 3} L{cx},{cy + 9} Z" fill="{col}" stroke="{OL}" stroke-width="1.8"/><path d="M{cx - 9},{cy - 3} L{cx + 9},{cy - 3}" stroke="#FFFFFF" stroke-width="1.2" opacity=".7"/>'
    return ''


def stripes(shape, cols):
    out = ''
    for k, c in enumerate(cols):
        y = 50 - k * 5
        out += f'<rect x="{X0 + 18}" y="{y}" width="{NW - 36}" height="3.4" fill="{c}"/>'
    return f'<g clip-path="url(#nclip{shape})">{out}</g>'


def wings(col):
    return (f'<path d="M{X0 + 12},44 L{X0 - 2},38 L{X0 + 12},34 Z" fill="{col}" stroke="{OL}" stroke-width="1.4"/>'
            f'<path d="M{X0 + NW - 12},44 L{X0 + NW + 2},38 L{X0 + NW - 12},34 Z" fill="{col}" stroke="{OL}" stroke-width="1.4"/>')


def window(shape):
    y = {1: 22, 2: 22, 3: 18, 4: 24, 5: 18}[shape]
    return f'<path d="M{CX - 26},{y + 10} C{CX - 22},{y} {CX + 22},{y} {CX + 26},{y + 10} C{CX + 12},{y + 7} {CX - 12},{y + 7} {CX - 26},{y + 10} Z" fill="#BEE6F7" stroke="{OL}" stroke-width="2"/>'


def spikes(col):
    return ''.join(f'<path d="M{X0 + 22 + k * 20},56 L{X0 + 29 + k * 20},56 L{X0 + 25.5 + k * 20},62 Z" fill="{col}" stroke="{OL}" stroke-width="1.2"/>' for k in range(8))


# (shape, emblem, emblem colour, stripe colours, extras)
TIER_STYLE = [
    (3, 'star', '#FFD166', ['#FFFFFF'], 'window'),            # 6 Polar Comet
    (1, 'flake', '#5E9CCF', ['#FFD43B', '#3C3A40'], ''),      # 7 Glacier Hauler
    (4, 'star', '#FFE49A', ['#FFFFFF', '#5E9CCF'], 'wings'),  # 8 Northern Star
    (3, 'heart', '#F49AB2', ['#FFFFFF'], ''),                 # 9 Snowfox Line
    (5, 'flake', '#9FD3EE', ['#FFFFFF'], 'spikes'),           # 10 Blizzard King
    (2, 'gem', '#BEE6F7', ['#FFFFFF'], ''),                   # 11 Crystal Arrow
    (3, 'sun', '#FFC85C', ['#FFE49A', '#F08A5D'], 'window'),  # 12 Midnight Sun
    (1, 'star', '#E5484D', ['#C9D3DC', '#8F99A4'], 'spikes'), # 13 Tundra Titan
    (4, 'sun', '#FF8F5A', ['#FFD166', '#E5484D'], 'wings'),   # 14 Frostfire
    (3, 'moon', '#E6ECF2', ['#C9D3DC'], 'window'),            # 15 Silver Owl
    (2, 'gem', '#8FC2AE', ['#7CF2C8', '#3F9E74'], 'spikes'),  # 16 Ice Dragon
    (5, 'star', '#B9A2EE', ['#7CF2C8', '#B9A2EE'], ''),       # 17 Aurora Royale
    (1, 'flake', '#FFFFFF', ['#9FD3EE', '#FFFFFF'], 'window'),# 18 Permafrost Express
    (4, 'star', '#FFE49A', ['#FFFFFF', '#FFE49A', '#FFFFFF'], 'wings'),  # 19 Comet Tail
    (3, 'moon', '#FFE49A', ['#2B3A67'], 'window'),            # 20 Starlight Sleeper
    (2, 'sun', '#FFD43B', ['#3C3A40', '#FFD43B'], 'spikes'),  # 21 Thunder Sledge
    (4, 'heart', '#FF8F5A', ['#FFC85C', '#E5484D', '#FFC85C'], 'wings'),  # 22 Winter Phoenix
    (3, 'star', '#FFFFFF', ['#B9A2EE', '#FFFFFF'], 'wings'),  # 23 Celestial Line
    (5, 'sun', '#FFD166', ['#FFD166', '#FFFFFF', '#FFD166'], 'spikes'),  # 24 The Last Light
]
TRIM_COLS = ['#FFFFFF', '#FFD43B', '#5E9CCF', '#F49AB2', '#9FD3EE', '#BEE6F7', '#FFC85C', '#E5484D', '#FF8F5A', '#C9D3DC',
             '#8FC2AE', '#B9A2EE', '#9FD3EE', '#FFE49A', '#FFE49A', '#FFD43B', '#FF8F5A', '#B9A2EE', '#FFD166']

for k, (shape, emb, ecol, scol, extra) in enumerate(TIER_STYLE):
    i = 6 + k
    d = SHAPES_D[shape]
    deco = f'<defs><clipPath id="nclip{shape}"><path d="{d}"/></clipPath></defs>' + stripes(shape, scol)
    if extra == 'window': deco += window(shape)
    if extra == 'wings': deco += wings(ecol)
    if extra == 'spikes': deco += spikes(ecol)
    lx, ly = LAMP_POS[shape]
    deco += emblem(emb, CX, EMBLEM_Y[shape] + (6 if extra == 'window' else 0), ecol) + lamp(X0 + lx - X0 + 0, ly) + lamp(X0 + NW - (lx - X0), ly)
    nose(i, d, f'<path d="{d}" fill="none" stroke="{OL}" stroke-width="2.4" stroke-linejoin="round"/>' + deco)
    # wall trim: a band in the tier's colour along both long walls plus studs at the ends
    c = TRIM_COLS[k]
    style = k % 4
    if style == 0:
        body = f'<rect x="{M + 1}" y="{M + 16}" width="3.4" height="{H - 32}" rx="1.7" fill="{c}" stroke="{OL}" stroke-width=".8"/><rect x="{M + W - 4.4}" y="{M + 16}" width="3.4" height="{H - 32}" rx="1.7" fill="{c}" stroke="{OL}" stroke-width=".8"/>'
    elif style == 1:
        body = ''.join(f'<rect x="{M + x}" y="{M - 1}" width="{w}" height="5" fill="{c}" stroke="{OL}" stroke-width=".8"/>' for x, w in ((24, 30), (W - 54, 30))) + \
               ''.join(f'<rect x="{M + x}" y="{M + H - 4}" width="{w}" height="5" fill="{c}" stroke="{OL}" stroke-width=".8"/>' for x, w in ((24, 30), (W - 54, 30)))
    elif style == 2:
        body = ''.join(f'<circle cx="{M + x}" cy="{M + y}" r="2.2" fill="{c}" stroke="{OL}" stroke-width=".9"/>' for x in range(24, W - 16, 18) for y in (3, H - 3))
    else:
        body = ''.join(f'<path d="M{M + x},{M + y} l5,-4 l5,4 l-5,4 Z" fill="{c}" stroke="{OL}" stroke-width=".9"/>' for x in (14, W - 24) for y in (12, H - 12))
    art(f'car_trim{i}', W + 2 * M, H + 2 * M, body, 4)


# ---------------- more of the frozen world: one set per region the train passes ----------------
# Wind farm / industry
art('spr_turbine', 64, 64, f"""<ellipse cx="30" cy="57" rx="12" ry="3.5" fill="{SNOW_SH}"/>
<path d="M28,57 L30,18 L33,18 L35,57 Z" fill="#E6ECF2" {o(1.8)}/>
<g transform="rotate(25 31 18)"><path d="M31,18 L29,2 C31,0 33,1 33,3 Z" fill="#F4F7FA" {o(1.4)}/>
<path d="M31,18 L46,25 C46,28 44,28 43,28 Z" fill="#F4F7FA" {o(1.4)}/>
<path d="M31,18 L17,27 C15,25 16,23 18,23 Z" fill="#F4F7FA" {o(1.4)}/></g>
<circle cx="31" cy="18" r="3" fill="#C9D3DC" {o(1.4)}/>
{icicles([24, 38], 26, 3)}<path d="M20,58 C24,52 38,52 42,58 Z" fill="#FFFFFF" {o(1.4)}/>""")
art('spr_tank', 64, 64, f"""<ellipse cx="32" cy="56" rx="26" ry="5" fill="{SNOW_SH}"/>
<rect x="10" y="22" width="44" height="34" rx="4" fill="{RUST}" {o(2.2)}/>
<ellipse cx="32" cy="22" rx="22" ry="6" fill="#D9A68E" {o(2)}/>
<path d="M10,34 L54,34 M10,46 L54,46" stroke="{RUST_D}" stroke-width="1.6"/>
<path d="M44,22 L44,56 M48,24 L48,56" stroke="{OL}" stroke-width="1.2"/>
{''.join(f'<path d="M44,{y} L48,{y}" stroke="{OL}" stroke-width="1.2"/>' for y in range(26, 56, 5))}
{snowcap("M10,22 C14,15 50,15 54,22 C44,26 20,26 10,22 Z")}{icicles([14, 20, 26, 38], 55, 3)}
<path d="M2,59 C8,52 22,52 28,58 C36,52 52,52 62,59 Z" fill="#FFFFFF" {o(1.4)}/>""")
art('spr_crane', 64, 64, f"""<ellipse cx="22" cy="57" rx="14" ry="3.5" fill="{SNOW_SH}"/>
<path d="M18,57 L22,8 L26,57 Z" fill="none" stroke="{OL}" stroke-width="5"/><path d="M18,57 L22,8 L26,57" fill="none" stroke="#FFD166" stroke-width="2.6"/>
<path d="M14,10 L58,10" stroke="{OL}" stroke-width="5" stroke-linecap="round"/><path d="M14,10 L58,10" stroke="#FFD166" stroke-width="2.6" stroke-linecap="round"/>
<path d="M50,10 L50,34" stroke="{OL}" stroke-width="1.4"/><rect x="45" y="34" width="10" height="7" rx="1.5" fill="#8F99A4" {o(1.4)}/>
<rect x="10" y="8" width="7" height="8" rx="1.5" fill="#6F7E8C" {o(1.4)}/>
{snowcap("M13,9 C20,6 52,6 59,9 C48,11 24,11 13,9 Z")}<path d="M8,59 C12,52 30,52 36,59 Z" fill="#FFFFFF" {o(1.4)}/>""")
art('spr_container', 64, 64, f"""<ellipse cx="32" cy="55" rx="28" ry="5" fill="{SNOW_SH}"/>
<rect x="5" y="30" width="34" height="24" rx="2" fill="#5E9CCF" {o(2)}/>{''.join(f'<path d="M{x},32 L{x},52" stroke="#3F7DAF" stroke-width="1.4"/>' for x in range(9, 38, 5))}
<g transform="rotate(-8 44 40)"><rect x="28" y="16" width="32" height="22" rx="2" fill="#E8890C" {o(2)}/>{''.join(f'<path d="M{x},18 L{x},36" stroke="#C26F08" stroke-width="1.4"/>' for x in range(32, 58, 5))}
{snowcap("M27,17 C32,12 56,12 61,17 C52,19 36,19 27,17 Z")}</g>
{snowcap("M4,31 C8,26 36,26 40,31 C30,33 14,33 4,31 Z")}<path d="M0,59 C6,50 24,50 30,58 C40,50 56,52 64,59 Z" fill="#FFFFFF" {o(1.4)}/>""")
# Frozen harbour
art('spr_ship', 64, 64, f"""<ellipse cx="32" cy="55" rx="30" ry="5" fill="{SNOW_SH}"/>
<g transform="rotate(-10 32 40)"><path d="M4,36 L60,36 L52,52 L12,52 Z" fill="#B98375" {o(2.2)}/>
<path d="M4,36 L60,36 L58,40 L6,40 Z" fill="#E5484D"/><path d="M4,36 L60,36" stroke="{OL}" stroke-width="2"/>
<rect x="30" y="22" width="18" height="14" rx="2" fill="#F4F7FA" {o(1.8)}/>{''.join(f'<rect x="{x}" y="26" width="3.5" height="4" rx=".8" fill="{FROST}" {o(.9)}/>' for x in (33, 38, 43))}
<rect x="40" y="12" width="5" height="10" fill="{RUST}" {o(1.6)}/><path d="M18,36 L18,14 M18,16 L28,30" stroke="{OL}" stroke-width="1.6"/>
{snowcap("M29,23 C33,18 45,18 49,23 C42,25 36,25 29,23 Z")}{icicles([14, 20, 44, 50], 52, 3)}</g>
<path d="M0,60 C6,48 20,46 30,52 C40,46 58,48 64,58 L64,60 Z" fill="#DDF3FF" {o(1.6)}/><path d="M10,56 L18,54 M40,55 L50,53" stroke="#8EC3E3" stroke-width="1.2"/>""")
art('spr_lighthouse', 64, 64, f"""<ellipse cx="32" cy="57" rx="14" ry="4" fill="{SNOW_SH}"/>
<path d="M24,57 L27,18 L37,18 L40,57 Z" fill="#F4F7FA" {o(2)}/><path d="M25,46 L39,46 L38.4,38 L25.6,38 Z M26.3,30 L37.7,30 L37.2,24 L26.8,24 Z" fill="#E5484D"/>
<rect x="25" y="12" width="14" height="7" rx="1.5" fill="#FFE49A" {o(1.6)}/><path d="M24,12 L32,5 L40,12 Z" fill="#E5484D" {o(1.6)}/>
<rect x="30" y="48" width="5" height="9" rx="1" fill="#8C6A5A" {o(1.2)}/>
{snowcap("M23,12 C26,9 38,9 41,12 C36,13 28,13 23,12 Z")}<path d="M14,59 C18,52 44,51 50,59 Z" fill="#FFFFFF" {o(1.4)}/>""")
art('spr_buoy', 48, 48, f"""<ellipse cx="24" cy="40" rx="16" ry="4" fill="#DDF3FF" {o(1.4)}/>
<path d="M16,40 L18,22 L30,22 L32,40 Z" fill="#E5484D" {o(1.8)}/><path d="M17,31 L31,31" stroke="#fff" stroke-width="3"/>
<path d="M24,22 L24,12" stroke="{OL}" stroke-width="1.6"/><circle cx="24" cy="11" r="3" fill="#FFE49A" {o(1.4)}/>
{snowcap("M17,23 C20,19 28,19 31,23 C27,24 21,24 17,23 Z")}""")
# Survivor camps in the mountains
art('spr_tent', 64, 64, f"""<ellipse cx="32" cy="56" rx="26" ry="4.5" fill="{SNOW_SH}"/>
<path d="M8,54 L32,16 L56,54 Z" fill="#E8890C" {o(2.2)}/><path d="M32,16 L26,54 L38,54 Z" fill="#5A4636" {o(1.6)}/>
<path d="M32,16 L32,10" stroke="{OL}" stroke-width="1.8"/><path d="M32,10 L40,12 L32,14 Z" fill="#E5484D" {o(1)}/>
{snowcap("M20,34 C25,26 30,18 32,16 C34,18 39,26 44,34 C39,32 25,32 20,34 Z")}
<path d="M2,59 C8,52 22,52 26,57 C34,52 52,52 62,59 Z" fill="#FFFFFF" {o(1.4)}/>""")
art('spr_campfire', 48, 48, f"""<ellipse cx="24" cy="39" rx="16" ry="4" fill="{SNOW_SH}"/>
<path d="M12,38 L36,30 M12,30 L36,38" stroke="{OL}" stroke-width="5.4" stroke-linecap="round"/>
<path d="M12,38 L36,30 M12,30 L36,38" stroke="#B9876A" stroke-width="3.2" stroke-linecap="round"/>
<path d="M24,10 C30,18 32,24 29,30 C27,34 21,34 19,30 C16,25 19,20 22,24 C21,18 22,14 24,10 Z" fill="url(#glow)" {o(1.8)}/>
<path d="M24,20 C27,24 27,28 25,31 C23,32 21,30 22,27 Z" fill="#FF8F5A"/>
{''.join(f'<ellipse cx="{x}" cy="{y}" rx="3.2" ry="2.4" fill="#A3ABB5" {o(1)}/>' for x, y in ((10, 36), (14, 40), (24, 42), (34, 40), (38, 36)))}""")
art('spr_cliff', 64, 64, f"""<ellipse cx="32" cy="56" rx="30" ry="5" fill="{SNOW_SH}"/>
<path d="M2,56 L8,30 L18,22 L24,10 L34,14 L42,6 L50,18 L58,24 L62,56 Z" fill="#9AA6B4" {o(2.2)}/>
<path d="M24,10 L28,56 M42,6 L40,56 M18,22 L14,56 M50,18 L52,56" stroke="#7C8794" stroke-width="1.4"/>
{snowcap("M8,30 L18,22 L24,10 L34,14 L42,6 L50,18 L58,24 C50,30 44,22 38,26 C30,30 24,22 18,30 C14,34 10,32 8,30 Z")}
<path d="M0,60 C6,52 22,52 30,57 C40,52 56,52 64,60 Z" fill="#FFFFFF" {o(1.4)}/>""")
# frozen lake: a flat patch on the ground (seen from above)
art('spr_lake', 96, 64, f"""<ellipse cx="48" cy="32" rx="44" ry="26" fill="#DDF3FF" {o(2)}/>
<ellipse cx="48" cy="32" rx="34" ry="18" fill="#C6E8FA"/>
<path d="M22,24 L36,30 L44,26 L58,36 M50,30 L56,22 M36,30 L32,42" fill="none" stroke="#8EC3E3" stroke-width="1.4" stroke-linecap="round"/>
<path d="M60,20 C66,18 72,20 74,24" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-linecap="round"/>""")

# ---------------- coin booster icons (48 x 48, like the other shop items) ----------------
def coin_stack(n, label, ring):
    coins = ''.join(f'<ellipse cx="24" cy="{36 - k * 5}" rx="13" ry="5" fill="url(#gold)" {o(1.8)}/>' for k in range(n))
    return f"""<ellipse cx="24" cy="42" rx="15" ry="3" fill="#000" opacity=".1"/>{coins}
<path d="M18,{34 - (n - 1) * 5} l3,-2" stroke="#fff" stroke-width="1.6" stroke-linecap="round"/>
<rect x="22" y="2" width="24" height="15" rx="7.5" fill="{ring}" {o(1.8)}/>
<text x="34" y="13.6" text-anchor="middle" font-family="Inter Display, Inter, sans-serif" font-weight="900" font-size="{11 if len(label) < 4 else 9}" fill="#fff" stroke="{OL}" stroke-width="2.2" paint-order="stroke">{label}</text>"""
art('item_rush5', 48, 48, coin_stack(3, 'x5', '#F08A5D'))
art('item_rush10', 48, 48, coin_stack(4, 'x10', '#D6336C'))
art('item_rush100', 48, 48, coin_stack(5, 'x100', '#8E6FD8'))


# ---------------- a wolf (side view, facing right) ----------------
art('spr_wolf', 64, 48, f"""<ellipse cx="32" cy="44" rx="22" ry="3" fill="#000" opacity=".12"/>
<path d="M10,26 C4,22 2,16 6,14 C8,20 12,22 16,23" fill="#8F99A4" {o(1.8)}/>
<path d="M14,22 C18,16 34,15 42,18 L50,14 L52,8 L55,14 C60,15 62,19 60,22 L52,25 C50,30 46,32 42,32 L40,42 L35,42 L36,33 L24,33 L22,42 L17,42 L18,31 C14,30 12,27 14,22 Z" fill="#A3ADB8" {o(1.8)}/>
<path d="M20,30 C26,27 36,27 42,30" fill="none" stroke="#DCE3EA" stroke-width="3" stroke-linecap="round"/>
<path d="M52,8 L50,15 L54,14 Z" fill="#6F7E8C"/><circle cx="54.5" cy="18" r="1.4" fill="#2B2A33"/>
<path d="M59,21 L62,21" stroke="{OL}" stroke-width="1.6" stroke-linecap="round"/>
<path d="M30,17 C34,15 40,15 44,17" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round"/>""")

# ---------------- keepsakes for the album: 6 per region ----------------
REGION_TINT = ['#BFE0D2', '#FFD9C2', '#CDE7F8', '#FFE49A', '#B8D8F2', '#FFC9D6']
NAMES = [
    ["Pine Cone Charm", "Carved Owl", "Lost Mitten", "Ranger Badge", "Birch Whistle", "Frozen Acorn"],
    ["Old Photograph", "Toy Train", "Street Sign Plate", "Shop Bell", "Snow Globe", "Love Letter"],
    ["Ice Crystal", "Skate Blade", "Frozen Fish", "Polar Compass", "Sled Bell", "Glacier Pearl"],
    ["Brass Gear", "Turbine Bolt", "Worker Helmet", "Old Fuse", "Copper Coil", "Oil Lamp"],
    ["Ship in a Bottle", "Anchor Pin", "Seashell", "Captain's Log", "Lighthouse Key", "Sea Glass"],
    ["Campfire Mug", "Hand-knit Scarf", "Harmonica", "Survivor Map", "Tin Star", "Warm Socks"],
]
SHAPES = {
    'cone': f'<ellipse cx="24" cy="26" rx="8" ry="11" fill="#B9876A" {o(1.6)}/><path d="M17,22 L31,22 M16,27 L32,27 M17,32 L31,32" stroke="#8C5E4C" stroke-width="1.4"/><path d="M24,15 L24,10" stroke="{OL}" stroke-width="1.6"/>',
    'owl': f'<ellipse cx="24" cy="27" rx="10" ry="12" fill="#D9A576" {o(1.6)}/><circle cx="20" cy="23" r="3.6" fill="#fff" {o(1.2)}/><circle cx="28" cy="23" r="3.6" fill="#fff" {o(1.2)}/><circle cx="20" cy="23" r="1.4" fill="{OL}"/><circle cx="28" cy="23" r="1.4" fill="{OL}"/><path d="M23,27 L25,27 L24,29 Z" fill="#E8890C"/>',
    'mitten': f'<path d="M17,36 L17,20 C17,14 30,14 30,20 L30,24 C34,22 36,26 33,29 L30,32 L30,36 Z" fill="#E5484D" {o(1.6)}/><rect x="16" y="33" width="15" height="5" rx="2" fill="#fff" {o(1.4)}/>',
    'badge': f'<path d="M24,12 L33,16 L32,28 C30,34 24,37 24,37 C24,37 18,34 16,28 L15,16 Z" fill="url(#gold)" {o(1.6)}/><path d="M24,19 l2,4 l4.4,.6 l-3.2,3 l.8,4.3 l-4,-2.1 l-4,2.1 l.8,-4.3 l-3.2,-3 l4.4,-.6 Z" fill="#fff" {o(1)}/>',
    'whistle': f'<rect x="12" y="20" width="22" height="10" rx="5" fill="#E9BC8C" {o(1.6)}/><circle cx="17" cy="25" r="2" fill="{OL}"/><path d="M34,22 L38,20 L38,30 L34,28" fill="#D9A576" {o(1.4)}/>',
    'acorn': f'<ellipse cx="24" cy="28" rx="8" ry="9" fill="#C98F72" {o(1.6)}/><path d="M15,22 C15,16 33,16 33,22 Z" fill="#8C6A5A" {o(1.6)}/><path d="M24,16 L24,12" stroke="{OL}" stroke-width="1.8"/><path d="M19,25 C20,30 22,32 24,33" stroke="{ICE}" stroke-width="1.6" fill="none"/>',
    'photo': f'<rect x="13" y="13" width="22" height="24" rx="1.5" fill="#FFF6E9" {o(1.6)}/><rect x="16" y="16" width="16" height="13" fill="#B8D8F2"/><path d="M16,29 L22,22 L26,26 L28,24 L32,29 Z" fill="#8FC2AE"/>',
    'train': f'<rect x="12" y="20" width="16" height="12" rx="2" fill="#E5484D" {o(1.6)}/><rect x="28" y="14" width="9" height="18" rx="2" fill="#5E9CCF" {o(1.6)}/><circle cx="17" cy="34" r="3" fill="#4A4550" {o(1.2)}/><circle cx="30" cy="34" r="3" fill="#4A4550" {o(1.2)}/>',
    'sign': f'<rect x="11" y="17" width="26" height="14" rx="2" fill="#2F8FE0" {o(1.6)}/><path d="M15,24 L33,24" stroke="#fff" stroke-width="2.4" stroke-dasharray="3 2"/>',
    'bell': f'<path d="M16,31 C16,20 18,15 24,15 C30,15 32,20 32,31 Z" fill="url(#gold)" {o(1.6)}/><rect x="14" y="30" width="20" height="4" rx="2" fill="#F2B84B" {o(1.4)}/><circle cx="24" cy="36" r="2.2" fill="#C98A1E" {o(1)}/>',
    'globe': f'<rect x="16" y="32" width="16" height="6" rx="2" fill="#8C6A5A" {o(1.6)}/><circle cx="24" cy="24" r="10" fill="#DDF3FF" {o(1.6)}/><path d="M18,28 L24,20 L30,28 Z" fill="#7FC29A"/><circle cx="21" cy="19" r="1" fill="#fff"/><circle cx="27" cy="22" r="1" fill="#fff"/>',
    'letter': f'<rect x="11" y="16" width="26" height="18" rx="2" fill="#FFF6E9" {o(1.6)}/><path d="M11,17 L24,27 L37,17" fill="none" stroke="{OL}" stroke-width="1.6"/><path d="M24,24 C22,21 19,23 21,25 L24,28 L27,25 C29,23 26,21 24,24 Z" fill="#E5484D"/>',
    'crystal': f'<path d="M24,10 L32,20 L28,38 L20,38 L16,20 Z" fill="{ICE}" {o(1.6)}/><path d="M24,10 L24,38 M16,20 L32,20" stroke="{ICE_D}" stroke-width="1.2"/>',
    'blade': f'<path d="M12,30 L36,30 C38,30 38,33 36,33 L12,33 Z" fill="#C9D3DC" {o(1.6)}/><rect x="16" y="16" width="16" height="12" rx="3" fill="#E5484D" {o(1.6)}/><path d="M20,28 L20,30 M28,28 L28,30" stroke="{OL}" stroke-width="1.6"/>',
    'fish': f'<path d="M12,24 C18,16 30,16 34,24 C30,32 18,32 12,24 Z" fill="#9FD3EE" {o(1.6)}/><path d="M34,24 L40,18 L40,30 Z" fill="#9FD3EE" {o(1.6)}/><circle cx="18" cy="23" r="1.4" fill="{OL}"/>',
    'compass': f'<circle cx="24" cy="25" r="11" fill="url(#gold)" {o(1.6)}/><circle cx="24" cy="25" r="8" fill="#FFF6E9" {o(1.2)}/><path d="M24,18 L26,25 L24,32 L22,25 Z" fill="#E5484D" {o(.8)}/>',
    'pearl': f'<path d="M12,30 C12,20 36,20 36,30 Z" fill="#B8C6D6" {o(1.6)}/><circle cx="24" cy="26" r="6" fill="#FFFFFF" {o(1.4)}/><circle cx="22" cy="24" r="1.6" fill="#DDF3FF"/>',
    'gear': f'<circle cx="24" cy="24" r="9" fill="#F2B84B" {o(1.6)}/>' + ''.join(f'<rect x="22" y="11" width="4" height="5" fill="#F2B84B" {o(1.2)} transform="rotate({a} 24 24)"/>' for a in range(0, 360, 45)) + f'<circle cx="24" cy="24" r="3.5" fill="#FFF6E9" {o(1.2)}/>',
    'bolt': f'<rect x="21" y="12" width="6" height="22" rx="1" fill="#C9D3DC" {o(1.6)}/><path d="M21,16 L27,18 M21,20 L27,22 M21,24 L27,26" stroke="#8F99A4" stroke-width="1.2"/><rect x="16" y="32" width="16" height="6" rx="1.5" fill="#A3ABB5" {o(1.6)}/>',
    'helmet': f'<path d="M12,30 C12,18 36,18 36,30 Z" fill="#FFD166" {o(1.6)}/><rect x="10" y="29" width="28" height="5" rx="2.5" fill="#FFC85C" {o(1.6)}/><path d="M24,19 L24,29" stroke="#E8A93C" stroke-width="3"/>',
    'fuse': f'<rect x="15" y="18" width="18" height="12" rx="3" fill="#BEE6F7" {o(1.6)}/><rect x="11" y="20" width="5" height="8" rx="1" fill="#C9D3DC" {o(1.4)}/><rect x="32" y="20" width="5" height="8" rx="1" fill="#C9D3DC" {o(1.4)}/><path d="M17,24 C20,20 22,28 25,24 C27,21 29,26 31,24" stroke="#E8890C" stroke-width="1.4" fill="none"/>',
    'coil': ''.join(f'<ellipse cx="24" cy="{16 + k * 4}" rx="9" ry="3" fill="none" stroke="{OL}" stroke-width="3.6"/><ellipse cx="24" cy="{16 + k * 4}" rx="9" ry="3" fill="none" stroke="#D98457" stroke-width="2"/>' for k in range(6)),
    'lamp': f'<rect x="18" y="16" width="12" height="16" rx="3" fill="#FFF3C4" {o(1.6)}/><rect x="17" y="13" width="14" height="4" rx="2" fill="#8F99A4" {o(1.4)}/><rect x="17" y="31" width="14" height="5" rx="2" fill="#8F99A4" {o(1.4)}/><circle cx="24" cy="24" r="3" fill="#FFB86B"/>',
    'bottle': f'<path d="M14,22 L30,22 C34,22 34,34 30,34 L14,34 C10,34 10,22 14,22 Z" fill="#DDF3FF" {o(1.6)}/><rect x="33" y="25" width="5" height="6" rx="1" fill="#B9876A" {o(1.2)}/><path d="M16,31 L28,31 L26,27 L22,27 L22,24 L20,27 L18,27 Z" fill="#B98375"/>',
    'anchor': f'<path d="M24,14 L24,34 M17,22 L31,22 M14,28 C16,36 32,36 34,28" fill="none" stroke="{OL}" stroke-width="4.4" stroke-linecap="round"/><path d="M24,14 L24,34 M17,22 L31,22 M14,28 C16,36 32,36 34,28" fill="none" stroke="#5E9CCF" stroke-width="2.2" stroke-linecap="round"/><circle cx="24" cy="12" r="3" fill="none" stroke="{OL}" stroke-width="1.8"/>',
    'shell': f'<path d="M24,36 L12,22 C14,12 34,12 36,22 Z" fill="#FFC9D6" {o(1.6)}/><path d="M24,36 L18,16 M24,36 L24,14 M24,36 L30,16" stroke="#F49AB2" stroke-width="1.4"/>',
    'book': f'<rect x="13" y="13" width="22" height="24" rx="2" fill="#8C6A5A" {o(1.6)}/><rect x="16" y="16" width="16" height="18" rx="1" fill="#B98375"/><path d="M18,22 L30,22 M18,26 L28,26" stroke="#FFE49A" stroke-width="1.6"/>',
    'key': f'<circle cx="17" cy="24" r="6" fill="none" stroke="{OL}" stroke-width="4.2"/><circle cx="17" cy="24" r="6" fill="none" stroke="#F2B84B" stroke-width="2.2"/><path d="M23,24 L37,24 M33,24 L33,29 M29,24 L29,28" stroke="{OL}" stroke-width="4.2" stroke-linecap="round"/><path d="M23,24 L37,24 M33,24 L33,29 M29,24 L29,28" stroke="#F2B84B" stroke-width="2.2" stroke-linecap="round"/>',
    'glass': f'<path d="M16,20 L26,14 L34,22 L30,34 L18,32 Z" fill="#8FC2AE" opacity=".85" {o(1.6)}/><path d="M22,20 L26,18" stroke="#fff" stroke-width="2" stroke-linecap="round"/>',
    'mug': f'<rect x="14" y="17" width="16" height="19" rx="3" fill="#E5484D" {o(1.6)}/><path d="M30,21 C37,21 37,31 30,31" fill="none" stroke="{OL}" stroke-width="4"/><path d="M30,21 C37,21 37,31 30,31" fill="none" stroke="#E5484D" stroke-width="2"/><path d="M19,13 C17,10 21,9 19,6 M25,13 C23,10 27,9 25,6" stroke="#fff" stroke-width="1.8" fill="none" stroke-linecap="round"/>',
    'scarf': f'<path d="M12,18 C20,14 28,14 36,18 L36,24 C28,20 20,20 12,24 Z" fill="#F08A5D" {o(1.6)}/><path d="M26,21 L30,38 L24,38 L22,22 Z" fill="#F08A5D" {o(1.6)}/><path d="M16,19 L16,23 M22,17 L22,21 M30,17 L30,21" stroke="#FFE49A" stroke-width="2"/>',
    'harmonica': f'<rect x="10" y="19" width="28" height="11" rx="2" fill="#C9D3DC" {o(1.6)}/>' + ''.join(f'<rect x="{12 + k * 4.4}" y="23" width="2.6" height="3" fill="{OL}"/>' for k in range(6)),
    'map': f'<path d="M10,16 L19,13 L29,16 L38,13 L38,33 L29,36 L19,33 L10,36 Z" fill="#FFF0C8" {o(1.6)}/><path d="M19,13 L19,33 M29,16 L29,36" stroke="#D9C6AE" stroke-width="1.2"/><path d="M13,30 C18,26 22,28 26,22 C29,19 32,20 35,18" stroke="#E5484D" stroke-width="1.6" stroke-dasharray="2 2" fill="none"/>',
    'star': f'<path d="M24,11 l4,8 l9,1.3 l-6.5,6.2 l1.6,8.9 l-8.1,-4.3 l-8.1,4.3 l1.6,-8.9 l-6.5,-6.2 l9,-1.3 Z" fill="#C9D3DC" {o(1.6)}/><circle cx="24" cy="25" r="3" fill="#A3ABB5"/>',
    'socks': f'<path d="M15,12 L22,12 L22,26 C22,30 26,30 30,30 C34,30 34,37 30,37 L20,37 C16,37 15,34 15,30 Z" fill="#B9A2EE" {o(1.6)}/><path d="M15,17 L22,17 M15,21 L22,21" stroke="#fff" stroke-width="2"/>',
}
SHAPE_FOR = [
    ['cone', 'owl', 'mitten', 'badge', 'whistle', 'acorn'],
    ['photo', 'train', 'sign', 'bell', 'globe', 'letter'],
    ['crystal', 'blade', 'fish', 'compass', 'bell', 'pearl'],
    ['gear', 'bolt', 'helmet', 'fuse', 'coil', 'lamp'],
    ['bottle', 'anchor', 'shell', 'book', 'key', 'glass'],
    ['mug', 'scarf', 'harmonica', 'map', 'star', 'socks'],
]
for r in range(6):
    for i in range(6):
        body = f'<circle cx="24" cy="24" r="21" fill="{REGION_TINT[r]}" {o(2)}/><circle cx="24" cy="24" r="17" fill="#FFFFFF" opacity=".55"/>' + SHAPES[SHAPE_FOR[r][i]]
        art(f'col_{r}_{i}', 48, 48, body, 4)


# ---------------- rendering ----------------
async def render(only):
    from playwright.async_api import async_playwright
    jobs = [(n, *v) for n, v in ART.items() if only in n]
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page(device_scale_factor=1)
        for name, w, h, body, sc in jobs:
            pw, ph = int(w * sc), int(h * sc)
            await pg.set_viewport_size({'width': pw, 'height': ph})
            await pg.set_content(C.svg(w, h, body, pw, ph))
            png = await pg.screenshot(omit_background=True, clip={'x': 0, 'y': 0, 'width': pw, 'height': ph})
            img = Image.open(io.BytesIO(png)).convert('RGBA')
            img.save(os.path.join(OUT, name + '.webp'), 'WEBP', quality=92, method=6)
            print('rendered', name, img.size)
        await b.close()
    return [j[0] for j in jobs]


def sheet(names, path):
    ims = [Image.open(os.path.join(OUT, n + '.webp')).convert('RGBA') for n in names]
    cell = 260
    cols = 6
    rows = (len(ims) + cols - 1) // cols
    out = Image.new('RGBA', (cols * cell, rows * cell), (232, 240, 247, 255))
    for k, im in enumerate(ims):
        im.thumbnail((cell - 10, cell - 10))
        out.alpha_composite(im, (k % cols * cell + 5, k // cols * cell + 5))
    out.save(path)
    print('sheet', path)


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    names = asyncio.run(render(args[0] if args else ''))
    if '--sheet' in sys.argv:
        sheet(names, os.environ.get('SHEET', '/tmp/world_sheet.png'))
