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

JOBS = [
    ('tree_round', lambda: plant('tree_round', 0.035, flutter=0.4)),
    ('tree_tall', lambda: plant('tree_tall', 0.03, flutter=0.3, stiff=1.9)),
    ('v_palm', lambda: plant('v_palm', 0.05, flutter=0.6, stiff=1.5)),
    ('v_banana', lambda: plant('v_banana', 0.045, flutter=0.6)),
    ('bush', lambda: plant('bush', 0.025, flutter=0.5, stiff=1.2)),
    ('turbine', turbine),
]
for name, job in JOBS:
    if ONLY and name not in ONLY: continue
    job()
