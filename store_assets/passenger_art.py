"""Warm-weather passengers: the same cosy chibi characters but in summer clothes (tees, caps, sun hats),
used in the hot biomes while the winter survivors ride through the cold ones.

    python3 store_assets/passenger_art.py
"""
import asyncio, io, os, sys
from PIL import Image
sys.path.insert(0, os.path.dirname(__file__))
import cozy_art as C
from cozy_art import OL, o

OUT = C.OUT
chibi = C.chibi
ART = {}

def cap(col, cd):
    return f'<path d="M5.4,11 C5,3 27,3 26.6,11 C22,8 10,8 5.4,11 Z" fill="{col}" {o(1.5)}/><path d="M3,11.5 C8,9.5 16,9.6 17,11 C12,11.4 7,11.6 3,11.5 Z" fill="{cd}" {o(1.2)}/>'
def sunhat(col, cd):
    return f'<ellipse cx="16" cy="11" rx="15" ry="4.4" fill="{col}" {o(1.4)}/><path d="M8,11 C8,4 24,4 24,11 Z" fill="{cd}" {o(1.3)}/><path d="M8,9.4 L24,9.4" stroke="{col}" stroke-width="1.6"/>'
def bandana(col):
    return f'<path d="M5.6,11 C5,5 27,5 26.4,11 C22,8.6 10,8.6 5.6,11 Z" fill="{col}" {o(1.4)}/><circle cx="7" cy="8.6" r=".7" fill="#fff"/><circle cx="12" cy="7.6" r=".7" fill="#fff"/><circle cx="20" cy="7.6" r=".7" fill="#fff"/><circle cx="25" cy="8.6" r=".7" fill="#fff"/>'
def shades(x=0):
    return f'<rect x="8" y="14.6" width="7" height="4" rx="2" fill="#2B2F36"/><rect x="17" y="14.6" width="7" height="4" rx="2" fill="#2B2F36"/><path d="M15,16 L17,16" stroke="#2B2F36" stroke-width="1.4"/>'
def hair(col, braids=False):
    if braids:
        return f'<path d="M5,12 C2,20 3,28 7,30 L9,19 Z M27,12 C30,20 29,28 25,30 L23,19 Z" fill="{col}" {o(1.1)}/>'
    return f'<path d="M5.4,13 C5,6 27,6 26.6,13 C22,9 10,9 5.4,13 Z" fill="{col}" {o(1.2)}/>'

# tee colour, tee dark, headwear, hair, extra, skin
SUM = [
    chibi('#F2C14E', '#D9A636', cap('#E5484D', '#C23B40')),                                  # red cap, yellow tee
    chibi('#2FA5A0', '#1F8A86', sunhat('#E8C98A', '#D4AF6A'), skin='#E9B48F'),                # straw sun hat, teal tank
    chibi('#F28C4A', '#D9733A', hair('#4A3A34'), extra=shades()),                             # sunglasses, orange tee
    chibi('#FFFFFF', '#D7DEE6', bandana('#4C8DB8')),                                          # blue bandana, white tee
    chibi('#F49AB2', '#E07A96', cap('#5BBE7A', '#49A063')),                                   # green cap, pink tee
    chibi('#A7D3F0', '#8ABBE0', sunhat('#FFFFFF', '#E6ECF0'), hair('#6B4A3A', braids=True)),   # floppy hat, light-blue
    chibi('#E5484D', '#C23B40', f'<path d="M5.6,12 C6,9 26,9 26.4,12" fill="none" stroke="#F2C14E" stroke-width="2.4" stroke-linecap="round"/>', hair('#3A2E28')),  # headband
    chibi('#5BBE7A', '#49A063', hair('#8B5E3C')),                                             # no hat, green polo
]
for i, svg in enumerate(SUM):
    ART[f'npc_sum{i+1}'] = (32, 40, svg, 6)


async def render():
    from playwright.async_api import async_playwright
    async with async_playwright() as p:
        b = await p.chromium.launch()
        pg = await b.new_page(device_scale_factor=1)
        for name, (w, h, body, sc) in ART.items():
            pw, ph = int(w*sc), int(h*sc)
            await pg.set_viewport_size({'width': pw, 'height': ph})
            await pg.set_content(C.svg(w, h, body, pw, ph))
            png = await pg.screenshot(omit_background=True, clip={'x':0,'y':0,'width':pw,'height':ph})
            Image.open(io.BytesIO(png)).convert('RGBA').save(os.path.join(OUT, name+'.webp'), 'WEBP', quality=92, method=6)
            print('rendered', name)
        await b.close()

if __name__ == '__main__':
    asyncio.run(render())
    if '--sheet' in sys.argv:
        ims=[Image.open(os.path.join(OUT,f'npc_sum{i+1}.webp')).convert('RGBA') for i in range(len(SUM))]
        cell=160; out=Image.new('RGBA',(cell*4,cell*2),(220,235,245,255))
        for k,im in enumerate(ims):
            im.thumbnail((cell-8,cell-8)); out.alpha_composite(im,(k%4*cell+4,k//4*cell+4))
        out.save(os.environ.get('SHEET','/tmp/sum.png')); print('sheet')
