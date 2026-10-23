"""Cuts the store trailer from the gameplay clips recorded by TrailerTest.

    python3 store_assets/make_trailer.py <app/build/trailer folder> <out folder>

Writes <out>/trailer-vertical.mp4 (1080 x 1920, for phones / Shorts) and <out>/trailer-landscape.mp4
(1920 x 1080, for the Play Store's YouTube link): a title card, the clips with short captions and
cross-fades, an end card, and the game's own music fading out.
"""
import os, re, subprocess, sys, shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter

SRC, OUT = sys.argv[1], sys.argv[2]
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'app', 'src', 'main', 'res')
os.makedirs(OUT, exist_ok=True)
FPS = 24
INK = (107, 74, 58)
CLIPS = [
    ('c1_interior', 'Run your own cozy train'),
    ('c2_station', 'Grand stations, busy platforms'),
    ('c3_village', 'Seven lands, alive in the wind'),
    ('c4_desert', 'From green fields to red deserts'),
    ('c5_night', 'Moonlit nights on the line'),
    ('c6_window', 'Watch the world roll by'),
    ('c7_window_snow', None),
    ('c8_zoomout', 'Build the train of your dreams'),
]

def font(size):
    for p in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', '/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf'):
        if os.path.exists(p): return ImageFont.truetype(p, size)
    return ImageFont.load_default()

def title(img, text, y, size):
    d = ImageDraw.Draw(img)
    f = font(size)
    w = d.textlength(text, font=f)
    d.text(((img.width - w) / 2, y), text, font=f, fill=(255, 255, 255), stroke_width=max(4, size // 9), stroke_fill=INK)

def frames(clip):
    fs = sorted(f for f in os.listdir(SRC) if re.match(re.escape(clip) + r'_\d{4}\.png$', f))
    return [os.path.join(SRC, f) for f in fs]

def fit_vertical(im):
    """The phone recording (1080 x 2340) cut to 1080 x 1920 around the middle of the action."""
    im = im.convert('RGB')
    if im.width != 1080: im = im.resize((1080, int(im.height * 1080 / im.width)))
    top = max(0, min(im.height - 1920, int(im.height * 0.07)))
    return im.crop((0, top, 1080, top + 1920))

BG = None
def landscape(v):
    """The vertical frame shown as a phone in the middle of a soft, blurred copy of itself."""
    global BG
    canvas = v.resize((1920, int(1920 * v.height / v.width))).crop((0, 400, 1920, 1480)).filter(ImageFilter.GaussianBlur(30))
    canvas = Image.blend(canvas, Image.new('RGB', canvas.size, (255, 246, 233)), 0.35)
    ph = v.resize((int(1000 * v.width / v.height), 1000))
    frame = Image.new('RGB', (ph.width + 24, ph.height + 24), INK)
    frame.paste(ph, (12, 12))
    canvas.paste(frame, ((1920 - frame.width) // 2, 28))
    return canvas

def card(lines, sub=None):
    im = Image.new('RGB', (1080, 1920), (205, 231, 248))
    g = Image.new('RGB', (1, 1920))
    for y in range(1920):
        t = y / 1919; g.putpixel((0, y), tuple(int(a + (b - a) * t) for a, b in zip((143, 208, 240), (255, 246, 233))))
    im = g.resize((1080, 1920))
    icon = None
    for p in ('drawable-nodpi/ic_launcher_fg.webp', 'drawable-nodpi/ic_launcher_fg.png'):
        if os.path.exists(os.path.join(RES, p)): icon = Image.open(os.path.join(RES, p)).convert('RGBA'); break
    if icon:
        icon = icon.resize((760, 760)); im.paste(icon, (160, 380), icon)
    y = 1180
    for ln in lines:
        title(im, ln, y, 150); y += 170
    if sub: title(im, sub, y + 40, 64)
    return im

tmp = os.path.join(OUT, 'frames'); shutil.rmtree(tmp, ignore_errors=True); os.makedirs(tmp)
seq = []
def add(img, n=1):
    for _ in range(n): seq.append(img)

intro = card(['Little', 'Express'], 'A cozy train through seven lands')
add(intro, int(FPS * 2.2))
prev_last = intro
for clip, cap in CLIPS:
    fs = frames(clip)
    if not fs: print('missing clip', clip); continue
    imgs = []
    for k, p in enumerate(fs):
        im = fit_vertical(Image.open(p))
        if cap:
            # caption fades in over the first 8 frames, out over the last 8
            a = min(1.0, k / 8, (len(fs) - 1 - k) / 8)
            if a > 0:
                lay = im.copy(); band = Image.new('RGB', (1080, 260), (255, 246, 233))
                lay.paste(Image.blend(lay.crop((0, 1540, 1080, 1800)), band, 0.55), (0, 1540))
                title(lay, cap, 1610, 76)
                im = Image.blend(im, lay, a)
        imgs.append(im)
    xf = 7   # cross-fade from the previous shot
    for k in range(min(xf, len(imgs))):
        imgs[k] = Image.blend(prev_last, imgs[k], (k + 1) / (xf + 1))
    for im in imgs: add(im)
    prev_last = imgs[-1]
outro = card(['Little', 'Express'], 'Coming soon on Google Play')
for k in range(8): add(Image.blend(prev_last, outro, (k + 1) / 9))
add(outro, int(FPS * 2.5))

for i, im in enumerate(seq):
    im.save(os.path.join(tmp, 'v%05d.png' % i))
    landscape(im).save(os.path.join(tmp, 'l%05d.png' % i))
dur = len(seq) / FPS
music = os.path.join(RES, 'raw', 'music_1.ogg')
for kind, size in (('v', 'vertical'), ('l', 'landscape')):
    cmd = ['ffmpeg', '-loglevel', 'error', '-y', '-framerate', str(FPS), '-i', os.path.join(tmp, kind + '%05d.png')]
    if os.path.exists(music):
        cmd += ['-stream_loop', '-1', '-i', music, '-af', 'afade=t=in:d=0.8,afade=t=out:st=%.2f:d=1.5' % (dur - 1.6), '-shortest', '-c:a', 'aac', '-b:a', '160k']
    cmd += ['-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', '-movflags', '+faststart', os.path.join(OUT, 'trailer-%s.mp4' % size)]
    subprocess.run(cmd, check=True)
shutil.rmtree(tmp)
print('trailer %.1f s' % dur)
