"""Play Store graphics, rendered with headless Chromium (Playwright) from the game's own art.

    python3 store_assets/make_store_assets.py <folder with CI screenshots (screens.zip unpacked)>

Writes store_assets/play/:
  icon-512.png               app icon (the launcher icon layers, full square)
  feature-graphic.png        1024 x 500 banner
  screenshot-1..7.png        1080 x 1920 phone screenshots with a caption on top
"""
import asyncio, base64, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
OUT = os.path.join(HERE, 'play')

SHOTS = [
    ('normal_2_game.png', 'A cozy train in a frozen world', '#CDE7F8'),
    ('normal_8_night_station.png', 'Ride through snowy nights', '#C9C2F0'),
    ('normal_4_device.png', 'Install and upgrade 50+ devices', '#FFE0B8'),
    ('normal_3_upgrades.png', 'Grow your train car by car', '#CDEBDC'),
    ('normal_10_newtrain.png', 'Evolve through 25 trains', '#DCCBFA'),
    ('normal_7_quests.png', 'Daily quests and long journeys', '#FFE49A'),
    ('normal_11_shop.png', 'Boosters for a quick rush', '#FFD3C4'),
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


def feature_html():
    sprite = lambda n, x, y, s, extra='': f'<img src="{res(n)}" style="position:absolute;left:{x}px;top:{y}px;width:{s}px;{extra}">'
    return page(1024, 500, f'''<div style="position:relative;width:1024px;height:500px;overflow:hidden;
background:linear-gradient(180deg,#9FC9EA 0%,#CDE3F4 55%,#EEF5FB 100%)">
<div style="position:absolute;left:-60px;top:300px;width:1200px;height:300px;border-radius:50%;background:#FFFFFF;box-shadow:0 -6px 0 #C9DCEC inset"></div>
{sprite('spr_ruin_a', 600, 170, 190)}{sprite('spr_dead_a', 520, 210, 130)}{sprite('spr_frost_pine_a', 880, 170, 170)}
{sprite('spr_ice', 830, 290, 110)}{sprite('spr_frost_pine_b', 760, 230, 120)}{sprite('spr_lamp', 950, 260, 110)}
<img src="{res('ic_launcher_fg')}" style="position:absolute;left:580px;top:150px;width:340px;filter:drop-shadow(0 8px 0 rgba(107,74,58,.25))">
<div class="title" style="position:absolute;left:52px;top:118px;font-size:96px;line-height:96px">Frost<br>Express</div>
<div style="position:absolute;left:58px;top:345px;{FONT}font-weight:800;font-size:30px;color:{INK};
background:#FFF6E9;border:4px solid {INK};border-radius:22px;padding:8px 18px">The world froze. The train didn't.</div>
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
