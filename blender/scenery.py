"""Little Express: the whole roadside world as toon-shaded 3D models, rendered to sprites.

    blender -b -P blender/scenery.py -- <out_dir> [--only name1,name2]

Every scenery item (trees, plants, rocks, village houses, props, vehicles) is built from simple
shapes and rendered on its own to <out_dir>/<name>.png, framed tight, seen from the same raised
three-quarter angle as the rest of the art. Station buildings, the bench and the lamp are seen
from above (the train runs up the screen) to match the platforms. store_assets/make_env_sprites.py
then fits each render into the game's sprite slot.
Shading: flat colour, a lighter tone on faces that look up, a darker band toward the silhouette,
and a thin warm-brown inverted-hull outline (the same look as the NPCs).
"""
import bpy, bmesh, math, os, sys, random
from mathutils import Vector, Matrix

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = next((a for i, a in enumerate(ARGS) if not a.startswith('--') and (i == 0 or ARGS[i - 1] != '--only')), '/tmp/scenery')
os.makedirs(OUT, exist_ok=True)
ONLY = ARGS[ARGS.index('--only') + 1].split(',') if '--only' in ARGS else None

# ------------------------------------------------------------------ scene
bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'
sc.cycles.device = 'CPU'
sc.cycles.samples = 12
sc.cycles.use_denoising = False
sc.cycles.max_bounces = 0
sc.render.film_transparent = True
sc.render.filter_size = 1.2
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'
sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world
world.use_nodes = True
world.node_tree.nodes['Background'].inputs[1].default_value = 0.0
camd = bpy.data.cameras.new('cam'); camd.type = 'ORTHO'
CAM = bpy.data.objects.new('cam', camd); sc.collection.objects.link(CAM); sc.camera = CAM

# ------------------------------------------------------------------ materials
def lin(h, k=1.0):
    h = h.lstrip('#'); c = [min(1.0, int(h[i:i + 2], 16) / 255 * k) for i in (0, 2, 4)]
    return [x ** 2.2 for x in c] + [1]

MATS = {}
def toon(col, shade=0.84, top=1.1, stripes=None, flat=False, alpha=1.0):
    """Flat colour; faces looking up get [top]x brighter, the silhouette band [shade]x darker.
    stripes=(col2, bands per unit, axis) paints bands in object space; flat=True is an unshaded glow."""
    key = (col, shade, top, stripes, flat, alpha)
    if key in MATS: return MATS[key]
    m = bpy.data.materials.new('m%d' % len(MATS)); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    def rgb(c):
        n = nt.nodes.new('ShaderNodeRGB'); n.outputs[0].default_value = c; return n.outputs[0]
    if stripes:
        tc = nt.nodes.new('ShaderNodeTexCoord'); sep = nt.nodes.new('ShaderNodeSeparateXYZ')
        mul = nt.nodes.new('ShaderNodeMath'); mul.operation = 'MULTIPLY'; mul.inputs[1].default_value = stripes[1] * math.pi * 2
        sn = nt.nodes.new('ShaderNodeMath'); sn.operation = 'SINE'
        gt = nt.nodes.new('ShaderNodeMath'); gt.operation = 'GREATER_THAN'; gt.inputs[1].default_value = 0.0
        mix = nt.nodes.new('ShaderNodeMix'); mix.data_type = 'RGBA'
        L(rgb(lin(col)), mix.inputs[6]); L(rgb(lin(stripes[0])), mix.inputs[7])
        L(tc.outputs['Object'], sep.inputs[0]); L(sep.outputs[stripes[2] if len(stripes) > 2 else 'Z'], mul.inputs[0])
        L(mul.outputs[0], sn.inputs[0]); L(sn.outputs[0], gt.inputs[0]); L(gt.outputs[0], mix.inputs[0])
        base = mix.outputs[2]
    else:
        base = rgb(lin(col))
    if flat:
        em = nt.nodes.new('ShaderNodeEmission'); L(base, em.inputs['Color']); em.inputs['Strength'].default_value = 1.0
        L(em.outputs[0], out.inputs['Surface']); MATS[key] = m; return m
    # lighter where the face looks up
    geo = nt.nodes.new('ShaderNodeNewGeometry'); sep2 = nt.nodes.new('ShaderNodeSeparateXYZ')
    up = nt.nodes.new('ShaderNodeMath'); up.operation = 'GREATER_THAN'; up.inputs[1].default_value = 0.62
    L(geo.outputs['Normal'], sep2.inputs[0]); L(sep2.outputs['Z'], up.inputs[0])
    lit = nt.nodes.new('ShaderNodeMix'); lit.data_type = 'RGBA'; lit.blend_type = 'MULTIPLY'; lit.inputs[0].default_value = 1.0
    L(base, lit.inputs[6]); lit.inputs[7].default_value = [top ** 2.2] * 3 + [1]
    m1 = nt.nodes.new('ShaderNodeMix'); m1.data_type = 'RGBA'
    L(up.outputs[0], m1.inputs[0]); L(base, m1.inputs[6]); L(lit.outputs[2], m1.inputs[7])
    # darker toward the silhouette
    lw = nt.nodes.new('ShaderNodeLayerWeight'); lw.inputs['Blend'].default_value = 0.35
    ramp = nt.nodes.new('ShaderNodeValToRGB'); ramp.color_ramp.interpolation = 'CONSTANT'
    ramp.color_ramp.elements[0].color = (0, 0, 0, 1); ramp.color_ramp.elements[1].position = 0.58
    L(lw.outputs['Facing'], ramp.inputs['Fac'])
    dk = nt.nodes.new('ShaderNodeMix'); dk.data_type = 'RGBA'; dk.blend_type = 'MULTIPLY'; dk.inputs[0].default_value = 1.0
    L(m1.outputs[2], dk.inputs[6]); dk.inputs[7].default_value = [shade ** 2.2] * 3 + [1]
    m2 = nt.nodes.new('ShaderNodeMix'); m2.data_type = 'RGBA'
    L(ramp.outputs['Color'], m2.inputs[0]); L(m1.outputs[2], m2.inputs[6]); L(dk.outputs[2], m2.inputs[7])
    em = nt.nodes.new('ShaderNodeEmission'); L(m2.outputs[2], em.inputs['Color'])
    if alpha < 1.0:
        tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader')
        mx.inputs[0].default_value = alpha; L(tr.outputs[0], mx.inputs[1]); L(em.outputs[0], mx.inputs[2])
        L(mx.outputs[0], out.inputs['Surface'])
    else:
        L(em.outputs[0], out.inputs['Surface'])
    MATS[key] = m; return m

def outline_mat():
    if 'ol' in MATS: return MATS['ol']
    m = bpy.data.materials.new('ol'); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial'); geo = nt.nodes.new('ShaderNodeNewGeometry')
    em = nt.nodes.new('ShaderNodeEmission'); em.inputs['Color'].default_value = lin('#5A3A2C')
    tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader')
    L(geo.outputs['Backfacing'], mx.inputs['Fac']); L(em.outputs[0], mx.inputs[1]); L(tr.outputs[0], mx.inputs[2])
    L(mx.outputs[0], out.inputs['Surface'])
    MATS['ol'] = m; return m

# ------------------------------------------------------------------ geometry helpers
LINE = [0.02]          # outline thickness for the item being built (world units)
CUR = []
def reg(o): CUR.append(o); return o

def finish(o, mat, line=None, smooth=True):
    line = LINE[0] if line is None else line
    o.data.materials.append(mat)
    for p in o.data.polygons: p.use_smooth = smooth
    if line > 0:
        o.data.materials.append(outline_mat())
        s = max(abs(v) for v in o.scale) or 1
        md = o.modifiers.new('ol', 'SOLIDIFY'); md.thickness = line / s; md.offset = 1.0
        md.use_flip_normals = True; md.use_rim = False; md.material_offset = 1
    return reg(o)

def M(col, **k):
    return col if isinstance(col, bpy.types.Material) else toon(col, **k)

def R(rot): return [math.radians(a) for a in rot]

def sphere(loc, r, sc_=(1, 1, 1), col='#FFFFFF', seg=32, rot=(0, 0, 0), line=None, smooth=True, **k):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=max(6, seg // 2), radius=r, location=loc, rotation=R(rot))
    o = bpy.context.object; o.scale = sc_
    return finish(o, M(col, **k), line, smooth)

def blob(loc, r, sc_=(1, 1, 1), col='#FFFFFF', rough=0.18, seed=1, sub=3, rot=(0, 0, 0), line=None, flat_bottom=False, smooth=True, **k):
    """A lumpy rounded shape (rock, bush, snow pile): an icosphere pushed in and out by smooth noise."""
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=sub, radius=1.0, location=(0, 0, 0))
    o = bpy.context.object
    rnd = random.Random(seed)
    waves = [(Vector((rnd.uniform(-1, 1), rnd.uniform(-1, 1), rnd.uniform(-1, 1))).normalized(), rnd.uniform(1.5, 3.2), rnd.uniform(0, 6.28)) for _ in range(5)]
    for v in o.data.vertices:
        n = v.co.normalized()
        d = sum(math.sin(w.dot(n) * f + ph) for w, f, ph in waves) / len(waves)
        v.co = n * (1 + rough * d)
        if flat_bottom and v.co.z < -0.25: v.co.z = -0.25 + (v.co.z + 0.25) * 0.15
    o.location = loc; o.scale = [r * s for s in sc_]; o.rotation_euler = R(rot)
    return finish(o, M(col, **k), line, smooth)

def limb(a, b, r1, r2=None, col='#8A5A3C', verts=16, line=None, smooth=True, **k):
    """A (tapered) cylinder from a to b."""
    a, b = Vector(a), Vector(b); d = b - a
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r1, radius2=r1 if r2 is None else r2, depth=d.length, location=(a + b) / 2)
    o = bpy.context.object
    o.rotation_mode = 'QUATERNION'; o.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(d.normalized())
    return finish(o, M(col, **k), line, smooth)

def cone(loc, r1, r2, h, col, verts=24, rot=(0, 0, 0), sc_=(1, 1, 1), line=None, smooth=True, **k):
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r1, radius2=r2, depth=h, location=(loc[0], loc[1], loc[2] + h / 2), rotation=R(rot))
    o = bpy.context.object; o.scale = sc_
    return finish(o, M(col, **k), line, smooth)

def box(loc, size, col, bevel=0.1, rot=(0, 0, 0), line=None, **k):
    """A rounded box, [size] = full extents, [bevel] in world units."""
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=loc, rotation=R(rot))
    o = bpy.context.object
    o.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    if bevel > 0:
        b = o.modifiers.new('bv', 'BEVEL'); b.width = min(bevel, min(size) * 0.45); b.segments = 3
    return finish(o, M(col, **k), line, smooth=False)

def ring(loc, R_, r, col, sc_=(1, 1, 1), rot=(0, 0, 0), line=None, seg=40, **k):
    bpy.ops.mesh.primitive_torus_add(major_radius=R_, minor_radius=r, major_segments=seg, minor_segments=10, location=loc, rotation=R(rot))
    o = bpy.context.object; o.scale = sc_
    return finish(o, M(col, **k), line)

def mesh_obj(verts, faces, col, line=None, smooth=True, **k):
    me = bpy.data.meshes.new('m'); me.from_pydata(verts, [], faces); me.update()
    o = bpy.data.objects.new('o', me); sc.collection.objects.link(o)
    bm = bmesh.new(); bm.from_mesh(me); bmesh.ops.recalc_face_normals(bm, faces=bm.faces); bm.to_mesh(me); bm.free()
    return finish(o, M(col, **k), line, smooth)

def strip(pts, widths, col, thick=0.015, normal_up=True, line=None, **k):
    """A thin ribbon with thickness along [pts] (leaf, frond, blade); widths per point, flat side facing up."""
    n = len(pts); V = []; F = []
    pts = [Vector(p) for p in pts]
    for i in range(n):
        d = (pts[min(i + 1, n - 1)] - pts[max(i - 1, 0)]).normalized()
        side = d.cross(Vector((0, 0, 1)))
        if side.length < 1e-4: side = Vector((1, 0, 0))
        side.normalize()
        nrm = side.cross(d).normalized()
        if nrm.z < 0: nrm = -nrm
        w = widths[i] / 2
        for s in (-1, 1):
            for t in (1, -1):
                V.append(tuple(pts[i] + side * s * w + nrm * t * thick / 2))
    # vertex order per point: (-side,+n)(-side,-n)(+side,+n)(+side,-n)
    for i in range(n - 1):
        a = 4 * i; b = 4 * (i + 1)
        F += [(a + 0, a + 2, b + 2, b + 0), (a + 1, b + 1, b + 3, a + 3), (a + 0, b + 0, b + 1, a + 1), (a + 2, a + 3, b + 3, b + 2)]
    F += [(0, 1, 3, 2), (4 * (n - 1), 4 * (n - 1) + 2, 4 * (n - 1) + 3, 4 * (n - 1) + 1)]
    return mesh_obj(V, F, col, line, **k)

def curve_pts(base, d, length, droop, n=10, twist=0.0):
    base = Vector(base); d = Vector(d).normalized()
    return [base + d * length * (i / (n - 1)) + Vector((0, 0, -droop)) * (i / (n - 1)) ** 2 for i in range(n)]

def snow_cap(loc, r, sc_=(1, 1, 0.35), seed=3, **k):
    return blob(loc, r, sc_, '#F7FBFE', rough=0.12, seed=seed, top=1.0, shade=0.9, **k)

def snow_top(o, cut=0.62, grow=1.035, lift=0.012):
    """Snow lying on [o]: a copy of its surface a touch bigger, kept only above [cut] (0 bottom .. 1 top)."""
    bpy.context.view_layer.update()
    me = o.data.copy(); me.materials.clear()
    me.transform(o.matrix_world)
    vs = me.vertices
    ctr = sum((v.co for v in vs), Vector()) / len(vs)
    for v in vs: v.co = ctr + (v.co - ctr) * grow + Vector((0, 0, lift))
    zs = [v.co.z for v in vs]; z0 = min(zs) + (max(zs) - min(zs)) * cut
    bm = bmesh.new(); bm.from_mesh(me)
    bmesh.ops.bisect_plane(bm, geom=bm.verts[:] + bm.edges[:] + bm.faces[:], dist=1e-5, plane_co=(0, 0, z0), plane_no=(0, 0, 1), clear_inner=True)
    bm.to_mesh(me); bm.free()
    c = bpy.data.objects.new('snow', me); sc.collection.objects.link(c)
    return finish(c, toon(SNOW, top=1.0, shade=0.9), smooth=True)

def surface_point(center, direction):
    """Where a ray from outside toward [center] first meets what has been built (and the normal there)."""
    bpy.context.view_layer.update()
    dg = bpy.context.evaluated_depsgraph_get()
    d = Vector(direction).normalized()
    hit, loc, nrm, _, _, _ = sc.ray_cast(dg, Vector(center) + d * 6.0, -d)
    return (loc, nrm) if hit else (None, None)

def dots_on(center, n, seed, make, a0=3.5, a1=5.95, e0=0.05, e1=0.95):
    """Calls make(point) for [n] points on the camera-facing surface around [center]."""
    rnd = random.Random(seed)
    pts = []
    for i in range(n):
        a = rnd.uniform(a0, a1); e = rnd.uniform(e0, e1)
        p, _ = surface_point(center, (math.cos(a) * math.cos(e), math.sin(a) * math.cos(e), math.sin(e)))
        if p is not None: pts.append(p)
    for p in pts: make(p)

def ground_patch(r, col, seed=7, h=0.0, sc_=(1, 1, 0.06)):
    """A soft mound of snow under a snowy item (no outline, so it melts into the snowy ground)."""
    o = blob((0, 0, h), r * 0.8, sc_, col, rough=0.12, seed=seed, sub=3, line=0, top=1.0, shade=0.94)
    o['ground'] = True
    return o

# ------------------------------------------------------------------ framing and render
def frame_and_render(name, elev=30.0, yaw=0.0, res=640, pad=1.08, aspect=None, margin=0.0, shoot=True):
    """Points the camera at everything built (elev degrees above the horizon, yaw about Z), fits it, renders.
    [margin] (world units) widens the fit on every side, for animations that move outside the first frame."""
    bpy.context.view_layer.update()
    rot = Matrix.Rotation(math.radians(yaw), 4, 'Z') @ Matrix.Rotation(math.radians(90 - elev), 4, 'X')
    rx = rot.to_3x3() @ Vector((1, 0, 0)); ry = rot.to_3x3() @ Vector((0, 1, 0)); rz = rot.to_3x3() @ Vector((0, 0, 1))
    xs, ys, pts = [], [], []
    dg = bpy.context.evaluated_depsgraph_get()
    for o in CUR:
        if o.type != 'MESH' or o.hide_render: continue
        oe = o.evaluated_get(dg)
        for c in oe.bound_box:
            p = oe.matrix_world @ Vector(c)
            xs.append(p.dot(rx)); ys.append(p.dot(ry)); pts.append(p)
    cx = (min(xs) + max(xs)) / 2; cy = (min(ys) + max(ys)) / 2
    w = max(xs) - min(xs) + 2 * margin; h = max(ys) - min(ys) + 2 * margin
    a = aspect if aspect else w / max(h, 1e-6)          # width / height of the picture
    rw, rh = (res, max(16, int(res / a))) if a >= 1 else (max(16, int(res * a)), res)
    # an orthographic camera's scale spans the larger side of the picture
    camd.ortho_scale = (max(w, h * rw / rh) if rw >= rh else max(h, w * rh / rw)) * pad
    centre = rx * cx + ry * cy
    CAM.matrix_world = Matrix.Translation(centre + rz * 60) @ rot
    camd.clip_end = 200
    sc.render.resolution_x, sc.render.resolution_y = rw, rh
    if shoot: shot(name)

def shot(name):
    """Renders with the camera as it is to <out>/<name>.png."""
    sc.render.filepath = os.path.join(OUT, name + '.png')
    bpy.ops.render.render(write_still=True)

def clear():
    for o in CUR:
        bpy.data.objects.remove(o, do_unlink=True)
    CUR.clear()

# ------------------------------------------------------------------ palette
BARK = '#8A5A3C'; BARK_D = '#6E4630'
LEAF = '#79BE5C'; LEAF_D = '#4E9A4A'; PINE = '#3F8A5A'; PINE_D = '#2F6E4C'
SNOW = '#F7FBFE'; ROCK = '#A3A9B0'; ROCK_D = '#7D858E'
SAND = '#E8C98A'; CLAY = '#C8643C'; STRAW = '#D9A84E'; WOOD = '#A8774E'; WOOD_D = '#7A5236'
CREAM = '#F3E6CC'; RED = '#D9534F'; WHITE = '#F5F2EA'; METAL = '#8C949C'; DARK = '#4A4E58'

ITEMS = {}
def item(name, **cam):
    def deco(f):
        ITEMS[name] = (f, cam); return f
    return deco

# ================================================================== trees
def conifer(h, r, tiers, snowy, seed, col=PINE, trunk_h=0.35):
    rnd = random.Random(seed)
    limb((0, 0, 0), (0, 0, trunk_h + 0.1), 0.09 * r / 0.6, 0.07 * r / 0.6, BARK)
    z = trunk_h
    for i in range(tiers):
        f = 1 - i / tiers
        rr = r * (0.45 + 0.55 * f)
        th = (h - trunk_h) / tiers * 1.55
        cone((0, 0, z), rr, rr * 0.08, th, col, verts=28, rot=(rnd.uniform(-3, 3), rnd.uniform(-3, 3), rnd.uniform(0, 60)))
        if snowy:
            cone((0, 0, z + th * 0.42), rr * 0.66, rr * 0.05, th * 0.62, SNOW, verts=28, top=1.0, shade=0.9)
            for k in range(4):
                a = rnd.uniform(0, 6.28)
                sphere((math.cos(a) * rr * 0.8, math.sin(a) * rr * 0.8, z + th * 0.12), rr * 0.17, (1, 1, 0.5), SNOW, seg=16, top=1.0, shade=0.9)
        z += th * 0.62
    if snowy: sphere((0, 0, z + 0.05), r * 0.12, (1, 1, 1.2), SNOW, seg=16, top=1.0)

@item('frost_pine_a')
def _(): conifer(2.6, 0.95, 4, True, 11); ground_patch(0.8, SNOW, h=0.02)
@item('frost_pine_b')
def _(): conifer(2.0, 0.8, 3, True, 12); ground_patch(0.7, SNOW, h=0.02)
@item('frost_pine_c')
def _(): conifer(1.4, 0.65, 3, True, 13, trunk_h=0.25); ground_patch(0.6, SNOW, h=0.02)
@item('tree_tall')
def _(): conifer(2.6, 0.9, 4, False, 14, col='#4F9B5E')
@item('xmas')
def _():
    cone((0, 0, 0), 0.32, 0.38, 0.38, '#C8643C', verts=24)
    ring((0, 0, 0.38), 0.37, 0.04, '#A84E2E')
    conifer(1.9, 0.75, 3, False, 15, col='#3F8A5A', trunk_h=0.42)
    rnd = random.Random(5)
    for i in range(14):
        z = rnd.uniform(0.62, 1.8); f = max(0.0, 1 - (z - 0.45) / 1.6)
        a = rnd.uniform(3.5, 5.9); rr = 0.75 * f + 0.06
        sphere((math.cos(a) * rr, math.sin(a) * rr, z), 0.055, (1, 1, 1), rnd.choice([RED, '#F2C14E', '#5E86B8', '#F7FBFE']), seg=12, line=0.008)
    for k in range(5):   # star
        a = k * 2 * math.pi / 5
        sphere((0.13 * math.cos(a + 1.57), -0.05, 2.25 + 0.13 * math.sin(a + 1.57)), 0.08, (1, 0.5, 1), '#F4C542', seg=10, line=0.008)
    sphere((0, -0.05, 2.25), 0.1, (1, 0.6, 1), '#F4C542', seg=12, line=0.008)
    ground_patch(0.6, SNOW, h=0.02)

def broadleaf(seed, trunk_h, canopy, col=LEAF, n=8, spread=0.55, flat=1.0, trunk_r=0.12):
    rnd = random.Random(seed)
    limb((0, 0, 0), (0, 0, trunk_h), trunk_r, trunk_r * 0.7, BARK)
    for s in (-1, 1):
        limb((0, 0, trunk_h * 0.65), (s * 0.28, 0.05, trunk_h + 0.15), trunk_r * 0.5, trunk_r * 0.3, BARK)
    blob((0, 0, trunk_h + canopy * 0.55), canopy, (1, 1, 0.82 * flat), col, rough=0.1, seed=seed)
    for i in range(n):
        a = 2 * math.pi * i / n + rnd.uniform(-0.3, 0.3)
        r = canopy * rnd.uniform(0.45, 0.6)
        z = trunk_h + canopy * (0.45 + rnd.uniform(-0.25, 0.4)) * flat
        blob((math.cos(a) * canopy * spread, math.sin(a) * canopy * spread, z), r, (1, 1, 0.85), col, rough=0.12, seed=seed + i)

@item('tree_round')
def _(): broadleaf(21, 0.9, 0.85)
@item('acacia')
def _():
    limb((0, 0, 0), (0.1, 0, 0.9), 0.1, 0.07, BARK)
    for dx, dy in ((-0.55, 0.1), (0.6, -0.1), (0.2, 0.35)):
        limb((0.1, 0, 0.8), (dx, dy, 1.35), 0.06, 0.035, BARK)
    rnd = random.Random(4)
    for i in range(9):
        a = rnd.uniform(0, 6.28); d = rnd.uniform(0.2, 0.95)
        blob((math.cos(a) * d * 1.1, math.sin(a) * d * 0.5, 1.42 + rnd.uniform(-0.05, 0.08)), rnd.uniform(0.35, 0.5), (1.4, 1.2, 0.32), '#9DB65A', rough=0.15, seed=i)
@item('dead_a')
def _(): dead_tree(31, False)
@item('dead_b')
def _(): dead_tree(32, True)

def dead_tree(seed, snowy):
    rnd = random.Random(seed)
    def branch(a, d, length, r, depth):
        b = Vector(a) + Vector(d).normalized() * length
        limb(a, b, r, r * 0.65, '#7A5A48')
        if snowy and d[2] < 0.9 and depth < 3:
            limb(Vector(a) + Vector((0, 0, r * 0.8)), b + Vector((0, 0, r * 0.55)), r * 0.55, r * 0.3, SNOW, top=1.0, shade=0.92, line=LINE[0] * 0.6)
        if depth == 0: return
        for k in range(3):
            nd = Vector(d) + Vector((rnd.uniform(-0.9, 0.9), rnd.uniform(-0.6, 0.6), rnd.uniform(0.1, 0.5)))
            branch(b, nd, length * rnd.uniform(0.55, 0.72), r * 0.62, depth - 1)
    branch((0, 0, 0), (0, 0, 1), 0.9, 0.12, 3)
    if snowy: ground_patch(0.55, SNOW, h=0.0)

@item('palm')
def _(): palm(41, 1.9, 7, '#6DB45A')
@item('v_palm')
def _(): palm(42, 2.6, 9, '#4F9B4A', coconuts=True)

def palm(seed, h, fronds, col, coconuts=True):
    rnd = random.Random(seed)
    n = 8; prev = Vector((0, 0, 0))
    for i in range(1, n + 1):
        t = i / n
        p = Vector((0.35 * t * t, 0, h * t))
        limb(prev, p, 0.11 - 0.035 * t, 0.11 - 0.035 * (t + 1 / n), '#A8814E')
        ring(p - Vector((0, 0, 0.02)), 0.1 - 0.03 * t, 0.018, '#8A6A3C', line=0)
        prev = p
    top = prev
    for i in range(fronds):
        a = 2 * math.pi * i / fronds + rnd.uniform(-0.2, 0.2)
        d = (math.cos(a), math.sin(a), rnd.uniform(0.15, 0.45))
        L = rnd.uniform(1.0, 1.25)
        pts = curve_pts(top, d, L, rnd.uniform(0.5, 0.8), n=12)
        widths = [0.06 + 0.32 * math.sin(math.pi * min(1, j / 11 * 1.1)) * (0.55 if j % 2 else 1.0) for j in range(12)]
        strip(pts, widths, col if i % 2 else LEAF_D)
    if coconuts:
        for k in range(3):
            a = k * 2.1
            sphere(top + Vector((0.12 * math.cos(a), 0.12 * math.sin(a), -0.12)), 0.09, (1, 1, 1), '#7A5236', seg=16)

@item('v_banana')
def _():
    limb((0, 0, 0), (0, 0, 1.0), 0.14, 0.1, '#7FA35A')
    rnd = random.Random(8)
    for i in range(7):
        a = 2 * math.pi * i / 7 + rnd.uniform(-0.2, 0.2)
        d = (math.cos(a), math.sin(a), rnd.uniform(0.9, 1.6))
        pts = curve_pts((0, 0, 0.85 + rnd.uniform(0, 0.2)), d, rnd.uniform(1.0, 1.3), rnd.uniform(0.5, 0.9), n=10)
        strip(pts, [0.05] + [0.42 * math.sin(math.pi * (j / 9) ** 0.8) + 0.04 for j in range(1, 10)], '#5DAA4E' if i % 2 else '#4C9445')
    # a hanging bunch with the purple flower bud
    limb((0.08, -0.05, 0.95), (0.25, -0.2, 0.55), 0.025, 0.02, '#6D8F45')
    for k in range(9):
        z = 0.85 - 0.06 * (k // 3); a = (k % 3) * 2.1
        sphere((0.17 + 0.07 * math.cos(a), -0.12 + 0.07 * math.sin(a), z), 0.05, (1, 1, 1.9), '#C9D45A', seg=12, line=0.008)
    sphere((0.25, -0.2, 0.5), 0.07, (1, 1, 1.5), '#8E3E5E', seg=14)

@item('v_bamboo')
def _():
    rnd = random.Random(9)
    for i in range(8):
        x, y = rnd.uniform(-0.35, 0.35), rnd.uniform(-0.25, 0.25)
        h = rnd.uniform(2.0, 2.9); lean = Vector((rnd.uniform(-0.15, 0.15), rnd.uniform(-0.1, 0.1), 1)).normalized()
        segs = 6; prev = Vector((x, y, 0))
        for s in range(segs):
            p = Vector((x, y, 0)) + lean * h * (s + 1) / segs
            limb(prev, p, 0.05, 0.045, '#7DB04E' if i % 2 else '#8DBE5A')
            ring(p, 0.05, 0.012, '#5E8C3A', line=0)
            prev = p
        for k in range(5):
            base = Vector((x, y, 0)) + lean * h * rnd.uniform(0.45, 1.0)
            a = rnd.uniform(0, 6.28)
            strip(curve_pts(base, (math.cos(a), math.sin(a), 0.2), 0.45, 0.18, n=6), [0.02, 0.09, 0.1, 0.08, 0.05, 0.01], '#5E9E45', thick=0.01, line=0.008)

@item('jungle')
def _():
    rnd = random.Random(10)
    for i in range(8):
        a = 2 * math.pi * i / 8 + rnd.uniform(-0.2, 0.2)
        stem_top = Vector((math.cos(a) * 0.35, math.sin(a) * 0.35, rnd.uniform(0.6, 1.1)))
        limb((0, 0, 0), stem_top, 0.025, 0.02, '#5E8C3A')
        d = (math.cos(a), math.sin(a), rnd.uniform(0.3, 0.9))
        pts = curve_pts(stem_top, d, rnd.uniform(0.7, 0.9), 0.35, n=10)
        strip(pts, [0.05] + [0.62 * math.sin(math.pi * (j / 9) ** 0.75) + 0.05 for j in range(1, 10)], '#3F8F4A' if i % 2 else '#52A456')
@item('fern')
def _():
    rnd = random.Random(12)
    for i in range(11):
        a = 2 * math.pi * i / 11 + rnd.uniform(-0.15, 0.15)
        d = (math.cos(a), math.sin(a), rnd.uniform(0.6, 1.2))
        pts = curve_pts((0, 0, 0.05), d, rnd.uniform(0.8, 1.05), rnd.uniform(0.35, 0.6), n=14)
        strip(pts, [0.04 + 0.22 * math.sin(math.pi * j / 13) * (0.35 if j % 2 else 1.0) for j in range(14)], '#5FAE55' if i % 2 else '#4B9A4A', thick=0.012)

# ================================================================== bushes, grass, flowers
def bush(seed, r, col, n=6, flat=0.8):
    rnd = random.Random(seed)
    blob((0, 0, r * 0.6), r, (1.1, 1, flat), col, rough=0.1, seed=seed, flat_bottom=True)
    for i in range(n):
        a = 2 * math.pi * i / n + rnd.uniform(-0.3, 0.3)
        blob((math.cos(a) * r * 0.65, math.sin(a) * r * 0.55, r * rnd.uniform(0.45, 0.9)), r * rnd.uniform(0.45, 0.6), (1, 1, 0.9), col, rough=0.14, seed=seed + i)

@item('bush')
def _():
    bush(51, 0.55, '#6DB45A', n=7)
    rnd = random.Random(2)
    dots_on((0, 0, 0.5), 15, 2, lambda p: flower(p + Vector((0, -0.01, 0.0)), 0.06, rnd.choice(['#F28FB0', '#F7D25C', '#F5F2EA'])))
@item('snow_bush')
def _():
    n0 = len(CUR)
    bush(52, 0.6, '#5E9C7A', n=7)
    for o in CUR[n0:]: snow_top(o, cut=0.74)
    ground_patch(0.85, SNOW, h=0.0)
@item('frost_shrub')
def _():
    n0 = len(CUR)
    bush(53, 0.45, '#7FB8A0', n=5, flat=0.65)
    for o in CUR[n0:]: snow_top(o, cut=0.76)
    for x in (-0.18, 0.2):
        sphere((x, -0.38, 0.35), 0.06, (1, 1, 1), '#D9534F', seg=12)
@item('v_vbush')
def _():
    bush(54, 0.6, '#3F7F45', n=8)
    dots_on((0, 0, 0.5), 24, 4, lambda p: sphere(p, 0.035, (1, 1, 1), '#B48AD6', seg=8, line=0.006))

def grass(seed, n, col, col2, h=0.6, spread=0.35, width=0.07):
    rnd = random.Random(seed)
    for i in range(n):
        a = rnd.uniform(0, 6.28); r0 = rnd.uniform(0, spread * 0.5)
        base = Vector((math.cos(a) * r0, math.sin(a) * r0, 0))
        d = (math.cos(a) * rnd.uniform(0.15, 0.5), math.sin(a) * rnd.uniform(0.15, 0.5), 1)
        hh = h * rnd.uniform(0.6, 1.1)
        strip(curve_pts(base, d, hh, hh * 0.25, n=6), [width, width * 0.85, width * 0.7, width * 0.5, width * 0.3, 0.005], col if i % 2 else col2, thick=0.012, line=LINE[0] * 0.6)

@item('grass_tuft')
def _(): grass(61, 14, '#E2B84E', '#C99B3A', h=0.7)
@item('v_vgrass')
def _(): grass(62, 26, '#2F6E3A', '#3F8A45', h=0.85, spread=0.55, width=0.08)
@item('dry_bush')
def _():
    rnd = random.Random(63)
    for i in range(7):
        a = rnd.uniform(0, 6.28)
        mid = Vector((math.cos(a) * 0.2, math.sin(a) * 0.2, 0.35))
        tip = Vector((math.cos(a) * 0.45, math.sin(a) * 0.45, rnd.uniform(0.6, 0.85)))
        limb((0, 0, 0), mid, 0.03, 0.022, '#B08A4E'); limb(mid, tip, 0.022, 0.01, '#B08A4E')
        limb(mid, mid + Vector((math.cos(a + 1) * 0.2, math.sin(a + 1) * 0.2, 0.25)), 0.015, 0.008, '#B08A4E')

def flower(loc, r, col, center='#F4C542'):
    loc = Vector(loc)
    for k in range(5):
        a = 2 * math.pi * k / 5
        sphere(loc + Vector((math.cos(a) * r, math.sin(a) * r * 0.6, 0.0)), r * 0.75, (1, 1, 0.5), col, seg=10, line=0.006)
    sphere(loc + Vector((0, -0.01, 0.02)), r * 0.5, (1, 1, 0.6), center, seg=10, line=0.004)

@item('flowers')
def _():
    for (x, y, h, c) in ((-0.3, 0.05, 0.55, '#F28FB0'), (0.0, -0.05, 0.75, WHITE), (0.3, 0.05, 0.6, '#B48AD6'), (-0.12, 0.2, 0.4, '#F7D25C')):
        limb((x, y, 0), (x, y, h), 0.018, 0.015, '#5E9E45')
        strip(curve_pts((x, y, h * 0.3), (0.6, -0.2, 0.6), 0.22, 0.05, n=5), [0.02, 0.08, 0.08, 0.05, 0.01], '#6DB45A', thick=0.01, line=0.006)
        flower((x, y, h), 0.09, c)

@item('cactus')
def _():
    G = '#5FA05A'
    limb((0, 0, 0), (0, 0, 1.6), 0.24, 0.22, G, verts=24); sphere((0, 0, 1.6), 0.22, (1, 1, 0.9), G, seg=24)
    for s, z, h in ((-1, 0.6, 0.55), (1, 0.85, 0.45)):
        limb((0, 0, z), (s * 0.42, 0, z), 0.12, 0.12, G); sphere((s * 0.42, 0, z), 0.12, (1, 1, 1), G, seg=16)
        limb((s * 0.42, 0, z), (s * 0.42, 0, z + h), 0.12, 0.11, G); sphere((s * 0.42, 0, z + h), 0.11, (1, 1, 1), G, seg=16)
    for k in range(6):   # ribs
        a = 2 * math.pi * k / 6
        limb((math.cos(a) * 0.235, math.sin(a) * 0.235, 0.05), (math.cos(a) * 0.215, math.sin(a) * 0.215, 1.62), 0.012, 0.012, '#4A8A48', line=0)
    flower((0, -0.05, 1.83), 0.08, '#F28FB0')
    # a little one beside it
    sphere((0.5, -0.25, 0.22), 0.22, (1, 1, 1.05), G, seg=24); flower((0.5, -0.3, 0.46), 0.05, '#F7D25C')
@item('cactus_ball')
def _():
    G = '#5FA05A'
    sphere((0, 0, 0.32), 0.36, (1, 1, 0.9), G, seg=32)
    for k in range(10):
        a = 2 * math.pi * k / 10
        ring((0, 0, 0.32), 0.36, 0.012, '#4A8A48', sc_=(1, 1, 0.9), rot=(90, 0, math.degrees(a)), line=0, seg=32)
    flower((0, -0.05, 0.66), 0.07, '#F28FB0')

# ================================================================== rocks and land
@item('boulder')
def _():
    blob((0, 0, 0.5), 0.75, (1.15, 1, 0.75), ROCK, rough=0.16, seed=71, flat_bottom=True)
    blob((0.75, -0.35, 0.18), 0.3, (1.1, 1, 0.7), ROCK, rough=0.2, seed=72, flat_bottom=True)
@item('rock_a')
def _():
    snow_top(blob((0, 0, 0.42), 0.7, (1.2, 1, 0.6), ROCK, rough=0.2, seed=73, flat_bottom=True), cut=0.78)
    blob((-0.75, -0.3, 0.12), 0.22, (1.2, 1, 0.7), ROCK_D, rough=0.2, seed=74)
@item('rock_b')
def _():
    blob((0, 0, 0.42), 0.7, (1.2, 1, 0.62), '#8E948C', rough=0.2, seed=75, flat_bottom=True)
    rnd = random.Random(6)
    for i in range(5):
        a = rnd.uniform(0, 6.28)
        blob((math.cos(a) * 0.4, math.sin(a) * 0.3, 0.7 + rnd.uniform(-0.1, 0.05)), 0.28, (1.3, 1, 0.3), '#8FA84E', rough=0.2, seed=i)
@item('mesa')
def _():
    cols = ['#D9774A', '#E58F5C', '#C8643C', '#E8A06A']
    rnd = random.Random(76)
    z = 0
    for i, (r, h) in enumerate(((1.0, 0.45), (0.9, 0.42), (0.78, 0.4), (0.66, 0.34))):
        blob((rnd.uniform(-0.06, 0.06), rnd.uniform(-0.04, 0.04), z + h / 2), 1.0, (r * 1.15, r * 0.85, h * 0.7), cols[i], rough=0.1, seed=80 + i, sub=2, smooth=False)
        z += h * 0.88
    blob((0.95, -0.5, 0.22), 0.3, (1, 1, 0.8), '#C8643C', rough=0.2, seed=77)
@item('cliff')
def _():
    rnd = random.Random(78)
    for i, (x, y, h, r) in enumerate(((-0.5, 0.1, 1.5, 0.45), (0.1, 0.2, 1.9, 0.5), (0.6, 0.0, 1.3, 0.45), (-0.1, -0.3, 0.9, 0.42), (0.55, -0.35, 0.6, 0.35))):
        snow_top(blob((x, y, h / 2), 1.0, (r, r * 0.85, h / 2), '#8C939C' if i % 2 else '#A3AAB2', rough=0.12, seed=90 + i, sub=2, smooth=False), cut=0.72)
    ground_patch(1.1, SNOW)
@item('ice')
def _():
    rnd = random.Random(79)
    for i in range(7):
        a = rnd.uniform(-35, 35); b = rnd.uniform(-30, 30); h = rnd.uniform(0.7, 1.6); r = rnd.uniform(0.1, 0.18)
        x, y = rnd.uniform(-0.4, 0.4), rnd.uniform(-0.2, 0.2)
        o = cone((0, 0, 0), r, r, h, '#BFE6F5', verts=6, smooth=False, top=1.12, shade=0.8)
        t = cone((0, 0, h), r, 0.0, r * 2.2, '#D8F2FB', verts=6, smooth=False, top=1.1, shade=0.8)
        for ob in (o, t):
            ob.rotation_euler = (math.radians(b), math.radians(a), 0)
            ob.location = Vector((x, y, 0)) + (Matrix.Rotation(math.radians(b), 3, 'X') @ Matrix.Rotation(math.radians(a), 3, 'Y') @ (ob.location))
    ground_patch(0.75, SNOW)
@item('logs')
def _():
    rnd = random.Random(81)
    rows = [(-0.6, 0.6, 0.0, 4), (-0.45, 0.45, 0.27, 3), (-0.3, 0.3, 0.52, 2)]
    for x0, x1, z, n in rows:
        for k in range(n):
            x = x0 + (x1 - x0) * k / max(1, n - 1) if n > 1 else 0
            limb((x, -0.3, z + 0.15), (x, 0.3, z + 0.15), 0.14, 0.14, BARK)
            sphere((x, -0.305, z + 0.15), 0.135, (1, 0.1, 1), '#E8C48E', seg=20, line=0)
            ring((x, -0.315, z + 0.15), 0.07, 0.008, '#B88A5A', rot=(90, 0, 0), line=0)
    # an axe in a stump
    limb((0.95, -0.2, 0), (0.95, -0.2, 0.3), 0.18, 0.18, BARK); sphere((0.95, -0.2, 0.3), 0.175, (1, 1, 0.08), '#E8C48E', seg=20, line=0)
    limb((0.95, -0.2, 0.32), (1.2, -0.25, 0.75), 0.02, 0.02, '#A8774E')
    box((0.97, -0.21, 0.36), (0.18, 0.03, 0.12), METAL, bevel=0.01, rot=(0, -30, 0))
    ground_patch(1.1, SNOW)
@item('lake', elev=55)
def _():
    blob((0, 0, 0), 1.6, (1.5, 1, 0.05), '#F7FBFE', rough=0.1, seed=82, top=1.0)
    blob((0, 0, 0.03), 1.4, (1.5, 1, 0.04), '#BFE3F2', rough=0.08, seed=83, top=1.05)
    rnd = random.Random(84)
    for i in range(7):   # cracks
        a = rnd.uniform(0, 6.28); p = Vector((rnd.uniform(-1, 1), rnd.uniform(-0.6, 0.6), 0.1))
        q = p + Vector((math.cos(a), math.sin(a), 0)) * rnd.uniform(0.3, 0.6)
        limb(p, q, 0.012, 0.012, '#8FC4DA', line=0, flat=True)
    for i in range(3):
        blob((rnd.uniform(-1.5, 1.5), rnd.uniform(-0.8, 0.8), 0.06), 0.2, (1.4, 1, 0.3), SNOW, rough=0.2, seed=i, top=1.0)

# ================================================================== village (Paddylands)
def thatch_roof(cx, cy, z, w, d, h, col, overhang=0.15):
    """A gable roof as a prism along X."""
    W = w / 2 + overhang; D = d / 2 + overhang
    V = [(cx - W, cy - D, z), (cx + W, cy - D, z), (cx + W, cy + D, z), (cx - W, cy + D, z), (cx - W, cy, z + h), (cx + W, cy, z + h)]
    F = [(0, 1, 5, 4), (3, 4, 5, 2), (0, 4, 3), (1, 2, 5), (0, 3, 2, 1)]
    o = mesh_obj(V, F, col, smooth=False)
    b = o.modifiers.new('bv', 'BEVEL'); b.width = 0.03; b.segments = 2
    o.modifiers.move(1, 0)
    return o

def window(loc, w, h, glow=True, rot=(0, 0, 0)):
    box(loc, (w, 0.03, h), '#FFC46B' if glow else '#3E4A57', bevel=0.01, rot=rot, flat=glow, line=0.012)

@item('v_house_a', yaw=-30)
def _():
    box((0, 0, 0.45), (1.5, 1.0, 0.9), '#C98E5A', bevel=0.05)     # mud walls
    box((0, 0, 0.06), (1.62, 1.12, 0.12), '#A9744A', bevel=0.03)  # plinth
    thatch_roof(0, 0, 0.88, 1.5, 1.0, 0.75, '#4F5F8A', overhang=0.22)
    box((0.25, -0.505, 0.36), (0.32, 0.03, 0.6), '#6B4A32', bevel=0.01)   # door
    window((-0.35, -0.505, 0.5), 0.26, 0.24, glow=True)
    box((-0.35, -0.53, 0.37), (0.34, 0.06, 0.04), WOOD_D, bevel=0.01)
    for x in (-0.6, 0.6): limb((x, -0.55, 0), (x, -0.55, 0.9), 0.03, 0.03, WOOD_D)
@item('v_house_b', yaw=-30)
def _():
    box((0, 0.1, 0.42), (1.5, 0.9, 0.84), '#E2C49A', bevel=0.04)
    box((0, 0, 0.05), (1.7, 1.25, 0.1), '#B89470', bevel=0.03)
    thatch_roof(0, 0.0, 0.82, 1.5, 1.15, 0.55, '#C2553A', overhang=0.2)
    for i in range(7):   # tile rows
        y = -0.77 + i * 0.13
        limb((-0.97, y, 0.82 + (y + 0.77) * 0.55 / 0.775), (0.97, y, 0.82 + (y + 0.77) * 0.55 / 0.775), 0.012, 0.012, '#9E4230', line=0)
    for x in (-0.65, 0.0, 0.65): limb((x, -0.52, 0.1), (x, -0.52, 0.85), 0.04, 0.04, WOOD_D)   # veranda posts
    box((0.3, -0.36, 0.35), (0.3, 0.03, 0.55), '#7A5236', bevel=0.01)
    window((-0.35, -0.36, 0.48), 0.28, 0.22, glow=False)
@item('v_court', yaw=-30)
def _():
    box((0, 0, 0.04), (2.2, 1.7, 0.08), '#D6B88A', bevel=0.03)     # courtyard floor
    for (x, y, w, d) in ((0, 0.8, 2.2, 0.12), (-1.05, 0, 0.12, 1.7), (1.05, 0, 0.12, 1.7), (-0.65, -0.8, 0.9, 0.12), (0.65, -0.8, 0.9, 0.12)):
        box((x, y, 0.22), (w, d, 0.36), '#B8845A', bevel=0.03)
    box((0.35, 0.35, 0.4), (1.1, 0.7, 0.8), '#C98E5A', bevel=0.04)
    thatch_roof(0.35, 0.35, 0.78, 1.1, 0.7, 0.5, '#8A6A3C', overhang=0.15)
    box((0.35, -0.005, 0.32), (0.26, 0.03, 0.5), '#6B4A32', bevel=0.01)
    sphere((-0.6, -0.35, 0.15), 0.14, (1, 1, 1.1), CLAY, seg=16); sphere((-0.4, -0.45, 0.12), 0.11, (1, 1, 1.1), '#B0573A', seg=16)
    blob((-0.6, 0.35, 0.18), 0.22, (1, 1, 0.8), '#5E9E45', seed=3)
@item('v_haystack')
def _():
    blob((0, 0, 0.55), 0.85, (1, 1, 0.95), STRAW, rough=0.06, seed=91, flat_bottom=True)
    rnd = random.Random(92)
    for i in range(40):   # straw strands
        a = rnd.uniform(0, 6.28); z = rnd.uniform(0.1, 1.2); rr = 0.85 * math.sqrt(max(0.05, 1 - ((z - 0.55) / 0.85) ** 2))
        p = Vector((math.cos(a) * rr, math.sin(a) * rr, z))
        limb(p, p + Vector((math.cos(a) * 0.1, math.sin(a) * 0.1, -0.18)), 0.008, 0.004, '#B88A3A', line=0)
    limb((0, 0, 1.2), (0, 0, 1.75), 0.03, 0.025, WOOD_D)
    cone((0, 0, 1.25), 0.22, 0.03, 0.3, '#C9973E')
@item('v_well', yaw=-20)
def _():
    limb((0, 0, 0), (0, 0, 0.55), 0.5, 0.5, '#9A8A78', verts=24)
    ring((0, 0, 0.55), 0.46, 0.07, '#B0A090')
    rnd = random.Random(93)
    for k in range(12):   # stone blocks
        a = 2 * math.pi * k / 12; z = 0.15 + 0.2 * (k % 2)
        box((math.cos(a) * 0.5, math.sin(a) * 0.5, z), (0.22, 0.04, 0.13), '#867868', bevel=0.02, rot=(0, 0, math.degrees(a) + 90), line=0.008)
    sphere((0, 0, 0.5), 0.42, (1, 1, 0.02), '#2E4E5E', seg=24, line=0, flat=True)
    for x in (-0.48, 0.48): limb((x, 0, 0.5), (x, 0, 1.3), 0.04, 0.04, WOOD_D)
    limb((-0.55, 0, 1.25), (0.55, 0, 1.25), 0.05, 0.05, WOOD)
    limb((0, 0, 1.22), (0, 0, 0.85), 0.008, 0.008, '#C9B48A', line=0)
    limb((0, 0, 0.72), (0, 0, 0.85), 0.08, 0.1, '#7A5236')
@item('v_pots')
def _():
    rnd = random.Random(94)
    spots = [(-0.4, 0.1, 0.42), (0.1, 0.25, 0.48), (0.5, 0.05, 0.38), (-0.15, -0.3, 0.36), (0.3, -0.35, 0.32), (0.0, 0.0, 0.3)]
    for i, (x, y, r) in enumerate(spots):
        z = r if i < 5 else 0.85
        c = rnd.choice([CLAY, '#B0573A', '#A84E2E'])
        sphere((x, y, z), r, (1, 1, 0.9), c, seg=28)
        limb((x, y, z + r * 0.75), (x, y, z + r * 1.05), r * 0.38, r * 0.45, c)
        ring((x, y, z + r * 1.05), r * 0.45, r * 0.07, c, shade=0.75)
        ring((x, y, z + r * 0.15), r * 0.92, r * 0.03, '#E8C48E', line=0)
@item('v_bed', yaw=-25)
def _():
    for x in (-0.55, 0.55):
        for y in (-0.95, 0.95):
            limb((x, y, 0), (x, y, 0.45), 0.05, 0.05, WOOD_D)
            limb((x, y, 0.45), (x, y, 1.5), 0.015, 0.015, '#E8E2D4')   # net poles
    for y in (-0.92, 0.92): limb((-0.6, y, 0.4), (0.6, y, 0.4), 0.05, 0.05, WOOD)
    for x in (-0.57, 0.57): limb((x, -1.0, 0.4), (x, 1.0, 0.4), 0.05, 0.05, WOOD)
    box((0, 0, 0.42), (1.08, 1.82, 0.03), '#E2CFA0', bevel=0.0, stripes=('#C9B07A', 6.0, 'X'))   # woven rope
    box((0, 0, 0.95), (1.14, 1.94, 1.1), '#F2EEE6', bevel=0.02, alpha=0.32, line=0.006)
    sphere((0, 0.65, 0.5), 0.18, (1.6, 0.8, 0.4), WHITE, seg=16)
@item('v_cart', yaw=-35)
def _():
    box((0, 0, 0.6), (1.2, 0.9, 0.08), WOOD, bevel=0.02)
    for y in (-0.47, 0.47): box((0, y, 0.75), (1.2, 0.05, 0.25), WOOD_D, bevel=0.01)
    for y in (-0.55, 0.55):
        ring((0.05, y, 0.45), 0.4, 0.05, WOOD_D, rot=(90, 0, 0))
        for k in range(8):
            a = 2 * math.pi * k / 8
            limb((0.05, y, 0.45), (0.05 + math.cos(a) * 0.38, y, 0.45 + math.sin(a) * 0.38), 0.022, 0.022, WOOD_D, line=0.008)
        sphere((0.05, y, 0.45), 0.07, (1, 1.4, 1), WOOD_D, seg=12)
    limb((0.05, -0.6, 0.45), (0.05, 0.6, 0.45), 0.03, 0.03, WOOD_D)
    for y in (-0.25, 0.25): limb((0.6, y, 0.6), (1.6, y * 0.4, 0.42), 0.04, 0.035, WOOD)
    limb((1.45, -0.25, 0.47), (1.45, 0.25, 0.47), 0.035, 0.035, WOOD)
    blob((-0.2, 0, 0.78), 0.35, (1.3, 1, 0.45), STRAW, rough=0.1, seed=5)
@item('v_paddy', elev=40, yaw=-20)
def _():
    box((0, 0, 0.06), (2.4, 2.0, 0.12), '#8C6A44', bevel=0.06)           # bund
    box((0, 0, 0.1), (2.15, 1.75, 0.06), '#6FA8A0', bevel=0.02, top=1.08)  # water
    rnd = random.Random(95)
    for i in range(6):
        for j in range(7):
            x = -0.9 + i * 0.36 + rnd.uniform(-0.03, 0.03); y = -0.75 + j * 0.25
            for k in range(4):
                a = k * 1.57 + rnd.uniform(-0.3, 0.3)
                strip(curve_pts((x, y, 0.12), (math.cos(a) * 0.3, math.sin(a) * 0.3, 1), 0.28, 0.06, n=4), [0.04, 0.035, 0.02, 0.004], '#7CC55A' if k % 2 else '#5FAE4A', thick=0.008, line=0.005)

# ================================================================== winter and wild props
@item('snowman')
def _():
    sphere((0, 0, 0.42), 0.45, (1, 1, 0.92), SNOW, seg=40, top=1.0, shade=0.88)
    sphere((0, 0, 1.0), 0.33, (1, 1, 0.95), SNOW, seg=40, top=1.0, shade=0.88)
    sphere((0, 0, 1.45), 0.25, (1, 1, 1), SNOW, seg=40, top=1.0, shade=0.88)
    ring((0, 0, 1.25), 0.24, 0.07, RED, sc_=(1, 1, 0.8))
    sphere((0.12, -0.3, 1.1), 1.0, (0.06, 0.03, 0.18), RED, seg=16)
    cone((0, -0.23, 1.45), 0.045, 0.0, 0.22, '#F08A3C', rot=(90, 0, 0))
    for x in (-0.09, 0.09): sphere((x, -0.22, 1.53), 0.03, (1, 1, 1), '#2B2220', seg=10, line=0)
    for z in (0.92, 1.05, 1.18): sphere((0, -0.32, z - 0.05), 0.03, (1, 1, 1), '#2B2220', seg=10, line=0)
    limb((0, 0, 1.66), (0, 0, 1.68), 0.3, 0.3, DARK); limb((0, 0, 1.68), (0, 0, 1.95), 0.18, 0.17, DARK)
    ring((0, 0, 1.72), 0.18, 0.025, RED, line=0)
    for s in (-1, 1):
        limb((s * 0.3, 0, 1.05), (s * 0.7, 0.05, 1.35), 0.025, 0.018, '#7A5A48')
        limb((s * 0.6, 0.04, 1.27), (s * 0.72, 0.05, 1.2), 0.015, 0.01, '#7A5A48')
    ground_patch(0.7, SNOW)
@item('birdhouse')
def _():
    limb((0, 0, 0), (0, 0, 1.2), 0.05, 0.05, WOOD_D)
    box((0, 0, 1.4), (0.42, 0.38, 0.42), '#CFE6DA', bevel=0.03)
    thatch_roof(0, 0, 1.6, 0.42, 0.38, 0.25, '#5E9C7A', overhang=0.08).rotation_euler = (0, 0, math.radians(90))
    sphere((0, -0.2, 1.42), 0.06, (1, 0.3, 1), '#2E3A44', seg=14, line=0)
    limb((0, -0.2, 1.33), (0, -0.3, 1.33), 0.012, 0.012, WOOD_D, line=0)
    snow_top(CUR[2], cut=0.55)
    ground_patch(0.4, SNOW)
@item('campfire')
def _():
    for k in range(9):
        a = 2 * math.pi * k / 9
        blob((math.cos(a) * 0.55, math.sin(a) * 0.45, 0.07), 0.12, (1.2, 1, 0.7), '#9AA0A8', seed=k)
    for a in (20, 80, 140):
        r = math.radians(a)
        limb((math.cos(r) * 0.42, math.sin(r) * 0.32, 0.05), (-math.cos(r) * 0.08, -math.sin(r) * 0.06, 0.65), 0.07, 0.06, BARK)
        limb((-math.cos(r) * 0.42, -math.sin(r) * 0.32, 0.05), (math.cos(r) * 0.08, math.sin(r) * 0.06, 0.65), 0.07, 0.06, BARK_D)
    sphere((0, 0, 0.32), 0.17, (1, 1, 1.9), '#F08A3C', seg=16, flat=True, line=0.012)
    cone((0, 0, 0.5), 0.15, 0.0, 0.3, '#F08A3C', verts=16, flat=True, line=0.012)
    sphere((0.02, -0.06, 0.26), 0.1, (1, 1, 1.8), '#F7D25C', seg=14, flat=True, line=0)
    sphere((-0.17, 0.02, 0.22), 0.08, (1, 1, 2.0), '#F4A24C', seg=12, flat=True, line=0.01)
    sphere((0.16, 0.0, 0.2), 0.07, (1, 1, 2.0), '#F4A24C', seg=12, flat=True, line=0.01)
@item('tent', yaw=-30)
def _():
    thatch_roof(0, 0, 0.0, 1.1, 1.4, 1.2, '#C9A26E', overhang=0.0).rotation_euler = (0, 0, math.radians(90))
    V = [(-0.3, -0.71, 0.0), (0.0, -0.71, 1.0), (0.05, -0.71, 0.0)]
    mesh_obj([(-0.35, -0.72, 0.02), (0.0, -0.72, 0.9), (0.15, -0.72, 0.02)], [(0, 1, 2)], '#5A4636', line=0, smooth=False, flat=True)
    for y in (-0.75, 0.75): limb((0, y, 0), (0, y, 1.35), 0.03, 0.03, WOOD_D)
@item('lamp')
def _():
    limb((0, 0, 0), (0, 0, 0.12), 0.16, 0.16, DARK)
    limb((0, 0, 0.12), (0, 0, 1.7), 0.05, 0.04, '#5A6470')
    pts = [Vector((0.25 * (1 - math.cos(t * math.pi / 2)), 0, 1.7 + 0.25 * math.sin(t * math.pi / 2))) for t in [i / 6 for i in range(7)]]
    for a, b in zip(pts, pts[1:]): limb(a, b, 0.04, 0.04, '#5A6470')
    cone((0.25, 0, 1.75), 0.14, 0.05, 0.14, '#5A6470', rot=(180, 0, 0))
    sphere((0.25, 0, 1.68), 0.08, (1, 1, 0.7), '#FFF3C4', seg=16, flat=True, line=0.01)
@item('pole')
def _():
    limb((0, 0, 0), (0, 0, 2.6), 0.08, 0.06, '#8A6A50')
    for z, w in ((2.35, 0.7), (2.05, 0.55)):
        box((0, 0, z), (w * 2, 0.08, 0.08), '#7A5A44', bevel=0.01)
        for x in (-w * 0.85, -w * 0.3, w * 0.3, w * 0.85):
            limb((x, 0, z + 0.04), (x, 0, z + 0.14), 0.03, 0.025, '#9FC4C8')
    limb((0, 0, 1.0), (0.35, 0, 0.0), 0.02, 0.02, '#5A5A5A', line=0)
@item('sign')
def _():
    limb((0, 0, 0), (0, 0, 1.5), 0.045, 0.045, '#9AA0A8')
    sphere((0, -0.04, 1.75), 0.38, (1, 0.08, 1), '#B8BEC6', seg=32)
    ring((0, -0.07, 1.75), 0.34, 0.025, '#E5484D', rot=(90, 0, 0))

@item('container', yaw=-25)
def _():
    box((0, 0, 0.55), (2.4, 1.0, 1.1), '#7E8A96', bevel=0.03, stripes=('#6E7A86', 6.0, 'X'))
    box((-1.2, 0, 0.55), (0.04, 1.02, 1.12), '#5E6A76', bevel=0.01)
    for y in (-0.25, 0.25): limb((-1.23, y, 0.15), (-1.23, y, 0.95), 0.02, 0.02, '#4A5460', line=0.006)
    blob((0.6, 0.1, 1.05), 0.35, (2.4, 1.2, 0.15), '#B8724A', rough=0.3, seed=2, line=0)   # rust
    snow_top(CUR[0], cut=0.97, grow=1.01)
@item('crane', yaw=-30)
def _():
    Y = '#F2C14E'
    for x, y in ((-0.5, -0.4), (0.5, -0.4), (-0.5, 0.4), (0.5, 0.4)):
        limb((x, y, 0), (x * 0.4, y * 0.4, 2.2), 0.05, 0.04, Y)
    for z in (0.7, 1.4):
        f = 1 - 0.6 * z / 2.2
        for (a, b) in (((-0.5, -0.4), (0.5, -0.4)), ((0.5, -0.4), (0.5, 0.4)), ((0.5, 0.4), (-0.5, 0.4)), ((-0.5, 0.4), (-0.5, -0.4))):
            limb((a[0] * f, a[1] * f, z), (b[0] * f, b[1] * f, z), 0.025, 0.025, Y)
    box((0, 0, 2.3), (0.5, 0.45, 0.35), '#E8A23C', bevel=0.04)
    limb((-0.6, 0, 2.5), (1.9, 0, 2.5), 0.06, 0.05, Y)
    limb((1.7, 0, 2.47), (1.7, 0, 1.2), 0.008, 0.008, DARK, line=0)
    box((1.7, 0, 1.12), (0.18, 0.18, 0.16), DARK, bevel=0.02)
    box((-0.55, 0, 2.42), (0.25, 0.3, 0.25), '#6A6A6A', bevel=0.02)
@item('tank')
def _():
    for x, y in ((-0.4, -0.4), (0.4, -0.4), (-0.4, 0.4), (0.4, 0.4)):
        limb((x, y, 0), (x * 0.7, y * 0.7, 1.4), 0.04, 0.035, WOOD_D)
    for z in (0.45, 0.9):
        f = 1 - 0.3 * z / 1.4
        limb((-0.4 * f, -0.4 * f, z), (0.4 * f, -0.4 * f, z), 0.025, 0.025, WOOD_D)
        limb((-0.4 * f, -0.4 * f, z), (0.4 * f, -0.4 * f, z + 0.42), 0.02, 0.02, WOOD_D)
    limb((0, 0, 1.4), (0, 0, 2.1), 0.55, 0.55, '#E8DCC2', verts=32, stripes=('#D6C8AA', 5.0, 'X'))
    cone((0, 0, 2.1), 0.6, 0.05, 0.4, '#8C949C')
    snow_top([o for o in CUR if o.type == 'MESH'][-1], cut=0.35)
    ring((0, 0, 1.42), 0.6, 0.03, WOOD_D)
@item('turbine')
def _():
    limb((0, 0, 0), (0, 0, 3.4), 0.14, 0.06, WHITE, verts=24)
    box((0, 0.05, 3.45), (0.2, 0.45, 0.2), WHITE, bevel=0.06)
    sphere((0, -0.2, 3.45), 0.09, (1, 1.2, 1), WHITE, seg=16)
    for k in range(3):
        a = 90 + 120 * k + 15
        d = Vector((math.cos(math.radians(a)), 0, math.sin(math.radians(a))))
        sphere(Vector((0, -0.24, 3.45)) + d * 0.78, 1.0, (0.1, 0.035, 0.78), WHITE, seg=24, rot=(0, 90 - a, 0))
@item('lighthouse')
def _():
    limb((0, 0, 0), (0, 0, 2.6), 0.42, 0.3, RED, verts=32, stripes=(WHITE, 1.6))
    ring((0, 0, 2.62), 0.38, 0.05, DARK)
    limb((0, 0, 2.62), (0, 0, 3.0), 0.24, 0.24, '#FFF3C4', verts=24, flat=True, line=0.015)
    for k in range(6):
        a = 2 * math.pi * k / 6
        limb((math.cos(a) * 0.24, math.sin(a) * 0.24, 2.62), (math.cos(a) * 0.24, math.sin(a) * 0.24, 3.0), 0.015, 0.015, DARK, line=0)
    cone((0, 0, 3.0), 0.32, 0.03, 0.35, RED)
    sphere((0, 0, 3.4), 0.05, (1, 1, 1), DARK, seg=10)
    box((0.62, 0.2, 0.32), (0.6, 0.55, 0.64), WHITE, bevel=0.03)
    thatch_roof(0.62, 0.2, 0.64, 0.6, 0.55, 0.3, RED, overhang=0.06)
    window((0.45, -0.08, 0.38), 0.15, 0.15, glow=True)

    ground_patch(0.9, SNOW)
@item('ship', yaw=-25)
def _():
    V = []; F = []
    prof = [(-1.4, 0.4), (-1.3, 0.0), (1.1, 0.0), (1.6, 0.55)]   # side profile (x, z) of the hull bottom edge
    hw = [0.35, 0.42, 0.42, 0.05]
    for i, ((x, z), w) in enumerate(zip(prof, hw)):
        V += [(x, -w, z), (x, w, z), (x, -w * 1.15, 0.75), (x, w * 1.15, 0.75)]
    for i in range(3):
        a = 4 * i; b = a + 4
        F += [(a, b, b + 1, a + 1), (a, a + 2, b + 2, b), (a + 1, b + 1, b + 3, a + 3), (a + 2, a + 3, b + 3, b + 2)]
    F += [(0, 1, 3, 2), (12, 14, 15, 13)]
    mesh_obj(V, F, RED, smooth=False, stripes=(WHITE, 1.0))
    box((-0.4, 0, 1.05), (0.9, 0.6, 0.6), WHITE, bevel=0.04)
    for x in (-0.7, -0.45, -0.2): window((x, -0.31, 1.1), 0.14, 0.14, glow=False)
    box((-0.4, 0, 1.4), (0.6, 0.45, 0.1), RED, bevel=0.03)
    limb((0.5, 0, 0.75), (0.8, 0, 1.9), 0.035, 0.03, DARK)
    limb((0.8, 0, 1.9), (1.4, 0, 1.2), 0.01, 0.01, DARK, line=0)
    blob((0, 0, 0.0), 1.0, (2.0, 0.9, 0.35), SNOW, rough=0.15, seed=4, top=1.0)
@item('buoy')
def _():
    blob((0, 0, 0.0), 0.75, (1, 1, 0.12), '#BFE3F2', rough=0.1, seed=6)
    ring((0, 0, 0.06), 0.62, 0.08, SNOW, sc_=(1, 1, 0.6))
    sphere((0, 0, 0.3), 0.36, (1, 1, 0.8), RED, seg=32, stripes=(WHITE, 3.0))
    limb((0, 0, 0.5), (0, 0, 1.15), 0.12, 0.08, RED, stripes=(WHITE, 3.0))
    limb((0, 0, 1.15), (0, 0, 1.4), 0.015, 0.015, DARK, line=0.006)
    sphere((0, 0, 1.45), 0.08, (1, 1, 1), '#F4C542', seg=14, flat=True)
@item('ruin_a', yaw=-20)
def _():
    B = '#9C5A44'
    box((0, 0, 1.0), (0.9, 0.8, 2.0), B, bevel=0.03, stripes=('#8A4E3A', 5.0))
    rnd = random.Random(97)
    for k in range(5):   # broken top
        box((rnd.uniform(-0.35, 0.35), rnd.uniform(-0.3, 0.3), 2.0 + rnd.uniform(0, 0.25)), (0.25, 0.25, 0.3), B, bevel=0.02, rot=(rnd.uniform(-15, 15), rnd.uniform(-15, 15), 0))
    for z in (0.5, 1.2, 1.7): window((-0.15, -0.41, z), 0.22, 0.28, glow=False); window((0.2, -0.41, z), 0.22, 0.28, glow=False)
    box((0.05, -0.41, 0.25), (0.3, 0.03, 0.5), '#3E4A57', bevel=0.01)

    blob((0.55, -0.4, 0.1), 0.25, (1.2, 1, 0.5), B, seed=3, smooth=False)
    ground_patch(0.9, SNOW)
@item('ruin_b', yaw=-25)
def _():
    for (x, y, z, s, r) in ((-0.45, 0, 0.32, 0.64, 0), (0.25, 0.05, 0.32, 0.64, 8), (-0.1, 0.0, 0.94, 0.6, -6), (0.75, -0.2, 0.22, 0.44, 15)):
        box((x, y, z), (s, s, s), WOOD, bevel=0.02, rot=(0, 0, r))
        for d in (-1, 1):
            o = box((x, y - s / 2 - 0.01, z), (s * 1.3, 0.03, 0.07), WOOD_D, bevel=0.005, rot=(0, d * 45, r))
    snow_top(CUR[6], cut=0.9, grow=1.02)
    ground_patch(1.0, SNOW)
@item('car', elev=90)
def _(): vehicle('#F1E7CF', 1.0)
@item('bus', elev=90)
def _(): vehicle('#7E8A96', 1.6, bus=True)

def vehicle(col, L, bus=False):
    """Seen from straight above, nose up the screen (+Y)."""
    W = 0.5 if not bus else 0.55
    box((0, 0, 0.25), (W, L, 0.35), col, bevel=0.12)
    roof_l = L * (0.55 if not bus else 0.9)
    box((0, -0.05 * L, 0.48), (W * 0.82, roof_l, 0.12), col, bevel=0.06, top=1.05)
    if bus:
        for k in range(6): box((0, -0.62 + k * 0.24, 0.55), (W * 0.62, 0.14, 0.02), '#3E4A57', bevel=0.01, line=0)
    else:
        box((0, roof_l / 2 + 0.06 - 0.05, 0.43), (W * 0.74, 0.16, 0.05), '#3E5468', bevel=0.02, line=0)
        box((0, -roof_l / 2 - 0.08, 0.43), (W * 0.72, 0.12, 0.05), '#3E5468', bevel=0.02, line=0)
    for s in (-1, 1):
        box((s * W * 0.32, L / 2 - 0.02, 0.27), (0.1, 0.03, 0.06), '#FFF3C4', bevel=0.01, line=0, flat=True)


# ================================================================== stations (seen from above, track to the right)
def gable(cx, cy, z, w, d, h, col, overhang=0.1, along_y=True, **k):
    o = thatch_roof(0, 0, 0, d if along_y else w, w if along_y else d, h, col, overhang=overhang)
    if along_y: o.rotation_euler = (0, 0, math.radians(90))
    o.location = (cx, cy, z)
    return o

def shingles(cx, cy, z, w, d, h, col, n=8, along_y=True):
    """Lines across a gable roof so it reads as tiles from above."""
    for i in range(n + 1):
        t = i / n
        for s in (-1, 1):
            x = cx + s * (w / 2 + 0.1) * (1 - t)
            zz = z + h * t
            limb((x, cy - d / 2 - 0.1, zz + 0.01), (x, cy + d / 2 + 0.1, zz + 0.01), 0.01, 0.01, '#5A3A2C', line=0, flat=True)

def ground(w, d, col):
    box((0, 0, -0.03), (w, d, 0.06), col, bevel=0.02, line=0)

@item('station_t0', elev=62, size=(346, 520))
def _():
    # village halt: a timber hut with a shingle roof and an open lean-to over the platform side
    box((0, 0.1, 0.45), (1.3, 2.0, 0.9), '#A8774E', bevel=0.03, stripes=('#93663F', 4.0, 'Z'))
    gable(0, 0.1, 0.9, 1.3, 2.0, 0.8, '#7A6A5A', overhang=0.18)
    shingles(0, 0.1, 0.9, 1.3, 2.0, 0.8, '#5A4A3A', n=6)
    box((0.66, -0.5, 0.35), (0.03, 0.4, 0.6), '#6B4A32', bevel=0.01)
    window((0.66, 0.5, 0.5), 0.3, 0.3, glow=True, rot=(0, 0, 90))
    for y in (-1.2, -0.4, 0.4, 1.2): limb((1.45, y, 0), (1.45, y, 1.0), 0.05, 0.05, WOOD_D)
    o = box((1.15, 0.0, 1.05), (0.75, 2.8, 0.06), '#8A7A6A', bevel=0.02, rot=(0, 12, 0))
    box((0.0, -1.35, 0.15), (0.9, 0.35, 0.3), WOOD, bevel=0.03)   # bench of crates

@item('station_t1', elev=62, size=(255, 520))
def _():
    # brick country station with a clock tower
    BR = '#C2553A'
    box((0, 0, 0.6), (1.6, 3.2, 1.2), '#C86A4A', bevel=0.03, stripes=('#B85A3C', 8.0))
    gable(0, 0, 1.2, 1.6, 3.2, 0.75, BR, overhang=0.15)
    shingles(0, 0, 1.2, 1.6, 3.2, 0.75, BR, n=7)
    box((0, 0, 0.7), (2.4, 0.9, 1.4), '#C86A4A', bevel=0.03, stripes=('#B85A3C', 8.0))
    gable(0, 0, 1.4, 2.4, 0.9, 0.6, BR, overhang=0.12, along_y=False)
    box((0, 0, 1.9), (0.6, 0.6, 1.2), '#C86A4A', bevel=0.02, stripes=('#B85A3C', 8.0))
    sphere((0.31, 0, 2.15), 0.2, (0.15, 1, 1), WHITE, seg=24)
    sphere((0.33, 0, 2.15), 0.03, (1, 1, 1), DARK, seg=8, line=0)
    cone((0, 0, 2.5), 0.45, 0.02, 0.9, '#5E6A76', verts=4, rot=(0, 0, 45), smooth=False)
    for y in (-1.2, -0.6, 0.6, 1.2): window((0.81, y, 0.65), 0.3, 0.35, glow=True, rot=(0, 0, 90))
    for y in (-1.0, 1.0): box((0, y, 1.75), (0.2, 0.2, 0.4), '#9C5A44', bevel=0.02)

@item('station_t2', elev=62, size=(261, 520))
def _():
    # city terminal: a glass arched hall between two stone towers
    STONE = '#C9B9A0'
    for y in (-1.7, 1.7):
        box((0, y, 1.0), (1.5, 0.9, 2.0), STONE, bevel=0.03, stripes=('#B8A88E', 6.0))
        cone((0, y, 2.0), 0.85, 0.05, 0.8, '#5E6A76', verts=4, rot=(0, 0, 45), smooth=False)
        for z in (0.6, 1.3):
            for yy in (-0.2, 0.2): window((0.76, y + yy, z), 0.28, 0.4, glow=True, rot=(0, 0, 90))
    # the hall roof: a half cylinder along Y with glass ribs
    bpy.ops.mesh.primitive_cylinder_add(vertices=24, radius=0.8, depth=2.6, location=(0, 0, 0.9), rotation=(math.radians(90), 0, 0))
    o = bpy.context.object
    bm = bmesh.new(); bm.from_mesh(o.data)
    bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.y < -0.01], context='VERTS'); bm.to_mesh(o.data); bm.free()
    o.rotation_euler = (math.radians(90), 0, 0)
    o.scale = (1, 1, 1)
    finish(o, toon('#9CC9DA', top=1.1, shade=0.8))
    for k in range(9):
        y = -1.2 + k * 0.3
        ring((0, y, 0.9), 0.8, 0.025, '#5E6A76', rot=(90, 0, 0), line=0, seg=32)
    box((0, 0, 0.45), (1.6, 2.6, 0.9), '#B8A88E', bevel=0.02)

@item('station_t3', elev=62, size=(324, 520))
def _():
    # mountain chalet: wide roof with deep eaves under snow, timber balcony
    box((0, 0, 0.55), (1.6, 2.4, 1.1), '#B8865A', bevel=0.03, stripes=('#A47448', 5.0))
    box((0, 0, 0.12), (1.75, 2.55, 0.25), '#9AA0A8', bevel=0.03)
    gable(0, 0, 1.08, 1.6, 2.4, 0.9, '#7A553E', overhang=0.42)
    shingles(0, 0, 1.08, 1.6, 2.4, 0.9, '#5A3A2C', n=7)
    gable(0, 0, 1.493, 1.2, 2.5, 0.517, '#F3F8FB', overhang=0.1)     # snow on the upper half of the roof
    for x in (-0.3, 0.3): window((x, -1.205, 0.62), 0.3, 0.32, glow=True)   # lit windows in the front gable
    window((0, -1.205, 1.35), 0.22, 0.2, glow=True)
    for y in (-0.7, 0.0, 0.7): window((0.81, y, 0.65), 0.3, 0.35, glow=True, rot=(0, 0, 90))
    box((1.05, 0, 0.75), (0.45, 2.2, 0.06), WOOD_D, bevel=0.01)
    for y in (-1.0, -0.5, 0.0, 0.5, 1.0): limb((1.25, y, 0.75), (1.25, y, 1.0), 0.02, 0.02, WOOD_D, line=0.008)
    limb((0.4, 0.8, 1.4), (0.4, 0.8, 2.0), 0.12, 0.12, '#8A8A8A')   # chimney

@item('station_t4', elev=62, size=(240, 520))
def _():
    # modern station: flat concrete block with a glass front and a cantilever canopy
    box((0, 0, 0.5), (1.4, 3.6, 1.0), '#D6DCE0', bevel=0.04)
    box((0.62, 0, 0.5), (0.18, 3.4, 0.85), '#8FC4DA', bevel=0.02, top=1.08)
    for k in range(9): limb((0.72, -1.6 + k * 0.4, 0.08), (0.72, -1.6 + k * 0.4, 0.92), 0.015, 0.015, '#5E6A76', line=0)
    box((0.1, 0, 1.05), (1.8, 3.9, 0.1), '#B8C0C6', bevel=0.02)
    box((1.0, 0, 1.05), (0.05, 3.9, 0.12), '#E5484D', bevel=0.01)       # red band along the canopy edge
    for y in (-1.45, -0.2, 1.05):                                        # solar panels
        box((-0.35, y + 0.2, 1.17), (0.5, 0.42, 0.04), '#2E4A7A', bevel=0.01)
        for k in range(1, 3): limb((-0.6, y + 0.2 - 0.21 + 0.14 * k, 1.2), (-0.1, y + 0.2 - 0.21 + 0.14 * k, 1.2), 0.004, 0.004, '#8FB8E8', line=0, flat=True)
    box((0.62, 0, 0.98), (0.12, 1.2, 0.14), '#26304F', bevel=0.02)       # name board
    box((0.69, 0, 0.98), (0.02, 1.0, 0.06), '#F2C14E', bevel=0.005, line=0, flat=True)
    box((0.15, 1.35, 1.15), (0.5, 0.7, 0.12), '#7FB060', bevel=0.03)   # roof garden
    for y in (1.15, 1.55): blob((0.15, y, 1.3), 0.16, (1, 1, 0.8), '#5E9E45', seed=int(y * 10) + 5)

@item('station_bench', elev=90, size=(112, 240))
def _():
    box((0, 0, 0.42), (0.42, 1.6, 0.05), WOOD, bevel=0.01, stripes=('#93663F', 6.0, 'X'))
    box((-0.25, 0, 0.7), (0.06, 1.6, 0.35), WOOD_D, bevel=0.01, rot=(0, -10, 0))
    for y in (-0.7, 0.7):
        box((0, y, 0.21), (0.46, 0.06, 0.42), '#5A6470', bevel=0.01)


@item('station_lamp', elev=90, size=(128, 128))
def _():
    sphere((0, 0, 0), 0.16, (1, 1, 0.3), DARK, seg=24)
    limb((0, 0, 0), (0, 0, 1.0), 0.05, 0.045, '#5A6470')
    ring((0, 0, 1.02), 0.2, 0.035, '#5A6470')
    sphere((0, 0, 1.0), 0.19, (1, 1, 0.6), '#FFF3C4', seg=24, flat=True, line=0.01)
    sphere((0, 0, 1.12), 0.08, (1, 1, 0.5), '#5A6470', seg=16)

# ------------------------------------------------------------------ run
# every item is seen from the same angle as the station buildings: from above (62 degrees), square on
VIEW_ELEV = 62.0
def view(cam):
    return (90.0 if cam.get('elev') == 90 else VIEW_ELEV), 0.0
# blender/scenery_anim.py reuses everything above and runs its own loop instead
for name, (fn, cam) in ([] if 'ANIM_HOOK' in globals() else ITEMS.items()):
    if ONLY and name not in ONLY: continue
    LINE[0] = 0.022
    fn()
    size = cam.get('size')
    el, yw = view(cam)
    frame_and_render(name, elev=el, yaw=yw,
                     res=700 if not size else 2 * max(size), pad=1.06,
                     aspect=(size[0] / size[1]) if size else None)
    print('done', name, flush=True)
    clear()
if 'ANIM_HOOK' in globals(): ANIM_HOOK(globals())
