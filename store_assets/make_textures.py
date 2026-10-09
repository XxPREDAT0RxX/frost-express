"""Generates the painted/realistic textures and sprites used by the train scene.

Run:  python3 store_assets/make_textures.py
Output: app/src/main/res/drawable-nodpi/*.webp  (tweak the numbers below and re-run)
"""
import os, math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from scipy.ndimage import gaussian_filter

OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)


def noise(h, w, sigma, wrap=True):
    n = rng.standard_normal((h, w))
    n = gaussian_filter(n, sigma, mode='wrap' if wrap else 'reflect')
    n -= n.min(); n /= max(n.max(), 1e-9)
    return n


def shade(height, strength=2.0, light=(-0.6, -0.8)):
    gy, gx = np.gradient(height)
    s = 0.5 + strength * (gx * light[0] + gy * light[1]) * height.shape[0] / 64
    return np.clip(s, 0, 1)


def lerp(a, b, t):
    a = np.array(a, float); b = np.array(b, float)
    return a + (b - a) * t[..., None]


def save(arr, name, alpha=None, q=82):
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), 'RGB')
    if alpha is not None:
        img.putalpha(Image.fromarray(np.clip(alpha * 255, 0, 255).astype(np.uint8)))
    img.save(os.path.join(OUT, name + '.webp'), 'WEBP', quality=q, method=6)
    print(name, img.size)


# ---------------- snow ground (512 px tile, drawn at 2 px per dp) ----------------
def snow():
    N = 512
    h = 0.55 * noise(N, N, 38) + 0.3 * noise(N, N, 12) + 0.15 * noise(N, N, 3)
    ripples = gaussian_filter(rng.standard_normal((N, N)), (1.6, 22), mode='wrap')
    ripples = (ripples - ripples.min()) / (ripples.max() - ripples.min())
    h = h + 0.08 * ripples
    s = shade(gaussian_filter(h, 1.5, mode='wrap'), 3.2)
    t = np.clip(0.25 + 0.75 * s, 0, 1)
    col = lerp((176, 198, 222), (250, 252, 255), t)
    col += (noise(N, N, 0.6) - 0.5)[..., None] * 10
    sp = rng.random((N, N)) > 0.9985
    col[sp] = (255, 255, 255)
    save(col, 'tex_snow', q=80)


# ---------------- track tile: ballast, two sleepers, two rails (66 x 28 dp at 4 px per dp) ----------------
def track():
    W, H = 264, 112
    base = 0.6 * noise(H, W, 2.5) + 0.4 * noise(H, W, 0.8)
    col = lerp((92, 96, 102), (168, 166, 160), base)
    img = Image.fromarray(col.astype(np.uint8), 'RGB')
    d = ImageDraw.Draw(img)
    for _ in range(900):
        x = rng.uniform(0, W); y = rng.uniform(0, H); r = rng.uniform(2.5, 6.5)
        g = rng.uniform(95, 185); tint = rng.uniform(-12, 12)
        c = (int(g + tint), int(g), int(g - tint))
        for dy in (-H, 0, H):
            d.ellipse([x - r, y + dy - r * 0.8, x + r, y + dy + r * 0.8], fill=(int(c[0] * .55), int(c[1] * .55), int(c[2] * .55)))
            d.ellipse([x - r * .85, y + dy - r * .75, x + r * .75, y + dy + r * .6], fill=c)
            d.ellipse([x - r * .5, y + dy - r * .6, x + r * .1, y + dy - r * .05], fill=(min(255, c[0] + 40), min(255, c[1] + 40), min(255, c[2] + 40)))
    img = img.filter(ImageFilter.GaussianBlur(0.6))
    d = ImageDraw.Draw(img)
    # sleepers
    for sy in (14, 70):
        sx0, sx1, sh = 26, 238, 26
        d.rectangle([sx0 + 4, sy + 5, sx1 + 4, sy + sh + 5], fill=(45, 42, 40))
        wood = 0.6 * gaussian_filter(rng.standard_normal((sh, sx1 - sx0)), (0.6, 14)) + 0.4 * rng.standard_normal((sh, sx1 - sx0)) * 0.3
        wood = (wood - wood.min()) / (wood.max() - wood.min())
        wc = lerp((70, 52, 38), (128, 100, 74), wood)
        wc[:, :6] *= 0.75; wc[:, -6:] *= 0.75; wc[:3, :] *= 1.12; wc[-3:, :] *= 0.8
        img.paste(Image.fromarray(np.clip(wc, 0, 255).astype(np.uint8)), (sx0, sy))
        for cx in (70, 182):
            d.rectangle([cx - 14, sy + 3, cx + 14, sy + sh - 3], fill=(58, 60, 64))
            for bx in (cx - 10, cx + 10):
                d.ellipse([bx - 2.5, sy + sh / 2 - 2.5, bx + 2.5, sy + sh / 2 + 2.5], fill=(150, 150, 150))
    # rails
    for rx in (68, 180):
        d.rectangle([rx + 5, 0, rx + 21, H], fill=(40, 40, 42))
        d.rectangle([rx, 0, rx + 16, H], fill=(78, 70, 64))
        d.rectangle([rx + 2, 0, rx + 14, H], fill=(112, 96, 84))
        d.rectangle([rx + 5, 0, rx + 11, H], fill=(214, 220, 226))
        d.rectangle([rx + 6, 0, rx + 8, H], fill=(245, 248, 250))
    arr = np.array(img).astype(float)
    x = np.arange(W)
    alpha = np.clip(np.minimum(x, W - 1 - x) / 10.0, 0, 1)[None, :].repeat(H, 0)
    save(arr, 'tex_track', alpha=alpha, q=85)


# ---------------- wooden floor planks (256 x 240, 4 px per dp) ----------------
def wood():
    W, H, P = 256, 240, 48
    out = np.zeros((H, W, 3))
    for row in range(H // P):
        grain = gaussian_filter(rng.standard_normal((P, W)), (0.7, 26), mode='wrap')
        grain = (grain - grain.min()) / (grain.max() - grain.min())
        rings = 0.5 + 0.5 * np.sin(np.linspace(0, rng.uniform(10, 18), W)[None, :] + grain * 9)
        t = 0.55 * grain + 0.45 * rings
        tone = rng.uniform(-14, 14)
        c = lerp((150 + tone, 112 + tone, 76 + tone), (205 + tone, 166 + tone, 120 + tone), t)
        c[:3] *= 1.08; c[-4:] *= 0.62; c[-2:] *= 0.5
        joint = int(rng.uniform(20, W - 20))
        c[:, joint:joint + 2] *= 0.55
        for nx in (joint - 8, joint + 9):
            for ny in (10, P - 14):
                c[ny:ny + 3, nx % W:nx % W + 3] = (70, 62, 56)
        out[row * P:(row + 1) * P] = c
    out += (noise(H, W, 0.7) - 0.5)[..., None] * 12
    save(out, 'tex_wood')


# ---------------- tread plate for the engine car (128 px, 4 px per dp) ----------------
def metal():
    N = 128
    base = 0.6 * noise(N, N, 6) + 0.4 * noise(N, N, 1)
    c = lerp((112, 120, 128), (150, 158, 166), base)
    img = Image.fromarray(c.astype(np.uint8)); d = ImageDraw.Draw(img)
    for gy in range(0, N, 16):
        for gx in range(0, N, 16):
            ox = 8 if (gy // 16) % 2 else 0
            for dx in (-N, 0, N):
                x = gx + ox + dx; y = gy
                d.line([x + 2, y + 10, x + 10, y + 3], fill=(196, 202, 208), width=3)
                d.line([x + 3, y + 11, x + 11, y + 4], fill=(70, 76, 82), width=1)
    arr = np.array(img).astype(float) + (noise(N, N, 0.6) - 0.5)[..., None] * 14
    save(arr, 'tex_metal')


# ---------------- grime overlay for car bodies (multiply) ----------------
def grime():
    N = 256
    v = 0.6 * noise(N, N, 20) + 0.4 * noise(N, N, 2)
    t = np.clip(0.8 + 0.25 * v, 0, 1)
    img = Image.fromarray((np.stack([t, t, t], -1) * 255).astype(np.uint8)); d = ImageDraw.Draw(img)
    for _ in range(40):
        x = rng.uniform(0, N); y = rng.uniform(0, N); l = rng.uniform(6, 24); a = rng.uniform(0, math.pi)
        g = int(rng.uniform(170, 215))
        d.line([x, y, x + math.cos(a) * l, y + math.sin(a) * l], fill=(g, g, g), width=1)
    save(np.array(img).astype(float), 'tex_grime')


# ---------------- concrete for station platforms ----------------
def concrete():
    N = 128
    v = 0.5 * noise(N, N, 8) + 0.5 * noise(N, N, 0.8)
    c = lerp((168, 172, 176), (214, 218, 222), v)
    sp = rng.random((N, N)) > 0.985
    c[sp] *= 0.75
    c[::32, :] *= 0.8
    save(c, 'tex_concrete')


# ---------------- sprites (rendered at 2x then downscaled) ----------------
def sprite_canvas(n):
    return Image.new('RGBA', (n, n), (0, 0, 0, 0))


def shadow_layer(n, cx, cy, rx, ry, a=110, blur=None):
    sh = Image.new('L', (n, n), 0)
    ImageDraw.Draw(sh).ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur or rx * 0.18))
    layer = Image.new('RGBA', (n, n), (20, 30, 48, 0)); layer.putalpha(sh)
    return layer


def pine(name, seed):
    r2 = np.random.default_rng(seed)
    n = 512; c = n / 2; R = n * 0.36
    img = sprite_canvas(n)
    img.alpha_composite(shadow_layer(n, c + R * 0.28, c + R * 0.32, R * 0.95, R * 0.85))
    d = ImageDraw.Draw(img)
    layers = 8
    for li in range(layers):
        lr = R * (1 - li / (layers + 0.4))
        t = li / (layers - 1)
        base = np.array([22, 52, 44]) * (1 - t) + np.array([70, 118, 96]) * t
        branches = int(34 - li * 2)
        for b in range(branches):
            a = 2 * math.pi * b / branches + r2.uniform(-.12, .12) + li * 0.37
            tip = lr * r2.uniform(0.82, 1.0)
            lit = 1 + 0.28 * (-(math.cos(a) * 0.6 + math.sin(a) * 0.8))
            # a branch: a spine with short needles along it
            steps = 14
            for k in range(steps):
                f = 0.35 + 0.65 * k / steps
                bx = c + math.cos(a) * tip * f; by = c + math.sin(a) * tip * f
                nl = (1 - f) * 22 + 8
                for side in (-1, 1):
                    na = a + side * r2.uniform(0.5, 0.9)
                    kk = r2.uniform(0.8, 1.15)
                    col = tuple(int(min(255, v * kk * lit)) for v in base) + (255,)
                    d.line([bx, by, bx + math.cos(na) * nl, by + math.sin(na) * nl], fill=col, width=3)
            d.line([c, c, c + math.cos(a) * tip, c + math.sin(a) * tip], fill=tuple(int(v * 0.7) for v in base) + (255,), width=3)
        # a dusting of snow on the upper layers
        if li >= 3:
            for _ in range(int(6 + li * 2)):
                a = r2.uniform(0, 2 * math.pi); rr = lr * r2.uniform(0.35, 0.85)
                x = c + math.cos(a) * rr; y = c + math.sin(a) * rr; s = r2.uniform(4, 8)
                d.ellipse([x - s * 1.3 + 2, y - s + 3, x + s * 1.3 + 2, y + s + 3], fill=(150, 175, 200, 160))
                d.ellipse([x - s * 1.3, y - s, x + s * 1.3, y + s], fill=(242, 247, 252, 235))
    d.ellipse([c - 10, c - 10, c + 10, c + 10], fill=(236, 243, 248, 255))
    img = img.filter(ImageFilter.GaussianBlur(0.8)).resize((256, 256), Image.LANCZOS)
    arr = np.array(img).astype(float)
    save(arr[..., :3], name, alpha=arr[..., 3] / 255, q=88)


def rock(name, seed):
    r2 = np.random.default_rng(seed)
    n = 384; c = n / 2
    yy, xx = np.mgrid[0:n, 0:n]
    ang = np.arctan2(yy - c, xx - c)
    radius = n * 0.3 * (1 + 0.18 * np.sin(ang * 3 + r2.uniform(0, 6)) + 0.1 * np.sin(ang * 5 + r2.uniform(0, 6)))
    dist = np.hypot(xx - c, yy - c)
    inside = dist < radius
    hgt = np.clip(1 - dist / radius, 0, 1) ** 0.6 + 0.15 * gaussian_filter(r2.standard_normal((n, n)), 6) + 0.05 * gaussian_filter(r2.standard_normal((n, n)), 1.5)
    s = shade(gaussian_filter(hgt, 2), 6)
    col = lerp((62, 68, 78), (176, 182, 190), s)
    col *= (0.9 + 0.2 * gaussian_filter(r2.random((n, n)), 1))[..., None]
    snowmask = (hgt > 0.45) & (s > 0.45)
    snowmask = gaussian_filter(snowmask.astype(float), 3) > 0.5
    col[snowmask] = lerp((205, 220, 235), (252, 253, 255), s[snowmask])
    alpha = gaussian_filter(inside.astype(float), 1.2)
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8)).convert('RGBA'); img.putalpha(Image.fromarray((alpha * 255).astype(np.uint8)))
    out = sprite_canvas(n)
    out.alpha_composite(shadow_layer(n, c + n * 0.08, c + n * 0.1, n * 0.32, n * 0.26, 120))
    out.alpha_composite(img)
    out = out.resize((192, 192), Image.LANCZOS)
    arr = np.array(out).astype(float)
    save(arr[..., :3], name, alpha=arr[..., 3] / 255, q=88)


def shrub(name, seed):
    r2 = np.random.default_rng(seed)
    n = 320; c = n / 2
    img = sprite_canvas(n)
    img.alpha_composite(shadow_layer(n, c + 18, c + 22, n * 0.3, n * 0.25))
    d = ImageDraw.Draw(img)
    for _ in range(60):  # twigs
        a = r2.uniform(0, 2 * math.pi); l = r2.uniform(40, 110)
        d.line([c, c, c + math.cos(a) * l, c + math.sin(a) * l], fill=(88, 64, 48, 255), width=3)
    for _ in range(220):
        a = r2.uniform(0, 2 * math.pi); rr = r2.uniform(0, 100) ** 0.9
        x = c + math.cos(a) * rr; y = c + math.sin(a) * rr; s = r2.uniform(8, 18)
        g = r2.uniform(0.75, 1.25); lit = 1 + 0.2 * (-(math.cos(a) * .6 + math.sin(a) * .8))
        d.ellipse([x - s, y - s, x + s, y + s], fill=(int(52 * g * lit), int(86 * g * lit), int(66 * g * lit), 255))
    for _ in range(26):
        a = r2.uniform(0, 2 * math.pi); rr = r2.uniform(10, 80)
        x = c + math.cos(a) * rr - 10; y = c + math.sin(a) * rr - 12; s = r2.uniform(6, 13)
        d.ellipse([x - s, y - s, x + s, y + s], fill=(246, 250, 253, 255))
    img = img.filter(ImageFilter.GaussianBlur(0.7)).resize((160, 160), Image.LANCZOS)
    arr = np.array(img).astype(float)
    save(arr[..., :3], name, alpha=arr[..., 3] / 255, q=88)


if __name__ == '__main__':
    snow(); track(); wood(); metal(); grime(); concrete()
    pine('spr_pine_a', 1); pine('spr_pine_b', 2); pine('spr_pine_c', 3)
    rock('spr_rock_a', 4); rock('spr_rock_b', 5)
    shrub('spr_shrub', 6)
