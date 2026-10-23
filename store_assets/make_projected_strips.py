"""Packs the painted-3D animations (blender/projected.py, release blender-N projected_frames.zip) into the game.

    python3 store_assets/make_projected_strips.py <unzipped projected folder> [<more folders>...]

Frames <sprite>_NN.png (already the painted sprite's own canvas size) become one strip
app/src/main/res/drawable-nodpi/spra_<game name>.webp, frames side by side. Game names are the scenery
names TrainScene uses (some differ from the sprite file, e.g. "pine_a" is drawn with spr_frost_pine_a).
"""
import os, re, sys
from PIL import Image

SRCS = sys.argv[1:]
RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'app/src/main/res/drawable-nodpi')
# game name -> rendered sprite
GAME = {
    'tree_round': 'tree_round', 'tree_tall': 'tree_tall', 'acacia': 'acacia', 'palm': 'palm', 'jungle': 'jungle', 'fern': 'fern',
    'bush': 'bush', 'flowers': 'flowers', 'grass_tuft': 'grass_tuft', 'xmas': 'xmas', 'snow_bush': 'snow_bush',
    'dead_a': 'dead_a', 'dead_b': 'dead_b', 'pine_a': 'frost_pine_a', 'pine_b': 'frost_pine_b', 'pine_c': 'frost_pine_c',
    'shrub': 'frost_shrub', 'v_palm': 'v_palm', 'v_banana': 'v_banana', 'v_bamboo': 'v_bamboo', 'v_bush': 'v_vbush',
    'v_grass': 'v_vgrass', 'campfire': 'campfire', 'turbine': 'turbine',
    'v_house_a': 'v_house_a', 'v_well': 'v_well', 'tent': 'tent', 'lighthouse': 'lighthouse', 'ship': 'ship', 'buoy': 'buoy',
    'snowman': 'snowman', 'crane': 'crane', 'v_paddy': 'v_paddy', 'v_bed': 'v_bed', 'v_haystack': 'v_haystack', 'lamp': 'lamp',
}
frames = {}
for SRC in SRCS:
    for f in sorted(os.listdir(SRC)):
        m = re.match(r'(.+)_(\d\d)\.png$', f)
        if m: frames.setdefault(m.group(1), []).append(os.path.join(SRC, f))
for old in os.listdir(RES):
    if old.startswith('spra_'): os.remove(os.path.join(RES, old))
n = 0
for game, spr in GAME.items():
    fs = frames.get(spr)
    if not fs: print('missing', spr); continue
    ims = [Image.open(f).convert('RGBA') for f in sorted(fs)]
    W, H = ims[0].size
    strip = Image.new('RGBA', (W * len(ims), H), (0, 0, 0, 0))
    for i, im in enumerate(ims): strip.paste(im, (i * W, 0))
    strip.save(os.path.join(RES, 'spra_%s.webp' % game), 'WEBP', quality=90, method=6, alpha_quality=100)
    n += 1
print('wrote', n)
