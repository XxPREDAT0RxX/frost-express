"""Puts the Blender scenery renders (blender/scenery.py, release blender-N scenery_frames.zip) into the game.

    python3 store_assets/make_env_sprites.py <unzipped scenery folder> [name ...]

Each render <name>.png replaces app/src/main/res/drawable-nodpi/spr_<name>.webp (or <name>.webp for the
station_* sprites). store_assets/env_slots.json holds the canvas size and the drawn box of the art that was
there before, so the new sprite takes exactly the same place and size in the scene: it is scaled to fit
that box and centred on it. Station buildings fill their whole canvas.
"""
import json, os, sys
from PIL import Image

SRC = sys.argv[1]
ONLY = set(sys.argv[2:])
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res/drawable-nodpi')
SLOTS = json.load(open(os.path.join(ROOT, 'store_assets/env_slots.json')))

done = 0
for f in sorted(os.listdir(SRC)):
    if not f.endswith('.png'): continue
    name = f[:-4]
    if ONLY and name not in ONLY: continue
    key = name if name.startswith('station_') else 'spr_' + name
    slot = SLOTS.get(key)
    if slot is None:
        print('no slot for', name); continue
    im = Image.open(os.path.join(SRC, f)).convert('RGBA')
    im = im.crop(im.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox())
    W, H = slot['size']
    if name.startswith('station_t'):
        x0, y0, x1, y1 = 0, 0, W, H
    else:
        x0, y0, x1, y1 = slot['bbox']
    bw, bh = x1 - x0, y1 - y0
    k = min(bw / im.width, bh / im.height)
    nw, nh = max(1, round(im.width * k)), max(1, round(im.height * k))
    im = im.resize((nw, nh), Image.LANCZOS)
    out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    out.alpha_composite(im, (round((x0 + x1 - nw) / 2), round((y0 + y1 - nh) / 2)))
    out.save(os.path.join(RES, key + '.webp'), 'WEBP', quality=92, method=6, alpha_quality=100)
    done += 1
print('wrote', done)
