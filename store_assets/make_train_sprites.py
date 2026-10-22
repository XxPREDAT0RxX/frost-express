"""Puts the Blender train renders (blender/train.py, release blender-N train_frames.zip) into the game.

    python3 store_assets/make_train_sprites.py <unzipped train folder>

The renders already have the exact frame the game draws them in, so each <name>.png just becomes
app/src/main/res/drawable-nodpi/<name>.webp (nose*_fill/line, car_fill, car_line, car_roof).
"""
import os, sys
from PIL import Image

SRC = sys.argv[1]
RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'app/src/main/res/drawable-nodpi')
n = 0
for f in sorted(os.listdir(SRC)):
    if not f.endswith('.png'): continue
    Image.open(os.path.join(SRC, f)).convert('RGBA').save(os.path.join(RES, f[:-4] + '.webp'), 'WEBP', quality=92, method=6, alpha_quality=100)
    n += 1
print('wrote', n)
