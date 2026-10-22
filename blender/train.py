"""Little Express: the train seen from above (nose caps for all 25 tiers, coach walls, coach roof).

    blender -b -P blender/train.py -- <out_dir> [--only nose3,car_fill]

Everything is modelled in "train space" dp (x across the car 0..190, y down the train; 1 dp = 0.01 units)
and rendered straight down with a fixed frame, so every sprite lands exactly where the game draws it.
Parts the game tints with the train colour come out as two images:
  <name>_fill.png  the tinted body alone, in light greys (shading survives the tint), no outline;
  <name>_line.png  outline + decorations (lamps, emblems, stripes, glass), body cut out (holdout).
Files: nose_fill/line (tier 0), nose1..24_fill/line, car_fill, car_line, car_roof.
"""
import bpy, bmesh, math, os, sys
from mathutils import Vector

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = next((a for i, a in enumerate(ARGS) if not a.startswith('--') and (i == 0 or ARGS[i - 1] != '--only')), '/tmp/train')
os.makedirs(OUT, exist_ok=True)
ONLY = ARGS[ARGS.index('--only') + 1].split(',') if '--only' in ARGS else None

bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'; sc.cycles.device = 'CPU'; sc.cycles.samples = 16
sc.cycles.use_denoising = False; sc.cycles.max_bounces = 0
sc.render.film_transparent = True; sc.render.filter_size = 1.2
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'; sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world; world.use_nodes = True
world.node_tree.nodes['Background'].inputs[1].default_value = 0.0
camd = bpy.data.cameras.new('cam'); camd.type = 'ORTHO'
CAM = bpy.data.objects.new('cam', camd); sc.collection.objects.link(CAM); sc.camera = CAM

S = 0.01   # world units per dp

# ------------------------------------------------------------------ materials
def lin(h, k=1.0):
    h = h.lstrip('#'); c = [min(1.0, int(h[i:i + 2], 16) / 255 * k) for i in (0, 2, 4)]
    return [x ** 2.2 for x in c] + [1]

MATS = {}
def toon(col, shade=0.82, top=1.08, flat=False):
    key = (col, shade, top, flat)
    if key in MATS: return MATS[key]
    m = bpy.data.materials.new('m%d' % len(MATS)); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    base = nt.nodes.new('ShaderNodeRGB'); base.outputs[0].default_value = lin(col)
    em = nt.nodes.new('ShaderNodeEmission')
    if flat:
        L(base.outputs[0], em.inputs['Color'])
    else:
        geo = nt.nodes.new('ShaderNodeNewGeometry'); sep = nt.nodes.new('ShaderNodeSeparateXYZ')
        up = nt.nodes.new('ShaderNodeMath'); up.operation = 'GREATER_THAN'; up.inputs[1].default_value = 0.93
        L(geo.outputs['Normal'], sep.inputs[0]); L(sep.outputs['Z'], up.inputs[0])
        lit = nt.nodes.new('ShaderNodeMix'); lit.data_type = 'RGBA'; lit.blend_type = 'MULTIPLY'; lit.inputs[0].default_value = 1
        L(base.outputs[0], lit.inputs[6]); lit.inputs[7].default_value = [top ** 2.2] * 3 + [1]
        m1 = nt.nodes.new('ShaderNodeMix'); m1.data_type = 'RGBA'
        L(up.outputs[0], m1.inputs[0]); L(base.outputs[0], m1.inputs[6]); L(lit.outputs[2], m1.inputs[7])
        # a softer second step on the rounded edges, darker where the surface turns away
        mid = nt.nodes.new('ShaderNodeMath'); mid.operation = 'GREATER_THAN'; mid.inputs[1].default_value = 0.55
        L(sep.outputs['Z'], mid.inputs[0])
        dk = nt.nodes.new('ShaderNodeMix'); dk.data_type = 'RGBA'; dk.blend_type = 'MULTIPLY'; dk.inputs[0].default_value = 1
        L(base.outputs[0], dk.inputs[6]); dk.inputs[7].default_value = [shade ** 2.2] * 3 + [1]
        m2 = nt.nodes.new('ShaderNodeMix'); m2.data_type = 'RGBA'
        L(mid.outputs[0], m2.inputs[0]); L(dk.outputs[2], m2.inputs[6]); L(m1.outputs[2], m2.inputs[7])
        L(m2.outputs[2], em.inputs['Color'])
    L(em.outputs[0], out.inputs['Surface'])
    MATS[key] = m; return m

def special(kind):
    if kind in MATS: return MATS[kind]
    m = bpy.data.materials.new(kind); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    if kind == 'holdout':
        L(nt.nodes.new('ShaderNodeHoldout').outputs[0], out.inputs['Surface'])
    elif kind == 'clear':
        L(nt.nodes.new('ShaderNodeBsdfTransparent').outputs[0], out.inputs['Surface'])
    else:   # outline
        geo = nt.nodes.new('ShaderNodeNewGeometry')
        em = nt.nodes.new('ShaderNodeEmission'); em.inputs['Color'].default_value = lin('#5A3A2C')
        tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader')
        L(geo.outputs['Backfacing'], mx.inputs['Fac']); L(em.outputs[0], mx.inputs[1]); L(tr.outputs[0], mx.inputs[2])
        L(mx.outputs[0], out.inputs['Surface'])
    MATS[kind] = m; return m

# ------------------------------------------------------------------ geometry (all inputs in dp)
CUR = []
BODY_GREY = '#E4E4E4'

def finish(o, mat, line, body=False, smooth=False):
    o.data.materials.append(mat)
    for p in o.data.polygons: p.use_smooth = smooth
    if line > 0:
        o.data.materials.append(special('outline'))
        md = o.modifiers.new('ol', 'SOLIDIFY'); md.thickness = line * S; md.offset = 1.0
        md.use_flip_normals = True; md.use_rim = False; md.material_offset = 1
    o['body'] = body
    CUR.append(o); return o

def P(x, y, z=0.0): return Vector((x * S, -y * S, z * S))

def prism(poly, z0, z1, col, line=1.0, bevel=0.0, segs=4, body=False, **k):
    """A flat shape [poly] (dp points) raised from z0 to z1 (dp), optionally with rounded top edges."""
    bm = bmesh.new()
    vs = [bm.verts.new(P(x, y, z0)) for x, y in poly]
    f = bm.faces.new(vs)
    bmesh.ops.recalc_face_normals(bm, faces=[f])
    if f.normal.z > 0: f.normal_flip()
    r = bmesh.ops.extrude_face_region(bm, geom=[f])
    top = [e for e in r['geom'] if isinstance(e, bmesh.types.BMVert)]
    bmesh.ops.translate(bm, verts=top, vec=(0, 0, (z1 - z0) * S))
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    me = bpy.data.meshes.new('p'); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new('p', me); sc.collection.objects.link(o)
    if bevel > 0:
        b = o.modifiers.new('bv', 'BEVEL'); b.width = bevel * S; b.segments = segs
        b.limit_method = 'ANGLE'; b.angle_limit = math.radians(50); b.harden_normals = False
    return finish(o, toon(col, **k), line, body)

def disc(cx, cy, r, z0, z1, col, line=0.8, n=32, **k):
    return prism([(cx + r * math.cos(2 * math.pi * i / n), cy + r * math.sin(2 * math.pi * i / n)) for i in range(n)], z0, z1, col, line, **k)

def dome(cx, cy, r, z, col, line=0.8, flat=False, sq=0.5):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=12, radius=r * S, location=P(cx, cy, z))
    o = bpy.context.object; o.scale = (1, 1, sq)
    return finish(o, toon(col, flat=flat, top=1.05), line, smooth=True)

def band(pts, w, z, col, line=0.6, **k):
    """A strip of width [w] dp along a polyline [pts] (dp), lying flat at height z."""
    left, right = [], []
    for i, p in enumerate(pts):
        a = Vector(pts[max(i - 1, 0)]); b = Vector(pts[min(i + 1, len(pts) - 1)])
        d = (b - a).normalized(); n = Vector((-d.y, d.x)) * w / 2
        left.append(tuple(Vector(p) + n)); right.append(tuple(Vector(p) - n))
    return prism(left + right[::-1], z, z + 0.6, col, line, **k)

# ------------------------------------------------------------------ shapes (dp, x 0..190, bottom edge at y 37)
def sample(f, x0, x1, n=40):
    return [(x0 + (x1 - x0) * i / n, f(x0 + (x1 - x0) * i / n)) for i in range(n + 1)]

def shape_poly(kind):
    B = 37.0
    if kind == 'classic':      # tier 0: a low rounded slab
        top = lambda x: -2 + 22 * (1 - math.sqrt(max(0.0, 1 - ((abs(x - 95) - 70) / 21) ** 2))) if abs(x - 95) > 70 else -2
        return [(4, B)] + sample(top, 4, 186, 60) + [(186, B)]
    if kind == 'box':
        r = 9.0
        left = [(16 + r * math.cos(math.radians(180 + 90 * i / 8)), 8 + r * math.sin(math.radians(180 + 90 * i / 8))) for i in range(9)]
        right = [(174 + r * math.cos(math.radians(270 + 90 * i / 8)), 8 + r * math.sin(math.radians(270 + 90 * i / 8))) for i in range(9)]
        return [(7, B)] + left + right + [(183, B)]
    if kind == 'wedge':
        return [(2, B), (56, 0), (95, -22), (134, 0), (188, B)]
    if kind == 'dome':
        return [(2, B)] + sample(lambda x: B - 57 * math.sqrt(max(0.0, 1 - ((x - 95) / 93) ** 2)), 2, 188, 60)[1:-1] + [(188, B)]
    if kind == 'ogive':
        return [(2, B)] + sample(lambda x: B - 62 * (1 - abs((x - 95) / 93)) ** 0.62, 2, 188, 60)[1:-1] + [(188, B)]
    if kind == 'notched':
        return [(2, B), (2, 10), (38, 0), (62, 0), (72, -16), (118, -16), (128, 0), (152, 0), (188, 10), (188, B)]

def width_at(poly, y):
    """x extent of the polygon at height y (dp)."""
    xs = []
    n = len(poly)
    for i in range(n):
        (x0, y0), (x1, y1) = poly[i], poly[(i + 1) % n]
        if (y0 - y) * (y1 - y) <= 0 and y0 != y1:
            xs.append(x0 + (x1 - x0) * (y - y0) / (y1 - y0))
    return (min(xs), max(xs)) if xs else (95, 95)

# ------------------------------------------------------------------ decorations
GOLD = '#F4C542'; CREAM = '#FFF3C4'; GLASS = '#BCE4F4'; WHITE = '#FFFFFF'; NAVY = '#1F2A55'

def lamp(x, y):
    disc(x, y, 5.2, 8, 9.5, '#6B4A3A', line=0.8)
    dome(x, y, 4.0, 9.5, CREAM, line=0.0, flat=True, sq=0.4)
    dome(x - 1.3, y - 1.3, 1.2, 10.6, WHITE, line=0, flat=True, sq=0.3)

def star(cx, cy, r, col, ring=None):
    if ring: disc(cx, cy, r * 1.35, 9, 10, ring, line=0.8)
    pts = [(cx + (r if i % 2 == 0 else r * 0.45) * math.cos(-math.pi / 2 + i * math.pi / 5),
            cy + (r if i % 2 == 0 else r * 0.45) * math.sin(-math.pi / 2 + i * math.pi / 5)) for i in range(10)]
    prism(pts, 10, 11, col, line=0.7)

def heart(cx, cy, r, col):
    pts = []
    for i in range(48):
        t = 2 * math.pi * i / 48
        x = 16 * math.sin(t) ** 3; y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((cx + x * r / 16, cy - y * r / 16))
    prism(pts, 10, 11, col, line=0.7)

def sun(cx, cy, r, col):
    for k in range(8):
        a = k * math.pi / 4
        prism([(cx + math.cos(a) * r * 1.25 - math.sin(a) * 1.0, cy + math.sin(a) * r * 1.25 + math.cos(a) * 1.0),
               (cx + math.cos(a) * r * 1.85, cy + math.sin(a) * r * 1.85),
               (cx + math.cos(a) * r * 1.25 + math.sin(a) * 1.0, cy + math.sin(a) * r * 1.25 - math.cos(a) * 1.0)], 9.5, 10.5, col, line=0.6)
    disc(cx, cy, r, 10, 11, col, line=0.8)

def snowflake(cx, cy, r, col='#8FD3F0'):
    disc(cx, cy, r * 1.35, 9, 10, col, line=0.8)
    for k in range(3):
        a = k * math.pi / 3
        band([(cx - math.cos(a) * r, cy - math.sin(a) * r), (cx + math.cos(a) * r, cy + math.sin(a) * r)], 1.6, 10.2, WHITE, line=0)

def diamond(cx, cy, r, col):
    prism([(cx - r, cy - r * 0.35), (cx - r * 0.55, cy - r * 0.9), (cx + r * 0.55, cy - r * 0.9), (cx + r, cy - r * 0.35), (cx, cy + r)], 10, 11, col, line=0.7)
    band([(cx - r * 0.8, cy - r * 0.35), (cx + r * 0.8, cy - r * 0.35)], 0.8, 11.1, WHITE, line=0, flat=True)

def porthole(cx, cy, r, col):
    disc(cx, cy, r * 1.2, 9, 10, '#6B4A3A', line=0.6)
    disc(cx, cy, r, 10, 11, col, line=0, top=1.0)
    dome(cx - r * 0.3, cy - r * 0.3, r * 0.25, 11.5, WHITE, line=0, flat=True, sq=0.3)

def screen(cy, w=50):
    pts = [(95 + w / 2 * t, cy + 4 * t * t) for t in [i / 10 - 1 for i in range(21)]]
    band(pts, 4.0, 9.6, GLASS, line=0.7, top=1.05)
    band([(95 - w / 3, cy + 0.2), (95 - w / 6, cy - 0.4)], 1.0, 10.4, WHITE, line=0, flat=True)

def stripes(poly, cols):
    for i, c in enumerate(cols):
        y = 33.5 - i * 3.2
        x0, x1 = width_at(poly, y)
        band([(x0 + 4, y), (x1 - 4, y)], 2.2, 9.2 + i * 0.05, c, line=0.5)

def spikes(poly, col, n=8):
    x0, x1 = width_at(poly, 36.5)
    for i in range(n):
        x = x0 + 10 + (x1 - x0 - 20) * i / (n - 1)
        prism([(x - 2.6, 37), (x + 2.6, 37), (x, 41.5)], 0, 2, col, line=0.6)

def fins(y, col):
    for s in (-1, 1):
        x = 0 if s < 0 else 190
        prism([(x, y - 3.5), (x, y + 3.5), (x + s * 7.5, y)], 4, 6, col, line=0.7)

def side_spikes(poly, col):
    """Little teeth along the sloping edges (the wedge nose)."""
    for i in range(len(poly) - 1):
        (x0, y0), (x1, y1) = poly[i], poly[i + 1]
        if abs(x1 - x0) < 1 or abs(y1 - y0) < 1: continue
        d = Vector((x1 - x0, y1 - y0)); L_ = d.length; d.normalize(); nrm = Vector((d.y, -d.x))
        if nrm.y > 0: nrm = -nrm
        for k in range(1, int(L_ // 14)):
            p = Vector((x0, y0)) + d * 14 * k
            prism([tuple(p - d * 2.4), tuple(p + d * 2.4), tuple(p + nrm * 4.5)], 2, 4, '#F2E6CC', line=0.6)

# per tier: shape, emblem, extras (screen, stripes, spikes, fins, special)
LAMPS = {'classic': (56, 15, 134, 15), 'box': (25, 2.5, 165, 2.5), 'wedge': (62, 10, 128, 10),
         'dome': (36, 18, 154, 18), 'ogive': (44, 20, 146, 20), 'notched': (40, 14, 150, 14)}
EMB_Y = {'classic': 15, 'box': 12, 'wedge': 8, 'dome': 16, 'ogive': 16, 'notched': 6}
TIERS = [
    dict(shape='classic', special='classic'),
    dict(shape='box', special='cab'),
    dict(shape='wedge', special='ridge'),
    dict(shape='dome', screen=True, rim='#7FE0C8'),
    dict(shape='ogive', special='arch'),
    dict(shape='notched', emblem=('star', GOLD), trim=GOLD, spikes=GOLD),
    dict(shape='dome', screen=True, emblem=('star', GOLD), stripes=[WHITE]),
    dict(shape='box', emblem=('snow', '#5BB5E6'), stripes=['#F4C542', '#2B2F3A']),
    dict(shape='ogive', emblem=('star', GOLD), stripes=[WHITE, '#5B9BD5'], fins=GOLD),
    dict(shape='dome', emblem=('heart', '#F28FB0'), stripes=[WHITE]),
    dict(shape='notched', emblem=('snow', '#8FD3F0'), stripes=[WHITE], spikes='#8FD3F0'),
    dict(shape='wedge', emblem=('diamond', '#9DDCF2'), stripes=[WHITE]),
    dict(shape='dome', screen=True, emblem=('sun', '#F7B733'), stripes=['#F4C542', '#F08A5C']),
    dict(shape='box', emblem=('star', WHITE, '#D9534F'), stripes=['#B8C0C8', '#8E98A3'], spikes='#D9534F'),
    dict(shape='ogive', emblem=('sun', '#F08A3C'), stripes=['#F4C542', '#D9534F'], fins='#F08A3C'),
    dict(shape='dome', screen=True, emblem=('port', NAVY), stripes=['#C8D0D8']),
    dict(shape='wedge', emblem=('diamond', '#7FD1AE'), stripes=['#7FD1AE', '#2E8B6A'], spikes='#2E8B6A'),
    dict(shape='notched', emblem=('star', '#E9E2FF', '#9C8BD0'), stripes=['#7FD1AE', '#9C8BD0']),
    dict(shape='box', screen=True, emblem=('port', WHITE), stripes=['#8FB8E8', WHITE]),
    dict(shape='ogive', emblem=('star', GOLD, '#FFF3C4'), stripes=[WHITE, '#D9534F', WHITE], fins='#FFF3C4'),
    dict(shape='dome', screen=True, emblem=('port', NAVY), stripes=[NAVY]),
    dict(shape='wedge', emblem=('sun', '#F7D25C'), stripes=['#2B2F3A', '#F7D25C'], spikes='#F4C542'),
    dict(shape='ogive', emblem=('heart', '#F08A5C'), stripes=['#F4C542', '#D9534F'], fins='#F08A5C'),
    dict(shape='dome', emblem=('star', WHITE, '#F4F0FF'), stripes=['#B9A6E8', WHITE], fins=WHITE),
    dict(shape='notched', emblem=('sun', '#F7D25C'), stripes=[WHITE, '#F4C542'], spikes='#F4C542'),
]

def build_nose(t):
    d = TIERS[t]; kind = d['shape']; poly = shape_poly(kind)
    prism(poly, 0, 9, BODY_GREY, line=1.3, bevel=6.5, segs=5, body=True)
    lx0, ly0, lx1, ly1 = LAMPS[kind]
    ey = EMB_Y[kind]
    sp = d.get('special')
    if sp == 'classic':
        band(sample(lambda x: 4 + 0.0 * x, 30, 160, 2), 1.6, 9.4, '#FFFFFF', line=0, flat=True)
    if sp == 'cab':
        for x0, x1 in ((48, 93), (97, 142)):
            prism([(x0, 0), (x1, 0), (x1, 14), (x0, 14)], 9, 10, GLASS, line=0.9, top=1.05)
        band([(54, 3), (66, 3)], 1.2, 10.2, WHITE, line=0, flat=True)
        for x in (25, 165): prism([(x - 7, -1), (x + 7, -1), (x + 7, 6), (x - 7, 6)], 9, 10, CREAM, line=0.9, flat=True)
        for i in range(17):   # hazard stripe along the bottom
            x = 10 + i * 10
            prism([(x, 37), (x + 5, 37), (x + 10, 28), (x + 5, 28)], 9.3, 10.1, '#2B2F3A', line=0)
        prism([(8, 28), (182, 28), (182, 37), (8, 37)], 9.0, 9.3, GOLD, line=0.8)
        lx0 = lx1 = None
    if sp == 'ridge':
        for s in (-1, 1):
            band([(95, -18), (95 + s * 58, 28)], 2.0, 9.3, '#D8DEE6', line=0.4)
        for i in range(10): disc(40 + i * 12, 31, 1.0, 9, 9.6, '#F2E6CC', line=0)
        side_spikes(poly, '#F2E6CC')
    if sp == 'arch':
        pts = [(95 + 9 * math.cos(a), ey - 4 - 9 * math.sin(a)) for a in [math.pi * i / 16 for i in range(17)]]
        band([(104, ey + 14)] + pts + [(86, ey + 14)], 2.8, 9.4, WHITE, line=0.7)
        band([(80, ey - 1), (110, ey - 1)], 3.0, 9.6, GLASS, line=0.6)
    if d.get('rim'):
        band([(x, y + 4) for x, y in poly[2:-2:2]], 2.0, 9.2, d['rim'], line=0.5)
    if d.get('screen'):
        screen(ey - 12 if kind != 'box' else 3)
    if 'emblem' in d:
        e = d['emblem']; ex, ey_ = 95, ey + (8 if d.get('screen') and kind == 'box' else 0)
        {'star': lambda: star(ex, ey_, 5.5, e[1], e[2] if len(e) > 2 else '#FFF3C4'),
         'heart': lambda: heart(ex, ey_, 5.5, e[1]), 'sun': lambda: sun(ex, ey_, 4.4, e[1]),
         'snow': lambda: snowflake(ex, ey_, 4.8, e[1]), 'diamond': lambda: diamond(ex, ey_, 5.5, e[1]),
         'port': lambda: porthole(ex, ey_, 6.0, e[1])}[e[0]]()
    if d.get('stripes'): stripes(poly, d['stripes'])
    if d.get('spikes'): spikes(poly, d['spikes'])
    if d.get('fins'): fins(ly0, d['fins'])
    if d.get('trim'): band(poly[1:-1], 1.6, 9.6, d['trim'], line=0.4)
    if lx0 is not None:
        lamp(lx0, ly0); lamp(lx1, ly1)

# ------------------------------------------------------------------ coach walls and roof
def rrect(x0, y0, x1, y1, r, n=8):
    pts = []
    for cx, cy, a0 in ((x1 - r, y0 + r, -90), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90), (x0 + r, y0 + r, 180)):
        for i in range(n + 1):
            a = math.radians(a0 + 90 * i / n); pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts

def build_walls():
    outer = rrect(0, 0, 190, 116, 12)
    inner = rrect(7, 7, 183, 109, 6)
    # a ring: outer shape with the inner cut out, built as one bmesh
    bm = bmesh.new()
    # sample both loops to the same count for a clean ring
    def resample(loop, n):
        L_ = [0.0]
        for a, b in zip(loop, loop[1:] + loop[:1]): L_.append(L_[-1] + math.dist(a, b))
        tot = L_[-1]; res = []
        j = 0
        for i in range(n):
            s_ = tot * i / n
            while L_[j + 1] < s_: j += 1
            a, b = loop[j], loop[(j + 1) % len(loop)]; f = (s_ - L_[j]) / max(1e-6, L_[j + 1] - L_[j])
            res.append((a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f))
        return res
    o_l = resample(outer, 160); i_l = resample(inner, 160)
    H = 10.0
    vb = [bm.verts.new(P(x, y, 0)) for x, y in o_l]; vt = [bm.verts.new(P(x, y, H)) for x, y in o_l]
    ib = [bm.verts.new(P(x, y, 0)) for x, y in i_l]; it = [bm.verts.new(P(x, y, H)) for x, y in i_l]
    n = len(o_l)
    for i in range(n):
        j = (i + 1) % n
        bm.faces.new((vt[i], vt[j], it[j], it[i]))      # top
        bm.faces.new((vb[i], vb[j], vt[j], vt[i]))      # outer side
        bm.faces.new((ib[i], it[i], it[j], ib[j]))      # inner side
        bm.faces.new((vb[i], ib[i], ib[j], vb[j]))      # bottom
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    me = bpy.data.meshes.new('walls'); bm.to_mesh(me); bm.free()
    o = bpy.data.objects.new('walls', me); sc.collection.objects.link(o)
    b = o.modifiers.new('bv', 'BEVEL'); b.width = 3.0 * S; b.segments = 4; b.limit_method = 'ANGLE'; b.angle_limit = math.radians(50)
    finish(o, toon(BODY_GREY), 1.2, body=True)
    # rivets along the long walls and handrail bumps on the sides (tinted with the body)
    for x in list(range(22, 175, 19)):
        for y in (3.5, 112.5): rivet(x, y, H)
    for y in (24, 92):
        for x in (-3.5, 193.5):
            prism([(x - 3, y - 8), (x + 3, y - 8), (x + 3, y + 8), (x - 3, y + 8)], 2, 8, BODY_GREY, line=1.0, bevel=2.0, body=True)
    # glass in the side walls (line layer)
    for y0, y1 in ((22, 50), (66, 94)):
        for x0, x1 in ((1.5, 5.5), (184.5, 188.5)):
            prism([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], H, H + 0.6, GLASS, line=0.6, top=1.05)
            band([(x0 + 2, y0 + 3), (x0 + 2, y0 + 9)], 0.9, H + 0.8, WHITE, line=0, flat=True)

def rivet(x, y, z):
    o = dome(x, y, 1.5, z, BODY_GREY, line=0.45, sq=0.6)
    o['body'] = True

def build_roof():
    prism(rrect(0, 0, 190, 116, 12), 0, 8, '#E9E9E9', line=1.3, bevel=7.0, segs=5, body=True)
    prism(rrect(75, 6, 115, 110, 9), 8, 11, '#F4F4F4', line=0.9, bevel=2.5, segs=3, body=True)       # centre ridge
    for vy in (30, 62, 94):
        prism(rrect(86, vy - 7, 104, vy + 7, 3), 11, 14, '#CFCFCF', line=0.8, bevel=1.2, segs=2, body=True)
        prism(rrect(88, vy - 5, 102, vy - 1, 1.5), 14, 14.5, '#F6F6F6', line=0, body=True)
    for sy in range(18, 110, 22):
        for x0, x1 in ((14, 72), (118, 176)):
            band([(x0, sy), (x1, sy)], 1.0, 8.05, '#C4C4C4', line=0, flat=True)

# ------------------------------------------------------------------ render passes
def render(path, x0, y0, x1, y1, px_per_dp):
    w, h = x1 - x0, y1 - y0
    camd.ortho_scale = max(w, h) * S
    CAM.location = ((x0 + x1) / 2 * S, -(y0 + y1) / 2 * S, 5.0); CAM.rotation_euler = (0, 0, 0)
    sc.render.resolution_x = round(w * px_per_dp); sc.render.resolution_y = round(h * px_per_dp)
    sc.render.filepath = os.path.join(OUT, path)
    bpy.ops.render.render(write_still=True)

def set_pass(kind):
    """'fill': only the body, grey, no outline. 'line': body cut out (holdout) but its outline kept, plus details.
    'full': everything as built."""
    for o in CUR:
        body = bool(o.get('body'))
        if '_mats' not in o: o['_mats'] = [m.name for m in o.data.materials]
        names = o['_mats']
        mats = [bpy.data.materials[n] for n in names]
        if kind == 'fill':
            o.hide_render = not body
            if body:
                mats = [mats[0]] + [special('clear') for _ in mats[1:]]
        elif kind == 'line':
            o.hide_render = False
            if body: mats = [special('holdout')] + mats[1:]
        for i, m in enumerate(mats): o.data.materials[i] = m

def clear():
    for o in CUR: bpy.data.objects.remove(o, do_unlink=True)
    CUR.clear()

JOBS = []
for t in range(25):
    nm = 'nose' if t == 0 else 'nose%d' % t
    JOBS.append((nm, lambda t=t: build_nose(t), (-6, -6, 196, 38) if t == 0 else (-6, -26, 196, 38), 4.0, True))
JOBS.append(('car', build_walls, (-8, -8, 198, 124), 4.0, True))
JOBS.append(('car_roof', build_roof, (-3, -3, 193, 119), 412 / 196, False))

for name, fn, frame, ppd, two in JOBS:
    if ONLY and name not in ONLY and not any(o.startswith(name) for o in ONLY): continue
    fn()
    if two:
        set_pass('fill'); render(name + '_fill.png', *frame, ppd)
        set_pass('line'); render(name + '_line.png', *frame, ppd)
    else:
        render(name + '.png', *frame, ppd)
    print('done', name, flush=True)
    clear()
