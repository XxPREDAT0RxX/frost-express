"""Play Store graphics, rendered with headless Chromium (Playwright) from the game's own art.

    python3 store_assets/make_store_assets.py <folder with CI screenshots (screens.zip unpacked)>

Writes store_assets/play/:
  icon-512.png               app icon (the launcher icon layers, full square)
  feature-graphic.png        1024 x 500 banner
  screenshot-1..8.png        1080 x 1920 phone screenshots with a caption on top
"""
import asyncio, base64, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
OUT = os.path.join(HERE, 'play')

SHOTS = [
    ('normal_30_int1.png', 'Run your own cozy passenger train', '#CDE7F8'),
    ('normal_32_st1.png', 'Grand stations full of travellers', '#FFE0B8'),
    ('normal_26_window.png', 'Watch the world roll by your window', '#CDEBDC'),
    ('normal_25_villagenight.png', 'Ride through villages and moonlit nights', '#C9C2F0'),
    ('normal_25_village.png', 'Seven lands, all alive in the wind', '#D8EFC4'),
    ('normal_14_evolve.png', 'Evolve into faster, finer trains', '#DCCBFA'),
    ('normal_16_journey.png', 'Travel country to country', '#FFE49A'),
    ('normal_3_upgrades.png', 'Upgrade every car and device', '#FFD3C4'),
]

FONT = "font-family:'Inter Display','Inter',sans-serif;"
INK = '#6B4A3A'


def data_uri(path):
    ext = os.path.splitext(path)[1][1:]
    mime = 'image/webp' if ext == 'webp' else 'image/png'
    return f'data:{mime};base64,' + base64.b64encode(open(path, 'rb').read()).decode()


def res(name):
    return data_uri(os.path.join(RES, name + '.webp'))


def page(w, h, body):
    return f'''<!doctype html><html><head><style>
html,body{{margin:0;width:{w}px;height:{h}px;overflow:hidden}}
.title{{{FONT}font-weight:900;color:#fff;-webkit-text-stroke:10px {INK};paint-order:stroke fill;letter-spacing:1px}}
</style></head><body>{body}</body></html>'''


def icon_html():
    return page(512, 512, f'''<div style="position:relative;width:512px;height:512px">
<img src="{res('ic_launcher_bg')}" style="position:absolute;inset:0;width:512px;height:512px">
<img src="{res('ic_launcher_fg')}" style="position:absolute;left:-40px;top:-48px;width:592px;height:592px"></div>''')


def npc_frame(name, anim='side', frame=2):
    """One frame of a Blender NPC strip as a data URI (frames sit side by side in the strip)."""
    import io, re
    from PIL import Image
    kt = open(os.path.join(HERE, '..', 'app', 'src', 'main', 'java', 'com', 'chetna', 'frostexpress', 'NpcAtlas.kt')).read()
    m = re.search(r'NpcStripInfo\("npc_%s_%s", (\d+), \d+, \d+, (\d+), (\d+)\)' % (name, anim), kt)
    n, w, h = int(m.group(1)), int(m.group(2)), int(m.group(3))
    im = Image.open(os.path.join(RES, 'npc_%s_%s.webp' % (name, anim))).convert('RGBA').crop((frame * w, 0, frame * w + w, h))
    buf = io.BytesIO(); im.save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode(), w, h


def feature_html():
    sprite = lambda n, x, y, s, extra='': f'<img src="{res(n)}" style="position:absolute;left:{x}px;top:{y}px;width:{s}px;{extra}">'
    walkers = ''
    for k, (n, x) in enumerate((('n10', 560), ('c1', 640), ('n12', 715), ('n07', 800), ('n13', 868))):
        uri, w, h = npc_frame(n, 'side', (k * 3) % 8)
        hh = 120 if n != 'n07' else 98
        walkers += f'<img src="{uri}" style="position:absolute;left:{x}px;top:{428 - hh}px;height:{hh}px">'
    return page(1024, 500, f'''<div style="position:relative;width:1024px;height:500px;overflow:hidden;
background:linear-gradient(180deg,#8FD0F0 0%,#D6F1FF 62%,#AFD98C 62%,#9CCB7A 100%)">
<div style="position:absolute;left:480px;top:250px;width:700px;height:120px;border-radius:50%;background:#8EC27A"></div>
{sprite('spr_tree_tall', 455, 150, 170)}{sprite('spr_v_house_a', 610, 200, 200)}{sprite('spr_tree_round', 820, 140, 200)}
{sprite('spr_bush', 930, 300, 100)}{sprite('spr_flowers', 520, 330, 80)}
<div style="position:absolute;left:520px;top:420px;width:520px;height:16px;border-radius:8px;background:#C9B596;box-shadow:0 4px 0 #A8926E"></div>
{walkers}
<img src="{res('ic_launcher_fg')}" style="position:absolute;left:-30px;top:240px;width:300px;filter:drop-shadow(0 8px 0 rgba(107,74,58,.25))">
<div class="title" style="position:absolute;left:52px;top:58px;font-size:96px;line-height:96px">Little<br>Express</div>
<div style="position:absolute;left:250px;top:390px;{FONT}font-weight:800;font-size:28px;color:{INK};
background:#FFF6E9;border:4px solid {INK};border-radius:22px;padding:8px 18px">A cozy train through seven lands</div>
</div>''')


def shot_html(img, caption, tint):
    return page(1080, 1920, f'''<div style="position:relative;width:1080px;height:1920px;overflow:hidden;
background:linear-gradient(180deg,{tint} 0%,#FFF6E9 70%)">
<div style="position:absolute;left:0;top:0;width:1080px;height:46px;background:
radial-gradient(circle at 40px 0,#fff 46px,transparent 47px) 0 0/80px 46px repeat-x"></div>
<div class="title" style="position:absolute;left:0;right:0;top:120px;text-align:center;font-size:84px;line-height:96px;padding:0 60px">{caption}</div>
<div style="position:absolute;left:190px;top:420px;width:700px;height:1517px;border-radius:56px;border:12px solid {INK};
overflow:hidden;background:#000;box-shadow:0 18px 0 rgba(107,74,58,.35)">
<img src="{data_uri(img)}" style="width:700px;height:1517px;object-fit:cover;object-position:top"></div>
</div>''')


async def main(shots_dir):
    from playwright.async_api import async_playwright
    os.makedirs(OUT, exist_ok=True)
    jobs = [('icon-512.png', 512, 512, icon_html()), ('feature-graphic.png', 1024, 500, feature_html())]
    for i, (f, cap, tint) in enumerate(SHOTS, start=1):
        p = os.path.join(shots_dir, f)
        if os.path.exists(p):
            jobs.append((f'screenshot-{i}.png', 1080, 1920, shot_html(p, cap, tint)))
    async with async_playwright() as pw:
        b = await pw.chromium.launch()
        pg = await b.new_page(device_scale_factor=1)
        for name, w, h, html in jobs:
            await pg.set_viewport_size({'width': w, 'height': h})
            await pg.set_content(html)
            await pg.wait_for_timeout(150)
            await pg.screenshot(path=os.path.join(OUT, name), clip={'x': 0, 'y': 0, 'width': w, 'height': h})
            print('wrote', name)
        await b.close()


if __name__ == '__main__':
    asyncio.run(main(sys.argv[1] if len(sys.argv) > 1 else '.'))
