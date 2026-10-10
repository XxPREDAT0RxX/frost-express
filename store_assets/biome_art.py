"""Biome scenery for the world the Little Express travels through: meadow, forest, desert,
rainforest and savanna. Same cozy pastel style as cozy_art.py / world_art.py.

    python3 store_assets/biome_art.py           # render every biome sprite
    python3 store_assets/biome_art.py spr_cact  # only names containing this text
    python3 store_assets/biome_art.py --sheet   # also write a preview contact sheet
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


# soft contact shadow that works on any ground colour
def sh(cx, cy, rx, ry=None):
    return f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry or rx * 0.26:.1f}" fill="#000" opacity=".12"/>'


# ---------------- meadow & forest: leafy green, no snow ----------------
art('spr_tree_round', 64, 64, f'''{sh(33, 54, 20, 5)}
<rect x="29" y="40" width="7" height="15" rx="3" fill="#B9876A" {o(2)}/><path d="M30,47 L35,47" stroke="#9A6E52" stroke-width="1.4"/>
<circle cx="32" cy="26" r="20" fill="#8FCBA6" {o(2.6)}/>
<path d="M18,22 C24,28 40,28 46,22" fill="none" stroke="#6FB08B" stroke-width="3" stroke-linecap="round"/>
<circle cx="24" cy="20" r="6" fill="#A6D9BB" opacity=".8"/><circle cx="40" cy="30" r="4" fill="#6FB08B" opacity=".6"/>''')

art('spr_tree_tall', 64, 64, f'''{sh(33, 56, 17, 4.5)}
<rect x="30" y="38" width="6" height="18" rx="2.6" fill="#B9876A" {o(2)}/>
<path d="M32,4 C42,14 46,24 42,28 C50,33 52,42 44,45 L20,45 C12,42 14,33 22,28 C18,24 22,14 32,4 Z" fill="#7FC29A" {o(2.6)}/>
<path d="M22,28 C27,32 37,32 42,28" fill="none" stroke="#6FB08B" stroke-width="2.6" stroke-linecap="round"/>
<circle cx="28" cy="18" r="4.5" fill="#A6D9BB" opacity=".8"/>''')

art('spr_bush', 48, 48, f'''{sh(24, 40, 17, 4.2)}
<path d="M7,38 C2,28 9,21 17,23 C19,15 29,15 31,23 C39,21 45,30 40,38 Z" fill="#8FCBA6" {o(2.2)}/>
<path d="M15,24 C19,20 27,20 30,24" fill="none" stroke="#6FB08B" stroke-width="2.2" stroke-linecap="round"/>
<circle cx="14" cy="30" r="2" fill="#F2A0B5"/><circle cx="34" cy="31" r="2" fill="#F2A0B5"/><circle cx="24" cy="26" r="2" fill="#FFD36A"/>''')

art('spr_flowers', 48, 48, f'''{sh(24, 41, 15, 3.6)}
<path d="M14,42 L14,30 M24,42 L24,26 M34,42 L34,32" stroke="#6FB08B" stroke-width="2.2" stroke-linecap="round"/>
<g>{''.join(f'<circle cx="{14+dx}" cy="{30+dy}" r="2.4" fill="#F28CB0"/>' for dx,dy in [(-3,0),(3,0),(0,-3),(0,3)])}<circle cx="14" cy="30" r="2" fill="#FFD36A"/></g>
<g>{''.join(f'<circle cx="{24+dx}" cy="{26+dy}" r="2.4" fill="#FFFFFF" stroke="{OL}" stroke-width=".5"/>' for dx,dy in [(-3,0),(3,0),(0,-3),(0,3)])}<circle cx="24" cy="26" r="2" fill="#FFD36A"/></g>
<g>{''.join(f'<circle cx="{34+dx}" cy="{32+dy}" r="2.2" fill="#B59CE8"/>' for dx,dy in [(-3,0),(3,0),(0,-3),(0,3)])}<circle cx="34" cy="32" r="1.8" fill="#FFD36A"/></g>''')

# ---------------- desert: sand, cactus, mesa ----------------
art('spr_cactus', 64, 64, f'''{sh(32, 56, 14, 4)}
<path d="M28,56 L28,24 C28,20 36,20 36,24 L36,56 Z" fill="#6FB07A" {o(2.4)}/>
<path d="M28,38 C22,38 18,34 18,27 C18,23 23,23 23,27 L23,33 C23,36 25,38 28,38 Z" fill="#6FB07A" {o(2)}/>
<path d="M36,42 C42,42 46,38 46,31 C46,27 41,27 41,31 L41,37 C41,40 39,42 36,42 Z" fill="#6FB07A" {o(2)}/>
<path d="M32,26 L32,52 M30,30 L34,30" stroke="#569063" stroke-width="1.4" stroke-linecap="round"/>
<path d="M30,22 C31,19 33,19 34,22 Z" fill="#F2A0B5"/>''')

art('spr_cactus_ball', 48, 48, f'''{sh(24, 40, 14, 3.6)}
<path d="M11,34 C11,22 37,22 37,34 C37,40 31,42 24,42 C17,42 11,40 11,34 Z" fill="#6FB07A" {o(2.2)}/>
<path d="M16,26 L16,38 M24,23 L24,41 M32,26 L32,38" stroke="#569063" stroke-width="1.6" stroke-linecap="round"/>
<circle cx="24" cy="22" r="3" fill="#F7C948"/><path d="M22,22 C22,19 26,19 26,22 Z" fill="#F28CB0"/>''')

art('spr_mesa', 64, 64, f'''{sh(32, 57, 28, 5)}
<path d="M10,56 L13,30 L51,30 L54,56 Z" fill="#D9A86A" {o(2.4)}/>
<path d="M13,30 L51,30 L50,36 L14,36 Z" fill="#E8C089"/>
<path d="M18,56 L20,38 M44,56 L46,40 M31,56 L31,40" stroke="#B88A50" stroke-width="1.6"/>
<path d="M12,47 L52,47" stroke="#B88A50" stroke-width="1.4" opacity=".7"/>''')

art('spr_dry_bush', 48, 48, f'''{sh(24, 40, 15, 3.6)}
<g stroke="#C2A362" stroke-width="2" stroke-linecap="round" fill="none">
<path d="M24,41 L24,24"/><path d="M24,32 C19,30 16,24 15,19"/><path d="M24,30 C29,28 33,23 34,18"/><path d="M24,36 C20,35 17,32 14,30"/><path d="M24,34 C28,33 32,31 35,29"/></g>
<circle cx="15" cy="19" r="1.4" fill="#D9C488"/><circle cx="34" cy="18" r="1.4" fill="#D9C488"/>''')

# ---------------- rainforest: palms, big leaves, ferns ----------------
art('spr_palm', 64, 64, f'''{sh(33, 57, 15, 4.2)}
<path d="M31,56 C30,42 32,30 33,22 C33,24 33,54 35,56 Z" fill="#B9876A" {o(2.2)}/>
<path d="M32,54 C31,46 32,36 33,24" fill="none" stroke="#9A6E52" stroke-width="1.2"/>
<g fill="#4E9A6B" {o(2)}>
<path d="M33,22 C24,16 14,16 8,20 C16,18 26,20 33,24 Z"/>
<path d="M33,22 C42,16 52,16 58,20 C50,18 40,20 33,24 Z"/>
<path d="M33,21 C28,12 20,8 13,8 C22,12 29,16 33,24 Z"/>
<path d="M33,21 C38,12 46,8 53,8 C44,12 37,16 33,24 Z"/>
<path d="M33,20 C33,12 33,7 33,4 C35,12 35,18 34,24 Z"/></g>
<circle cx="31" cy="24" r="2.2" fill="#C2772E"/><circle cx="36" cy="25" r="2" fill="#C2772E"/>''')

art('spr_jungle', 64, 64, f'''{sh(32, 56, 22, 5)}
<g fill="#3F8A5A" {o(2.4)}>
<path d="M32,56 C20,50 12,38 14,26 C18,36 26,44 32,48 Z"/>
<path d="M32,56 C44,50 52,38 50,26 C46,36 38,44 32,48 Z"/></g>
<g fill="#55A572" {o(2)}>
<path d="M32,54 C26,46 24,34 28,24 C30,34 31,44 32,50 Z"/>
<path d="M32,54 C38,46 40,34 36,24 C34,34 33,44 32,50 Z"/></g>
<path d="M18,30 C24,38 30,44 32,50 M46,30 C40,38 34,44 32,50 M28,27 L32,50 M36,27 L32,50" stroke="#2E6B43" stroke-width="1.2" fill="none" opacity=".7"/>''')

art('spr_fern', 48, 48, f'''{sh(24, 41, 16, 3.8)}
<g fill="#55A572" {o(2)}>
<path d="M24,42 C14,38 9,30 10,22 C14,28 20,34 24,38 Z"/>
<path d="M24,42 C34,38 39,30 38,22 C34,28 28,34 24,38 Z"/>
<path d="M24,42 C22,34 22,26 24,18 C26,26 26,34 24,40 Z"/></g>
<path d="M12,24 L24,38 M36,24 L24,38 M24,20 L24,40" stroke="#3F8A5A" stroke-width="1.1" opacity=".7"/>''')

# ---------------- savanna: acacia, golden grass, boulder ----------------
art('spr_acacia', 64, 64, f'''{sh(33, 57, 20, 4.6)}
<path d="M31,56 L31,30 M31,38 C26,34 22,30 18,28 M31,34 C36,31 41,28 46,27" fill="none" stroke="#9C7E5A" stroke-width="3" stroke-linecap="round"/>
<path d="M8,26 C8,19 18,15 32,15 C46,15 56,19 56,26 C56,29 50,30 44,29 L20,29 C14,30 8,29 8,26 Z" fill="#9CB36A" {o(2.6)}/>
<path d="M14,24 C24,27 40,27 50,24" fill="none" stroke="#86A055" stroke-width="2.4" stroke-linecap="round"/>''')

art('spr_grass_tuft', 48, 48, f'''{sh(24, 41, 14, 3.4)}
<g stroke="#D9B24E" stroke-width="2.2" stroke-linecap="round" fill="none">
<path d="M24,42 C22,34 20,27 17,21"/><path d="M24,42 C24,33 24,26 24,18"/><path d="M24,42 C26,34 29,27 32,21"/>
<path d="M24,42 C21,36 18,32 14,28"/><path d="M24,42 C27,36 30,32 34,28"/></g>
<circle cx="17" cy="21" r="1.3" fill="#E8C66A"/><circle cx="24" cy="18" r="1.3" fill="#E8C66A"/><circle cx="32" cy="21" r="1.3" fill="#E8C66A"/>''')

art('spr_boulder', 48, 48, f'''{sh(24, 40, 17, 4.2)}
<path d="M8,38 C6,26 14,18 24,19 C34,18 42,26 40,38 Z" fill="#CBB79A" {o(2.2)}/>
<path d="M14,24 C20,20 30,20 34,25 C28,23 20,23 14,24 Z" fill="#DFCFB4"/>
<path d="M12,33 L40,33 M20,19 L22,38" stroke="#B29E80" stroke-width="1.2" opacity=".7"/>''')


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
            Image.open(io.BytesIO(png)).convert('RGBA').save(os.path.join(OUT, name + '.webp'), 'WEBP', quality=92, method=6)
            print('rendered', name)
        await b.close()
    return [j[0] for j in jobs]


def sheet(names, path):
    ims = [Image.open(os.path.join(OUT, n + '.webp')).convert('RGBA') for n in names]
    cell, cols = 200, 5
    rows = (len(ims) + cols - 1) // cols
    out = Image.new('RGBA', (cols * cell, rows * cell), (210, 225, 205, 255))
    for k, im in enumerate(ims):
        im.thumbnail((cell - 10, cell - 10))
        out.alpha_composite(im, (k % cols * cell + 5, k // cols * cell + 5))
    out.save(path)
    print('sheet', path)


if __name__ == '__main__':
    args = [a for a in sys.argv[1:] if not a.startswith('--')]
    names = asyncio.run(render(args[0] if args else ''))
    if '--sheet' in sys.argv:
        sheet(names, os.environ.get('SHEET', '/tmp/biome_sheet.png'))
