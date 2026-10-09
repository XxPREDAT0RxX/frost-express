"""A tiny top-down 3D renderer for game sprites.

Objects are built from simple solids (boxes, cylinders, domes, tubes, rings) as a height field.
The renderer adds materials, light from the top-left, specular highlights, ambient occlusion and
cast shadows (also onto the ground around the object), then saves an RGBA .webp sprite.

Units: every sprite has its own size in "units" (for train furniture 1 unit = 1 dp).
"""
import math
import numpy as np
from PIL import Image
from scipy.ndimage import gaussian_filter, distance_transform_edt, map_coordinates

LIGHT = np.array([-0.55, -0.65, 0.75])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
NEG = -1e9


def c(hexstr):
    hexstr = hexstr.lstrip('#')
    return tuple(int(hexstr[i:i + 2], 16) / 255 for i in (0, 2, 4))


class Sprite:
    def __init__(self, w, h, ppu=8, seed=1):
        self.w, self.h, self.ppu = w, h, ppu
        self.W, self.H = int(round(w * ppu)), int(round(h * ppu))
        ys, xs = np.mgrid[0:self.H, 0:self.W]
        self.x = (xs + 0.5) / ppu
        self.y = (ys + 0.5) / ppu
        self.Z = np.full((self.H, self.W), NEG)
        self.A = np.zeros((self.H, self.W, 3))
        self.S = np.zeros((self.H, self.W))      # specular strength
        self.E = np.zeros((self.H, self.W))      # emissive (unlit) amount
        self.rng = np.random.default_rng(seed)
        self._mats = {}

    # ---------------- materials ----------------
    def mat(self, name):
        if name in self._mats:
            return self._mats[name]
        H, W = self.H, self.W
        r = self.rng
        def nz(s):
            n = gaussian_filter(r.standard_normal((H, W)), s)
            n -= n.min(); return n / max(n.max(), 1e-9)
        if name == 'metal':
            v = 0.85 + 0.15 * nz((0.5, 10))
        elif name == 'metalv':
            v = 0.85 + 0.15 * nz((10, 0.5))
        elif name == 'wood':
            g = nz((0.8, 18))
            v = 0.72 + 0.28 * (0.5 + 0.5 * np.sin(self.y * 2.2 + g * 7))
        elif name == 'woodv':
            g = nz((18, 0.8))
            v = 0.72 + 0.28 * (0.5 + 0.5 * np.sin(self.x * 2.2 + g * 7))
        elif name == 'fabric':
            v = 0.86 + 0.08 * np.sign(np.sin(self.x * 9)) * np.sign(np.sin(self.y * 9)) + 0.06 * nz(2)
        elif name == 'rough':
            v = 0.65 + 0.35 * nz(1.2)
        elif name == 'rubber':
            v = 0.85 + 0.15 * nz(0.8)
        elif name == 'glass':
            v = np.ones((H, W))
        else:  # paint
            v = 0.93 + 0.07 * nz(3)
        self._mats[name] = v
        return v

    # ---------------- solids (each returns a height array, NEG outside) ----------------
    def _profile(self, inside_dist, z0, z1, bevel):
        t = np.clip(inside_dist / max(bevel, 1e-6), 0, 1)
        t = np.sin(t * math.pi / 2)
        return z0 + (z1 - z0) * t

    def box(self, x0, y0, x1, y1, z0, z1, r=1.0, bevel=1.0):
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        hx, hy = (x1 - x0) / 2 - r, (y1 - y0) / 2 - r
        qx = np.abs(self.x - cx) - hx
        qy = np.abs(self.y - cy) - hy
        d = np.hypot(np.maximum(qx, 0), np.maximum(qy, 0)) + np.minimum(np.maximum(qx, qy), 0) - r
        h = self._profile(-d, z0, z1, bevel)
        return np.where(d < 0, h, NEG)

    def cyl(self, cx, cy, r, z0, z1, bevel=0.8):
        d = np.hypot(self.x - cx, self.y - cy) - r
        return np.where(d < 0, self._profile(-d, z0, z1, bevel), NEG)

    def dome(self, cx, cy, r, z0, hh, ry=None):
        ry = ry or r
        q = ((self.x - cx) / r) ** 2 + ((self.y - cy) / ry) ** 2
        return np.where(q < 1, z0 + hh * np.sqrt(np.clip(1 - q, 0, 1)), NEG)

    def tube(self, x1, y1, x2, y2, w, z0, z1):
        px, py = self.x - x1, self.y - y1
        dx, dy = x2 - x1, y2 - y1
        L2 = dx * dx + dy * dy or 1e-9
        t = np.clip((px * dx + py * dy) / L2, 0, 1)
        d = np.hypot(px - t * dx, py - t * dy)
        rr = w / 2
        return np.where(d < rr, z0 + (z1 - z0) * np.sqrt(np.clip(1 - (d / rr) ** 2, 0, 1)), NEG)

    def ring(self, cx, cy, R, w, z0, z1):
        d = np.abs(np.hypot(self.x - cx, self.y - cy) - R)
        rr = w / 2
        return np.where(d < rr, z0 + (z1 - z0) * np.sqrt(np.clip(1 - (d / rr) ** 2, 0, 1)), NEG)

    def poly(self, pts, z0, z1, bevel=1.0):
        from PIL import ImageDraw
        m = Image.new('L', (self.W, self.H), 0)
        ImageDraw.Draw(m).polygon([(px * self.ppu, py * self.ppu) for px, py in pts], fill=255)
        inside = np.array(m) > 127
        dist = distance_transform_edt(inside) / self.ppu
        return np.where(inside, self._profile(dist, z0, z1, bevel), NEG)

    # ---------------- painting ----------------
    def add(self, h, col, mat='paint', spec=0.25, emit=0.0, grad=None):
        """Union: wherever this solid is higher than what is there, it becomes the visible surface."""
        m = h > self.Z
        self.Z = np.where(m, h, self.Z)
        colr = np.array(col, float)[None, None, :] * self.mat(mat)[..., None]
        if grad is not None:  # (col2, axis) simple two-colour gradient
            col2, axis = grad
            t = (self.y / self.h) if axis == 'y' else (self.x / self.w)
            colr = (np.array(col)[None, None, :] * (1 - t[..., None]) + np.array(col2)[None, None, :] * t[..., None]) * self.mat(mat)[..., None]
        self.A = np.where(m[..., None], colr, self.A)
        self.S = np.where(m, spec, self.S)
        self.E = np.where(m, emit, self.E)
        return m

    def carve(self, h):
        """Cut a hole/recess: lowers the surface to h where h is defined and lower."""
        m = (h > NEG / 2) & (self.Z > NEG / 2)
        self.Z = np.where(m, np.minimum(self.Z, h), self.Z)

    # ---------------- render ----------------
    def render(self, path, out_scale=0.5, ground_shadow=0.5, shadow=True, q=88):
        solid = self.Z > NEG / 2
        Z = np.where(solid, self.Z, 0.0) * self.ppu  # height in pixels
        gy, gx = np.gradient(gaussian_filter(Z, 0.7))
        n = np.stack([-gx, -gy, np.ones_like(Z)], -1)
        n /= np.linalg.norm(n, axis=-1, keepdims=True)
        diff = np.clip(n @ LIGHT, 0, 1)
        hv = LIGHT + np.array([0, 0, 1.0]); hv /= np.linalg.norm(hv)
        spec = np.clip(n @ hv, 0, 1) ** 36 * self.S
        # ambient occlusion: darker where the surroundings are higher
        blur = gaussian_filter(Z, 3 * self.ppu / 4)
        ao = np.clip(1 - (blur - Z) / (self.ppu * 6), 0.45, 1)
        # cast shadows by marching toward the light over the height field
        sh = np.zeros_like(Z)
        if shadow:
            lxy = LIGHT[:2] / np.linalg.norm(LIGHT[:2])
            tan = LIGHT[2] / np.linalg.norm(LIGHT[:2])
            ys, xs = np.mgrid[0:Z.shape[0], 0:Z.shape[1]].astype(float)
            maxz = Z.max()
            steps = int(min(maxz / tan + 2, 60 * self.ppu / 8))
            stride = max(1, steps // 40)
            for k in range(1, steps + 1, stride):
                sx = xs + lxy[0] * k; sy = ys + lxy[1] * k
                zs = map_coordinates(Z, [sy, sx], order=1, mode='constant', cval=0.0)
                sh = np.maximum(sh, (zs > Z + k * tan + 0.5).astype(float))
            sh = gaussian_filter(sh, self.ppu * 0.35)
        light = (0.38 + 0.72 * diff) * ao * (1 - 0.5 * sh)
        col = self.A * light[..., None] + spec[..., None] * 0.9
        col = col * (1 - self.E[..., None]) + self.A * self.E[..., None]
        col = np.clip(col, 0, 1)
        # alpha: the object itself, plus a soft shadow on whatever is underneath
        a_obj = solid.astype(float)
        ground = (1 - a_obj) * np.clip(sh, 0, 1) * ground_shadow
        alpha = np.clip(a_obj + ground, 0, 1)
        rgb = np.where(solid[..., None], col, np.zeros_like(col) + np.array([0.04, 0.06, 0.1]))
        img = Image.fromarray((np.dstack([rgb, alpha]) * 255).astype(np.uint8), 'RGBA')
        if out_scale != 1:
            img = img.resize((max(1, int(img.width * out_scale)), max(1, int(img.height * out_scale))), Image.LANCZOS)
        img.save(path, 'WEBP', quality=q, method=6)
        return img
