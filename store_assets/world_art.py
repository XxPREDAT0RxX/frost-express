"""The frozen world outside the train: the ruins of a world that froze over, the bigger stations,
a different nose and trim for every train tier. Same cozy style as cozy_art.py (and uses its helpers).

    python3 store_assets/world_art.py           # everything in this file
    python3 store_assets/world_art.py spr_      # only names containing this text
    python3 store_assets/world_art.py --sheet   # also writes a preview contact sheet
"""
import asyncio, io, os, sys
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
trim4 = f'''<rect x="{M + 70}" y="{M - 1}" width="8" height="{H + 2}" fill="#FFFFFF" opacity=".9"/><rect x="{M + W - 78}" y="{M - 1}" width="8" height="{H + 2}" fill="#FFFFFF" opacity=".9"/>
<rect x="{M + 70}" y="{M - 1}" width="8" height="{H + 2}" fill="none" stroke="{OL}" stroke-width="1"/><rect x="{M + W - 78}" y="{M - 1}" width="8" height="{H + 2}" fill="none" stroke="{OL}" stroke-width="1"/>'''
trim5 = ''.join(f'<path d="M{M + x},{M + y} m-7,0 a7,7 0 0 1 14,0 a7,7 0 0 1 -14,0 Z" fill="#FFD166" stroke="{OL}" stroke-width="1.4"/>' for x in (6, W - 6) for y in (6, H - 6)) + \
        ''.join(f'<circle cx="{M + x}" cy="{M + 3}" r="1.6" fill="#FFD166" stroke="{OL}" stroke-width=".8"/><circle cx="{M + x}" cy="{M + H - 3}" r="1.6" fill="#FFD166" stroke="{OL}" stroke-width=".8"/>' for x in range(30, W - 20, 20))
for i, body in enumerate((trim1, trim2, trim3, trim4, trim5), start=1):
    art(f'car_trim{i}', W + 2 * M, H + 2 * M, body, 4)


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
