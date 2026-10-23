"""Little Express: the painted scenery art put onto 3D shapes and animated (camera projection).

    blender -b -P blender/projected.py -- <out_dir> [--only tree_round,turbine]

Each painted sprite (blender/painted/<name>.png) becomes a 3D shape: a fine mesh cut to the picture's outline
and puffed out toward the camera (thickest in the middle, like a pillow). The picture is projected onto it
from the game's camera, so the still frame looks exactly like the painting. Then the shape is animated in 3D:
plants bend in the wind with fluttering leaves (and catch a little more or less light as they turn),
the wind turbine's blades turn round the hub. Frames go to <out_dir>/<name>_NN.png at the sprite's own size.
"""
import bpy, bmesh, math, os, sys
import numpy as np
from mathutils import Vector, Matrix

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = next((a for i, a in enumerate(ARGS) if not a.startswith('--') and (i == 0 or ARGS[i - 1] != '--only')), '/tmp/projected')
os.makedirs(OUT, exist_ok=True)
ONLY = ARGS[ARGS.index('--only') + 1].split(',') if '--only' in ARGS else None
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'painted')

bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'; sc.cycles.device = 'CPU'; sc.cycles.samples = 6
sc.cycles.use_denoising = False; sc.cycles.max_bounces = 0
sc.render.film_transparent = True; sc.render.filter_size = 0.8
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'; sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world; world.use_nodes = True
world.node_tree.nodes['Background'].inputs[1].default_value = 0.0
camd = bpy.data.cameras.new('cam'); camd.type = 'ORTHO'
CAM = bpy.data.objects.new('cam', camd); sc.collection.objects.link(CAM); sc.camera = CAM

PX = 0.01                                   # world units per pixel of the sprite
LIGHT = Vector((-0.6, -0.5, 0.62)).normalized()
CUR = []

def load_image(name):
    img = bpy.data.images.load(os.path.join(SRC, name + '.png'))
    img.colorspace_settings.name = 'sRGB'
    w, h = img.size
    a = np.array(img.pixels[:], dtype=np.float32).reshape(h, w, 4)[::-1]   # row 0 = top
    return img, a

def material(img):
    """The picture as an unlit surface, multiplied by a per-vertex 'shade' (1 = as painted)."""
    m = bpy.data.materials.new('p'); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    tx = nt.nodes.new('ShaderNodeTexImage'); tx.image = img; tx.interpolation = 'Linear'; tx.extension = 'CLIP'
    at = nt.nodes.new('ShaderNodeAttribute'); at.attribute_name = 'shade'; at.attribute_type = 'GEOMETRY'
    mul = nt.nodes.new('ShaderNodeMix'); mul.data_type = 'RGBA'; mul.blend_type = 'MULTIPLY'; mul.inputs[0].default_value = 1
    L(tx.outputs['Color'], mul.inputs[6]); L(at.outputs['Color'], mul.inputs[7])
    em = nt.nodes.new('ShaderNodeEmission'); L(mul.outputs[2], em.inputs['Color'])
    tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader')
    L(tx.outputs['Alpha'], mx.inputs[0]); L(tr.outputs[0], mx.inputs[1]); L(em.outputs[0], mx.inputs[2])
    L(mx.outputs[0], out.inputs['Surface'])
    return m

def distance_inside(mask, steps):
    """How far (in grid steps, up to [steps]) each cell is from the outline: a cheap distance transform."""
    d = np.zeros(mask.shape, np.float32); cur = mask.copy()
    for k in range(steps):
        d += cur
        n = cur.copy()
        n[1:, :] &= cur[:-1, :]; n[:-1, :] &= cur[1:, :]; n[:, 1:] &= cur[:, :-1]; n[:, :-1] &= cur[:, 1:]
        cur = n
        if not cur.any(): break
    return d

def card(name, depth=0.35, res=96, origin=None, z_order=0.0):
    """The painted picture as a puffed-out 3D shape. Returns (object, image size, info about the shape)."""
    img, a = load_image(name)
    H, W = a.shape[:2]
    alpha = a[..., 3]
    ys, xs = np.where(alpha > 0.02)
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    step = max(x1 - x0, y1 - y0) / res
    nx = int(math.ceil((x1 - x0) / step)) + 1; ny = int(math.ceil((y1 - y0) / step)) + 1
    gx = x0 + np.arange(nx) * step; gy = y0 + np.arange(ny) * step
    samp = alpha[np.clip(gy.astype(int), 0, H - 1)][:, np.clip(gx.astype(int), 0, W - 1)]
    inside = samp > 0.02
    dist = distance_inside(inside, 40)
    dn = dist / max(1.0, dist.max())
    bm = bmesh.new(); uv = bm.loops.layers.uv.new('UVMap')
    vid = {}
    for j in range(ny):
        for i in range(nx):
            # keep the vertex if any neighbouring cell has paint, so the outline stays whole
            if not inside[max(0, j - 1):j + 2, max(0, i - 1):i + 2].any(): continue
            x = gx[i] - W / 2; z = H - gy[j]
            vid[(i, j)] = bm.verts.new((x * PX, -depth * math.sqrt(dn[j, i]) * (x1 - x0) * PX * 0.5 - z_order, z * PX))
    for j in range(ny - 1):
        for i in range(nx - 1):
            q = [(i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)]
            if all(k in vid for k in q):
                f = bm.faces.new([vid[k] for k in q])
                for loop, (ii, jj) in zip(f.loops, q):
                    loop[uv].uv = (gx[ii] / W, 1 - gy[jj] / H)
    me = bpy.data.meshes.new(name); bm.to_mesh(me); bm.free()
    for p in me.polygons: p.use_smooth = True
    o = bpy.data.objects.new(name, me); sc.collection.objects.link(o)
    me.materials.append(material(img))
    me.color_attributes.new('shade', 'FLOAT_COLOR', 'POINT')
    for c in me.color_attributes['shade'].data: c.color = (1, 1, 1, 1)
    CUR.append(o)
    zs = [v.co.z for v in me.vertices]
    return o, (W, H), dict(base=min(zs), top=max(zs))

def set_camera(W, H):
    camd.ortho_scale = max(W, H) * PX
    CAM.location = (0, -50, H / 2 * PX); CAM.rotation_euler = (math.radians(90), 0, 0)
    sc.render.resolution_x, sc.render.resolution_y = W, H

def shot(name):
    sc.render.filepath = os.path.join(OUT, name + '.png')
    bpy.ops.render.render(write_still=True)

def clear():
    for o in CUR: bpy.data.objects.remove(o, do_unlink=True)
    CUR.clear()

def shade_from_normals(o, rest_normals, k=0.9):
    """Faces turning toward the light get a little brighter, away from it a little darker."""
    me = o.data
    sh = me.color_attributes['shade'].data
    for v, n0, c in zip(me.vertices, rest_normals, sh):
        d = 1 + k * (v.normal.dot(LIGHT) - n0.dot(LIGHT))
        c.color = (d, d, d, 1)

# ------------------------------------------------------------------ animations
def plant(name, amp, flutter=0.35, frames=12, stiff=1.7, depth=0.35, swirl=0.5):
    """Bends the shape in the wind: the base stays, the top swings sideways and a little toward and away from us;
    leaves flutter on their own. [amp] is the sway at the top as a fraction of the height."""
    o, (W, H), info = card(name, depth=depth)
    set_camera(W, H)
    me = o.data
    rest = [v.co.copy() for v in me.vertices]
    me.update(); rest_n = [v.normal.copy() for v in me.vertices]
    hgt = info['top'] - info['base']
    for f in range(frames):
        ph = 2 * math.pi * f / frames
        for v, r in zip(me.vertices, rest):
            u = max(0.0, (r.z - info['base']) / hgt) ** stiff
            fl = flutter * math.sin(ph * 2 + r.x * 9 + r.z * 7) * u
            v.co = r + Vector((amp * hgt * u * (math.sin(ph) + 0.35 * fl),
                               amp * hgt * swirl * u * math.cos(ph),
                               -abs(amp * hgt * u * math.sin(ph)) * 0.25 + 0.04 * amp * hgt * fl))
        me.update()
        shade_from_normals(o, rest_n)
        shot('%s_%02d' % (name, f))
    print('done', name, flush=True)
    clear()

def turbine(frames=12):
    HX, HY = 131, 97   # hub in the painted picture (px)
    tower, (W, H), _ = card('turbine_tower', depth=0.25)
    blade_cards = []
    for k in range(3):
        b, _, _ = card('turbine_blade', depth=0.15, z_order=0.2 + 0.002 * k)
        blade_cards.append(b)
    hub, _, _ = card('turbine_hub', depth=0.3, z_order=0.3)
    set_camera(W, H)
    pivot = Vector(((HX - W / 2) * PX, 0, (H - HY) * PX))
    for f in range(frames):
        a = 2 * math.pi / 3 * f / frames         # 120 degrees per loop: three blades look alike
        for k, b in enumerate(blade_cards):
            ang = a + k * 2 * math.pi / 3
            b.matrix_world = Matrix.Translation(pivot) @ Matrix.Rotation(-ang, 4, 'Y') @ Matrix.Translation(-pivot)
        shot('turbine_%02d' % f)
    print('done turbine', flush=True)
    clear()

def campfire(frames=12):
    """The flames lick upward and flicker; the logs and sticks stay put."""
    o, (W, H), info = card('campfire', depth=0.2, res=110)
    set_camera(W, H)
    _, a = load_image('campfire')
    r, g, b, al = a[..., 0], a[..., 1], a[..., 2], a[..., 3]
    flame = ((r > 0.75) & (g > 0.55) & (b < 0.62) & (r - b > 0.3) & (al > 0.5)).astype(np.float32)
    for _ in range(6):   # soften the mask so the edge of the fire moves smoothly
        flame = (flame + np.roll(flame, 1, 0) + np.roll(flame, -1, 0) + np.roll(flame, 1, 1) + np.roll(flame, -1, 1)) / 5
    me = o.data
    rest = [v.co.copy() for v in me.vertices]
    ys, xs = np.where(flame > 0.3); fbase = (H - ys.max()) * PX; fcx = (xs.mean() - W / 2) * PX
    wgt = []
    for r_ in rest:
        px = int(r_.x / PX + W / 2); py = int(H - r_.z / PX)
        wgt.append(float(flame[min(max(py, 0), H - 1), min(max(px, 0), W - 1)]))
    sh = me.color_attributes['shade'].data
    for f in range(frames):
        ph = 2 * math.pi * f / frames
        s_up = 1 + 0.12 * math.sin(ph * 2) + 0.06 * math.sin(ph * 3 + 1)
        for v, r_, w_ in zip(me.vertices, rest, wgt):
            up = max(0.0, r_.z - fbase)
            v.co = r_ + Vector((w_ * (0.04 * math.sin(ph * 2 + r_.z * 30) * up * 2 + (r_.x - fcx) * (1 / math.sqrt(s_up) - 1)),
                                0, w_ * up * (s_up - 1)))
        for c, w_ in zip(sh, wgt):
            k = 1 + w_ * (0.14 * math.sin(ph * 3 + 0.5) + 0.06 * math.sin(ph * 5))
            c.color = (k, k, k, 1)
        me.update()
        shot('campfire_%02d' % f)
    print('done campfire', flush=True)
    clear()

def flat_mat(col, alpha):
    m = bpy.data.materials.new('f'); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    em = nt.nodes.new('ShaderNodeEmission'); em.inputs['Color'].default_value = [((int(col[k:k + 2], 16) / 255) ** 2.2) for k in (1, 3, 5)] + [1]
    tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader'); mx.inputs[0].default_value = alpha
    L(tr.outputs[0], mx.inputs[1]); L(em.outputs[0], mx.inputs[2]); L(mx.outputs[0], out.inputs['Surface'])
    return m

def px_to_world(px, py, W, H, front=0.6):
    return Vector(((px - W / 2) * PX, -front, (H - py) * PX))

def disc(cx, cy, r, col, alpha, W, H, front):
    bpy.ops.mesh.primitive_circle_add(vertices=24, radius=r * PX, fill_type='NGON', location=px_to_world(cx, cy, W, H, front), rotation=(math.radians(90), 0, 0))
    o = bpy.context.object; o.data.materials.append(flat_mat(col, alpha)); return o

def smoke(x, y, ph, W, H, n=3, rise=60, size=7):
    """Soft puffs drifting up from (x, y) px, growing and fading; returns the objects made."""
    objs = []
    for k in range(n):
        t = (ph / (2 * math.pi) + k / n) % 1.0
        cx = x + 6 * math.sin(t * 5 + k) + t * 10; cy = y - t * rise; r = size * (0.6 + 1.2 * t)
        a = 0.75 * (1 - t) * min(1.0, t * 6)
        objs.append(disc(cx, cy, r * 1.15, '#B9B4AC', a * 0.6, W, H, 0.8 + 0.01 * k))
        objs.append(disc(cx - r * 0.15, cy - r * 0.15, r * 0.8, '#EDEAE4', a, W, H, 0.81 + 0.01 * k))
    return objs

def beam(x, y, ph, W, H, length=95, spread=0.22):
    """A lighthouse beam sweeping round: seen from the side it swings out to the left and right."""
    c = math.cos(ph); L_ = length * abs(c); sgn = 1 if c >= 0 else -1
    a = 0.32 * abs(c) ** 0.7
    p0 = px_to_world(x, y, W, H, 0.9)
    p1 = px_to_world(x + sgn * L_, y - L_ * spread, W, H, 0.9); p2 = px_to_world(x + sgn * L_, y + L_ * spread, W, H, 0.9)
    me = bpy.data.meshes.new('beam'); me.from_pydata([p0, p1, p2], [], [(0, 1, 2)]); me.update()
    o = bpy.data.objects.new('beam', me); sc.collection.objects.link(o); o.data.materials.append(flat_mat('#FFE9A8', a))
    return [o]

def ss(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a))); return t * t * (3 - 2 * t)

def region_anim(name, fn, frames=12, res=140, depth=0.22, extras=None):
    """Moves parts of the painted shape: fn(px, py, ph, img) -> (dx, dy, shade) in sprite pixels for each vertex;
    extras(ph, W, H) may add overlay objects (smoke, light) for the frame."""
    o, (W, H), info = card(name, depth=depth, res=res)
    set_camera(W, H)
    _, img = load_image(name)
    me = o.data
    rest = [v.co.copy() for v in me.vertices]
    pts = [(r.x / PX + W / 2, H - r.z / PX) for r in rest]
    sh = me.color_attributes['shade'].data
    for f in range(frames):
        ph = 2 * math.pi * f / frames
        for v, r, (px, py), c in zip(me.vertices, rest, pts, sh):
            dx, dy, k = fn(px, py, ph, img)
            v.co = r + Vector((dx * PX, 0, -dy * PX)); c.color = (k, k, k, 1)
        me.update()
        extra = extras(ph, W, H) if extras else []
        shot('%s_%02d' % (name, f))
        for e in extra: bpy.data.objects.remove(e, do_unlink=True)
    print('done', name, flush=True)
    clear()

def pix(img, px, py):
    H, W = img.shape[:2]
    return img[min(max(int(py), 0), H - 1), min(max(int(px), 0), W - 1)]

def glow(cx, cy, r, amt):
    def f(px, py, ph, img):
        d = math.hypot(px - cx, py - cy)
        return 0, 0, 1 + amt(ph) * (1 - ss(r * 0.5, r, d))
    return f

def rocker(pivot, deg, bob, y0, y1, speed=1):
    """Whatever is above y0 (fading out by y1) rocks about pivot and bobs (boats, buoys)."""
    def f(px, py, ph, img):
        w = 1 - ss(y0, y1, py)
        a = math.radians(deg) * math.sin(ph * speed); b = bob * math.sin(ph * speed + 1.2)
        x, y = px - pivot[0], py - pivot[1]
        rx = x * math.cos(a) - y * math.sin(a); ry = x * math.sin(a) + y * math.cos(a)
        return w * (rx - x), w * (ry - y + b), 1
    return f

def pendulum(pivot, deg, box):
    x0, y0, x1, y1 = box
    def f(px, py, ph, img):
        w = ss(x0 - 4, x0 + 2, px) * (1 - ss(x1 - 2, x1 + 4, px)) * ss(y0 - 2, y0 + 4, py) * (1 - ss(y1 - 2, y1 + 4, py))
        a = math.radians(deg) * math.sin(ph)
        x, y = px - pivot[0], py - pivot[1]
        rx = x * math.cos(a) - y * math.sin(a); ry = x * math.sin(a) + y * math.cos(a)
        return w * (rx - x), w * (ry - y), 1
    return f

def flutter_box(box, along, amp, waves=0.25, root=0):
    """Cloth in [box] waving; [along] 'x' = flag fixed on its left edge, 'y' = scarf fixed at its top."""
    x0, y0, x1, y1 = box
    def f(px, py, ph, img):
        inside = ss(x0 - 3, x0 + 2, px) * (1 - ss(x1 - 2, x1 + 3, px)) * ss(y0 - 3, y0 + 2, py) * (1 - ss(y1 - 2, y1 + 3, py))
        if along == 'x':
            w = inside * ss(x0 + root, x1, px)
            return 0, w * amp * math.sin(ph * 2 - px * waves), 1 + 0.08 * w * math.sin(ph * 2 - px * waves + 1.5)
        w = inside * ss(y0 + root, y1, py)
        return w * amp * math.sin(ph * 2 - py * waves), 0, 1 + 0.08 * w * math.sin(ph * 2 - py * waves + 1.5)
    return f

def combine(*fs):
    def f(px, py, ph, img):
        dx = dy = 0.0; k = 1.0
        for g in fs:
            a, b, c = g(px, py, ph, img); dx += a; dy += b; k *= c
        return dx, dy, k
    return f

def paddy_fn(px, py, ph, img):
    r, g, b, a = pix(img, px, py)
    m = 1.0 if (g > r + 0.06 and g > b - 0.02 and a > 0.5 and g > 0.35) else 0.0
    return m * 1.8 * math.sin(ph + px * 0.07 + py * 0.05), 0, 1 + 0.06 * m * math.sin(ph + px * 0.07 + 1)

def net_fn(px, py, ph, img):
    r, g, b, a = pix(img, px, py)
    lum = 0.3 * r + 0.59 * g + 0.11 * b
    w = ss(0, 1, (235 - py) / 170) * (1.0 if lum > 0.5 else 0.0)
    return w * 2.6 * math.sin(ph + py * 0.025), 0, 1

MORE = [
    ('v_house_a', lambda: region_anim('v_house_a', glow(248, 187, 16, lambda ph: 0.28 * math.sin(ph * 3) + 0.12 * math.sin(ph * 7)),
                                      extras=lambda ph, W, H: smoke(232, 50, ph, W, H))),
    ('v_well', lambda: region_anim('v_well', pendulum((155, 70), 2.2, (143, 72, 168, 178)))),
    ('tent', lambda: region_anim('tent', flutter_box((84, 20, 112, 52), 'x', 2.6, waves=0.3))),
    ('lighthouse', lambda: region_anim('lighthouse', glow(119, 55, 11, lambda ph: 0.25 + 0.25 * abs(math.cos(ph))),
                                       extras=lambda ph, W, H: beam(119, 55, ph, W, H))),
    ('ship', lambda: region_anim('ship', rocker((128, 176), 2.5, 2.0, 168, 182))),
    ('buoy', lambda: region_anim('buoy', rocker((96, 141), 5.0, 2.5, 130, 146))),
    ('snowman', lambda: region_anim('snowman', flutter_box((124, 138, 146, 190), 'y', 3.0, waves=0.12, root=4))),
    ('crane', lambda: region_anim('crane', pendulum((192, 27), 4.0, (176, 28, 210, 152)))),
    ('v_paddy', lambda: region_anim('v_paddy', paddy_fn, res=160)),
    ('v_bed', lambda: region_anim('v_bed', net_fn)),
    ('v_haystack', lambda: plant('v_haystack', 0.012, flutter=0.3, stiff=2.0)),
    ('lamp', lambda: region_anim('lamp', glow(175, 30, 18, lambda ph: 0.16 * math.sin(ph * 4) + 0.08 * math.sin(ph * 9)))),
]

JOBS = [
    ('tree_round', lambda: plant('tree_round', 0.035, flutter=0.4)),
    ('tree_tall', lambda: plant('tree_tall', 0.03, flutter=0.3, stiff=1.9)),
    ('pine_a', lambda: plant('pine_a', 0.028, flutter=0.3, stiff=1.9)),
    ('pine_b', lambda: plant('pine_b', 0.028, flutter=0.3, stiff=1.9)),
    ('pine_c', lambda: plant('pine_c', 0.03, flutter=0.3, stiff=1.8)),
    ('frost_pine_a', lambda: plant('frost_pine_a', 0.022, flutter=0.2, stiff=2.0)),
    ('frost_pine_b', lambda: plant('frost_pine_b', 0.022, flutter=0.2, stiff=2.0)),
    ('frost_pine_c', lambda: plant('frost_pine_c', 0.022, flutter=0.2, stiff=2.0)),
    ('xmas', lambda: plant('xmas', 0.015, flutter=0.15, stiff=2.2)),
    ('acacia', lambda: plant('acacia', 0.025, flutter=0.35, stiff=1.8)),
    ('dead_a', lambda: plant('dead_a', 0.018, flutter=0.25, stiff=2.0)),
    ('dead_b', lambda: plant('dead_b', 0.018, flutter=0.25, stiff=2.0)),
    ('palm', lambda: plant('palm', 0.05, flutter=0.6, stiff=1.5)),
    ('v_palm', lambda: plant('v_palm', 0.05, flutter=0.6, stiff=1.5)),
    ('v_banana', lambda: plant('v_banana', 0.045, flutter=0.6)),
    ('v_bamboo', lambda: plant('v_bamboo', 0.05, flutter=0.5, stiff=1.4)),
    ('jungle', lambda: plant('jungle', 0.04, flutter=0.6, stiff=1.3)),
    ('fern', lambda: plant('fern', 0.04, flutter=0.6, stiff=1.2)),
    ('bush', lambda: plant('bush', 0.025, flutter=0.5, stiff=1.2)),
    ('v_vbush', lambda: plant('v_vbush', 0.025, flutter=0.5, stiff=1.2)),
    ('snow_bush', lambda: plant('snow_bush', 0.015, flutter=0.3, stiff=1.3)),
    ('shrub', lambda: plant('shrub', 0.02, flutter=0.4, stiff=1.2)),
    ('frost_shrub', lambda: plant('frost_shrub', 0.02, flutter=0.4, stiff=1.2)),
    ('grass_tuft', lambda: plant('grass_tuft', 0.07, flutter=0.7, stiff=1.1)),
    ('v_vgrass', lambda: plant('v_vgrass', 0.06, flutter=0.7, stiff=1.1)),
    ('flowers', lambda: plant('flowers', 0.06, flutter=0.6, stiff=1.1)),
    ('turbine', turbine),
    ('campfire', campfire),
] + MORE
for name, job in JOBS:
    if ONLY and name not in ONLY: continue
    job()
