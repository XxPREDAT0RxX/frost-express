"""Puts the Blender scenery renders (blender/scenery.py, release blender-N scenery_frames.zip) into the game.

    python3 store_assets/make_env_sprites.py <unzipped scenery folder> [name ...]

Each render <name>.png replaces app/src/main/res/drawable-nodpi/spr_<name>.webp (or <name>.webp for the
station_* sprites). store_assets/env_slots.json holds the canvas size and the drawn box of the art that was
there before, so the new sprite takes exactly the same place and size in the scene: it is scaled to fit
that box and centred on it. Station buildings fill their whole canvas.
Animation frames <name>_NN.png (blender/scenery_anim.py) become one strip spra_<name>.webp: the frames side
by side, each the size of the item's canvas, all scaled and placed together so they line up.
"""
import re
import json, os, sys
from PIL import Image

SRC = sys.argv[1]
ONLY = set(sys.argv[2:])
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app/src/main/res/drawable-nodpi')
SLOTS = json.load(open(os.path.join(ROOT, 'store_assets/env_slots.json')))

def alpha_box(im):
    return im.getchannel('A').point(lambda a: 255 if a > 8 else 0).getbbox()

def place(ims, slot, fill_canvas=False):
    """Scales the images together (by the union of their drawn boxes) into the slot; returns canvases."""
    ub = None
    for im in ims:
        b = alpha_box(im)
        ub = b if ub is None else (min(ub[0], b[0]), min(ub[1], b[1]), max(ub[2], b[2]), max(ub[3], b[3]))
    W, H = slot['size']
    x0, y0, x1, y1 = (0, 0, W, H) if fill_canvas else slot['bbox']
    uw, uh = ub[2] - ub[0], ub[3] - ub[1]
    k = min((x1 - x0) / uw, (y1 - y0) / uh)
    outs = []
    for im in ims:
        c = im.crop(ub)
        nw, nh = max(1, round(c.width * k)), max(1, round(c.height * k))
        c = c.resize((nw, nh), Image.LANCZOS)
        out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        out.alpha_composite(c, (round((x0 + x1 - nw) / 2), round((y0 + y1 - nh) / 2)))
        outs.append(out)
    return outs

done = 0
frames = {}
for f in sorted(os.listdir(SRC)):
    m = re.match(r'(.+)_(\d\d)\.png$', f)
    if m and not f.startswith('station_'):
        frames.setdefault(m.group(1), []).append(f)
for name, fs in frames.items():
    if ONLY and name not in ONLY: continue
    slot = SLOTS.get('spr_' + name)
    if slot is None: print('no slot for', name); continue
    outs = place([Image.open(os.path.join(SRC, f)).convert('RGBA') for f in sorted(fs)], slot)
    W, H = slot['size']
    strip = Image.new('RGBA', (W * len(outs), H), (0, 0, 0, 0))
    for i, o in enumerate(outs): strip.paste(o, (i * W, 0))
    strip.save(os.path.join(RES, 'spra_' + name + '.webp'), 'WEBP', quality=90, method=6, alpha_quality=100)
    done += 1
for f in sorted(os.listdir(SRC)):
    if not f.endswith('.png') or re.match(r'.+_\d\d\.png$', f): continue
    name = f[:-4]
    if ONLY and name not in ONLY: continue
    key = name if name.startswith('station_') else 'spr_' + name
    slot = SLOTS.get(key)
    if slot is None:
        print('no slot for', name); continue
    out = place([Image.open(os.path.join(SRC, f)).convert('RGBA')], slot, fill_canvas=name.startswith('station_t'))[0]
    out.save(os.path.join(RES, key + '.webp'), 'WEBP', quality=92, method=6, alpha_quality=100)
    done += 1
print('wrote', done)
