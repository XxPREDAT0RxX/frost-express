"""Little Express: all NPC passengers as toon-shaded 3D chibis, rendered to sprite frames.

    blender -b -P blender/npcs.py -- <out_dir> [--fast] [--only c1,n07]

Characters c1..c6 are the original six (same look as blender/old/chibis.py); n07.. are new NPCs.
Each one is a spec (skin, outfit, head pieces, props) put together from simple shapes.
Per character folder: idle_XX (front, breathing and blinking), walk_XX (front walk cycle),
side_XX (three-quarter walk, facing right), sit_XX (waist-up idle for the seats).
Shading is flat colour with a soft second tone toward the silhouette and a thin warm-brown
inverted-hull outline, to match the illustrated chibi sheet.
"""
import bpy, bmesh, math, os, sys
from mathutils import Vector

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = next((a for a in ARGS if not a.startswith('--') and ',' not in a), '/tmp/npcs')
os.makedirs(OUT, exist_ok=True)
FAST = '--fast' in ARGS
ONLY = ARGS[ARGS.index('--only') + 1].split(',') if '--only' in ARGS else None

# ------------------------------------------------------------------ scene
bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'
sc.cycles.device = 'CPU'
sc.cycles.samples = 8
sc.cycles.use_denoising = False
sc.cycles.max_bounces = 0
sc.render.film_transparent = True
sc.render.filter_size = 1.0
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'
sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world
world.use_nodes = True
world.node_tree.nodes['Background'].inputs[1].default_value = 0.0

def make_cam(name, loc, rot, scale):
    d = bpy.data.cameras.new(name); d.type = 'ORTHO'; d.ortho_scale = scale
    o = bpy.data.objects.new(name, d); sc.collection.objects.link(o)
    o.location = loc; o.rotation_euler = [math.radians(a) for a in rot]
    return o
TILT = 8.0
up = math.tan(math.radians(TILT)) * 10
CAM_FRONT = make_cam('front', (0, -10, 0.8 + up), (90 - TILT, 0, 0), 1.74)
# three-quarter side view (face still visible), walking toward screen-right
A3 = math.radians(205)
CAM_SIDE = make_cam('side', (10 * math.cos(A3), 10 * math.sin(A3), 0.8 + up), (90 - TILT, 0, -65), 1.74)
CAM_SIT = make_cam('sit', (0, -10, 1.06 + up), (90 - TILT, 0, 0), 1.06)

# ------------------------------------------------------------------ materials
def lin(h, k=1.0):
    h = h.lstrip('#'); c = [int(h[i:i + 2], 16) / 255 * k for i in (0, 2, 4)]
    return [x ** 2.2 for x in c] + [1]

MATS = {}
def toon(col, shade=0.9, stripes=None):
    """Flat colour, a soft darker band toward the silhouette. stripes=(col2, bands per unit) for a sweater."""
    key = (col, shade, stripes)
    if key in MATS: return MATS[key]
    m = bpy.data.materials.new('m%d' % len(MATS)); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    lw = nt.nodes.new('ShaderNodeLayerWeight'); lw.inputs['Blend'].default_value = 0.35
    ramp = nt.nodes.new('ShaderNodeValToRGB'); ramp.color_ramp.interpolation = 'CONSTANT'
    ramp.color_ramp.elements[0].color = (0, 0, 0, 1); ramp.color_ramp.elements[1].position = 0.55
    if stripes:
        tc = nt.nodes.new('ShaderNodeTexCoord'); sep = nt.nodes.new('ShaderNodeSeparateXYZ')
        mul = nt.nodes.new('ShaderNodeMath'); mul.operation = 'MULTIPLY'; mul.inputs[1].default_value = stripes[1] * math.pi * 2
        sn = nt.nodes.new('ShaderNodeMath'); sn.operation = 'SINE'
        gt = nt.nodes.new('ShaderNodeMath'); gt.operation = 'GREATER_THAN'; gt.inputs[1].default_value = 0.0
        mix = nt.nodes.new('ShaderNodeMix'); mix.data_type = 'RGBA'
        mix.inputs[6].default_value = lin(col); mix.inputs[7].default_value = lin(stripes[0])
        L(tc.outputs['Object'], sep.inputs[0]); L(sep.outputs['Z'], mul.inputs[0]); L(mul.outputs[0], sn.inputs[0])
        L(sn.outputs[0], gt.inputs[0]); L(gt.outputs[0], mix.inputs[0])
        base = mix.outputs[2]
        dark = nt.nodes.new('ShaderNodeMix'); dark.data_type = 'RGBA'; dark.blend_type = 'MULTIPLY'
        dark.inputs[0].default_value = 1.0; L(base, dark.inputs[6]); dark.inputs[7].default_value = [shade ** 2.2] * 3 + [1]
        darkc = dark.outputs[2]
    e1 = nt.nodes.new('ShaderNodeEmission'); e2 = nt.nodes.new('ShaderNodeEmission')
    if stripes: L(base, e1.inputs['Color']); L(darkc, e2.inputs['Color'])
    else: e1.inputs['Color'].default_value = lin(col); e2.inputs['Color'].default_value = lin(col, shade)
    mx = nt.nodes.new('ShaderNodeMixShader')
    L(lw.outputs['Facing'], ramp.inputs['Fac']); L(ramp.outputs['Color'], mx.inputs['Fac'])
    L(e1.outputs[0], mx.inputs[1]); L(e2.outputs[0], mx.inputs[2]); L(mx.outputs[0], out.inputs['Surface'])
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

LINE = 0.013
def finish(o, mat, line=LINE):
    o.data.materials.append(mat)
    for p in o.data.polygons: p.use_smooth = True
    if line > 0:
        o.data.materials.append(outline_mat())
        s = max(o.scale) if max(o.scale) > 0 else 1
        md = o.modifiers.new('ol', 'SOLIDIFY'); md.thickness = line / s; md.offset = 1.0
        md.use_flip_normals = True; md.use_rim = False; md.material_offset = 1
    return o

CUR = []   # objects of the character being built
def reg(o): CUR.append(o); return o

def sphere(loc, r, sc_=(1, 1, 1), col='#FFFFFF', line=LINE, seg=40, shade=0.9, mat=None, rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=seg // 2, radius=r, location=loc,
                                         rotation=[math.radians(a) for a in rot])
    o = bpy.context.object; o.scale = sc_
    return reg(finish(o, mat or toon(col, shade), line))

def shell(loc, sc_, col, cut_y, cut_z, line=LINE, shade=0.9):
    """A sphere with its front-lower part cut away (hood, head shawl): drops verts with y < cut_y and z < cut_z (world)."""
    bpy.ops.mesh.primitive_uv_sphere_add(segments=56, ring_count=28, radius=1.0, location=(0, 0, 0))
    o = bpy.context.object
    bm = bmesh.new(); bm.from_mesh(o.data)
    kill = [v for v in bm.verts
            if v.co.y * sc_[1] + loc[1] < cut_y and v.co.z * sc_[2] + loc[2] < cut_z]
    bmesh.ops.delete(bm, geom=kill, context='VERTS'); bm.to_mesh(o.data); bm.free()
    o.location = loc; o.scale = sc_
    return reg(finish(o, toon(col, shade), line))

def box(loc, size, col, line=LINE, shade=0.88, bevel=0.3):
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=loc)
    o = bpy.context.object; o.scale = size
    b = o.modifiers.new('bv', 'BEVEL'); b.segments = 3
    b.width = 0.5 * bevel   # in local (unit cube) space, so rounding follows the box proportions
    return reg(finish(o, toon(col, shade), line))

def tube(a, b, r, col, line=LINE, shade=0.9, mat=None):
    a, b = Vector(a), Vector(b); d = b - a
    bpy.ops.mesh.primitive_cylinder_add(vertices=24, radius=r, depth=d.length, location=(a + b) / 2)
    o = bpy.context.object
    o.rotation_mode = 'QUATERNION'; o.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(d.normalized())
    return reg(finish(o, mat or toon(col, shade), line))

def ring(loc, R, r, col, sc_=(1, 1, 1), rot=(0, 0, 0), line=LINE, shade=0.9):
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, major_segments=40, minor_segments=12, location=loc,
                                     rotation=[math.radians(a) for a in rot])
    o = bpy.context.object; o.scale = sc_
    return reg(finish(o, toon(col, shade), line))

def arc(loc, R, r, col, upper, line=0, shade=1.0, rx=90, rz=0, sy=1.0):
    """Half torus: upper=True keeps the top half (a band over the head when rx=90 and seen from the front)."""
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, major_segments=32, minor_segments=8)
    o = bpy.context.object
    bm = bmesh.new(); bm.from_mesh(o.data)
    kill = [v for v in bm.verts if (v.co.y < -0.0005 if upper else v.co.y > 0.0005)]
    bmesh.ops.delete(bm, geom=kill, context='VERTS'); bm.to_mesh(o.data); bm.free()
    o.location = loc; o.rotation_euler = (math.radians(rx), 0, math.radians(rz)); o.scale = (1, sy, 1)
    return reg(finish(o, toon(col, shade), line))

def empty(name, loc, parent=None):
    e = bpy.data.objects.new(name, None); e.location = loc; sc.collection.objects.link(e)
    if parent: setp(e, parent)
    CUR.append(e); return e

def setp(child, par):
    bpy.context.view_layer.update()
    mw = child.matrix_world.copy(); child.parent = par; child.matrix_world = mw

# ------------------------------------------------------------------ body plan (feet at z=0, facing -Y)
HC = Vector((0, 0, 1.14)); HA, HB, HCZ = 0.353, 0.325, 0.325     # head centre and radii
def face_y(x, z, push=0.0):
    t = 1 - (x / HA) ** 2 - ((z - HC.z) / HCZ) ** 2
    return -HB * math.sqrt(max(t, 0.0)) - push

EYE = '#3B2622'
# skin tone: (skin, blush, nose)
SKINS = {
    'fair':  ('#FCE0CC', '#F6A9A2', '#F3B7A6'),
    'wheat': ('#F1CBA8', '#EE9E8E', '#E2A688'),
    'tan':   ('#DDAA82', '#DE8C7C', '#C9916C'),
    'brown': ('#BD845E', '#C9766A', '#A9704F'),
}

def torso_front(z, clen, push=0.004):
    zc, rz = 0.56 - clen / 2, 0.27 + clen / 2
    return -0.165 * math.sqrt(max(0, 1 - ((z - zc) / rz) ** 2)) - push

def build(spec):
    """Builds one character from spec; returns its rig (dict of pivots and parts)."""
    CUR.clear()
    skin, blush, nose = SKINS[spec.get('skin', 'fair')]
    root = empty('root', (0, 0, 0))
    body = empty('body', (0, 0, 0.34), root)
    head = empty('head', (0, 0, 0.84), body)
    rig = dict(root=root, body=body, head=head, coat=[], eyes_open=[], eyes_shut=[], legs=[])
    # ---- legs and boots
    for sx in (-1, 1):
        hip = empty('hip', (sx * 0.085, 0, 0.36), root)
        leg = tube((sx * 0.085, 0, 0.36), (sx * 0.085, 0, 0.09), 0.062, spec['pants'])
        boot = sphere((sx * 0.085, -0.025, 0.055), 1.0, (0.075, 0.1, 0.06), spec['boots'])
        parts = [leg, boot]
        if spec.get('cuff'):
            parts.append(ring((sx * 0.085, 0, 0.11), 0.063, 0.022, spec['cuff']))
        for o in parts: setp(o, hip)
        rig['legs'].append(hip)
    # ---- coat / torso
    clen = spec.get('coat_len', 0.0)
    torso = sphere((0, 0, 0.56 - clen / 2), 1.0, (0.205, 0.165, 0.27 + clen / 2), spec['coat'], mat=spec.get('coat_mat'))
    rig['coat'].append(torso)
    torso_parts = [torso]
    if spec.get('puffer'):
        for z in (0.46, 0.6, 0.72):
            w = 0.205 * math.sqrt(max(0, 1 - ((z - 0.56) / 0.27) ** 2)) + 0.004
            torso_parts.append(ring((0, 0, z), w, 0.012, spec['coat'], sc_=(1, 0.8, 1), shade=0.78, line=0))
    if spec.get('buttons'):
        for z in (0.48, 0.58, 0.68):
            y = -0.165 * math.sqrt(max(0, 1 - ((z - 0.56) / 0.27) ** 2)) - 0.004
            torso_parts.append(sphere((0, y, z), 0.016, (1, 0.5, 1), spec['buttons'], line=0, seg=16))
    if spec.get('zip'):
        torso_parts.append(tube((0, -0.168, 0.36), (0, -0.168, 0.76), 0.006, '#7A4E3A', line=0))
    if spec.get('collar'):
        torso_parts.append(ring((0, 0.0, 0.8), 0.12, 0.05, spec['collar'], sc_=(1, 0.85, 1)))
    if spec.get('tie'):
        torso_parts.append(sphere((0, torso_front(0.66, clen, 0.006), 0.66), 1.0, (0.032, 0.012, 0.085), spec['tie'], line=0.008))
    if spec.get('backpack'):
        col = spec['backpack']
        torso_parts.append(box((0, 0.2, 0.57), (0.3, 0.14, 0.34), col, bevel=0.35))
        torso_parts.append(box((0, 0.275, 0.5), (0.2, 0.05, 0.14), col, shade=0.8, bevel=0.35))
        for sx in (-1, 1):
            torso_parts.append(tube((sx * 0.11, torso_front(0.78, clen, 0.006), 0.79),
                                    (sx * 0.125, torso_front(0.52, clen, 0.006), 0.52), 0.016, col, line=0.008, shade=0.8))
    if spec.get('shawl'):   # a draped shawl over the shoulders, ends hanging in front
        col = spec['shawl']
        torso_parts.append(ring((0, 0.0, 0.79), 0.16, 0.055, col, sc_=(1.05, 0.95, 0.85)))
        for sx in (-1, 1):
            torso_parts.append(sphere((sx * 0.1, torso_front(0.62, clen, 0.012), 0.62), 1.0, (0.06, 0.022, 0.15), col, rot=(0, sx * 8, 0)))
    for o in torso_parts: setp(o, body)
    # ---- arms
    for sx in (-1, 1):
        sh = empty('shoulder', (sx * 0.19, 0, 0.76), body)
        arm = tube((sx * 0.19, 0, 0.76), (sx * 0.235, 0, 0.53), 0.052, spec.get('sleeve', spec['coat']), mat=spec.get('coat_mat'))
        hand = sphere((sx * 0.24, -0.005, 0.5), 0.05, (1, 1, 1), spec.get('mitten', skin), seg=24)
        parts = [arm, hand]
        if sx > 0 and spec.get('briefcase'):
            col = spec['briefcase']
            parts.append(box((0.25, -0.01, 0.37), (0.06, 0.2, 0.15), col, bevel=0.3))
            parts.append(arc((0.25, -0.01, 0.445), 0.035, 0.009, '#3B2A20', True, rz=90))
        if sx < 0 and spec.get('bag'):   # small handbag hanging from the left hand
            col = spec['bag']
            parts.append(box((-0.25, -0.01, 0.39), (0.06, 0.15, 0.12), col, bevel=0.4))
        for o in parts: setp(o, sh)
        rig['arm_l' if sx < 0 else 'arm_r'] = sh
    # ---- scarf
    if spec.get('scarf'):
        s1 = ring((0, -0.005, 0.83), 0.15, 0.055, spec['scarf'], sc_=(1, 0.95, 0.9))
        s2 = sphere((0.075, -0.17, 0.68), 1.0, (0.05, 0.025, 0.11), spec['scarf'])
        for o in (s1, s2): setp(o, body)
    # ---- head
    hp = []
    hp.append(sphere(HC, 1.0, (HA, HB, HCZ), skin, seg=56))
    for sx in (-1, 1):
        hp.append(sphere((sx * HA * 0.98, 0.0, HC.z - 0.04), 0.055, (0.55, 1, 1), skin, seg=20))
    ez = HC.z - 0.04
    for sx in (-1, 1):
        x = sx * 0.12
        y = face_y(x, ez, 0.004)
        e = sphere((x, y, ez), 1.0, (0.042, 0.014, 0.054), EYE, line=0, seg=20)
        hl = sphere((x + 0.013, y - 0.01, ez + 0.018), 0.014, (1, 0.4, 1), '#FFFFFF', line=0, seg=12)
        rig['eyes_open'] += [e, hl]
        sh_ = arc((x, face_y(x, ez - 0.01, 0.006), ez - 0.014), 0.038, 0.008, EYE, True)
        rig['eyes_shut'].append(sh_)
        hp += [e, hl, sh_]
        bz = HC.z - 0.105
        hp.append(sphere((sx * 0.215, face_y(sx * 0.215, bz, 0.002), bz), 1.0, (0.052, 0.012, 0.034), blush, line=0, seg=20))
    hp.append(sphere((0, face_y(0, HC.z - 0.085, 0.002), HC.z - 0.085), 0.012, (1, 0.4, 0.8), nose, line=0, seg=12))
    if not spec.get('beard'):
        hp.append(arc((0, face_y(0, HC.z - 0.13, 0.004), HC.z - 0.115), 0.03, 0.007, '#7A3E34', False))
    if spec.get('bindi'):
        z = HC.z + 0.055
        hp.append(sphere((0, face_y(0, z, 0.002), z), 0.014, (1, 0.4, 1), spec['bindi'], line=0, seg=12))
    if spec.get('glasses'):
        for sx in (-1, 1):
            x = sx * 0.12
            hp.append(ring((x, face_y(x, ez, 0.02), ez), 0.074, 0.0085, spec['glasses'], rot=(90, 0, 0), line=0, shade=1.0))
        hp.append(tube((-0.046, face_y(0, ez, 0.02), ez + 0.01), (0.046, face_y(0, ez, 0.02), ez + 0.01), 0.006, spec['glasses'], line=0))
    for f in spec['head_extra']:
        hp += f()
    for o in hp: setp(o, head)
    root.scale = [spec.get('scale', 1.0)] * 3
    rig['all'] = list(CUR)
    return rig

# ---- head pieces (hair, hats, beards); each returns a list of objects
def hair_cap(col, back=0.04, top=0.06):
    return [sphere((0, back, HC.z + top), 1.0, (HA * 1.06, HB * 1.12, HCZ * 1.02), col, seg=56)]
def side_part(col, sx=1):
    """Short hair with a soft side-swept fringe."""
    return hair_cap(col, back=0.04, top=0.07) + [
        sphere((sx * 0.1, -0.2, HC.z + 0.21), 1.0, (0.2, 0.1, 0.075), col, rot=(0, -sx * 14, 0), seg=32)]
def bun(col, low=False):
    if low: return [sphere((0, 0.38, HC.z - 0.06), 0.11, (1, 0.85, 1), col)]
    return [sphere((0, 0.06, HC.z + 0.37), 0.12, (1, 1, 0.9), col)]
def beanie(col, cuff, pom=None):
    o = [sphere((0, 0.02, HC.z + 0.12), 1.0, (HA * 1.08, HB * 1.14, HCZ * 0.96), col, seg=56),
         ring((0, 0.0, HC.z + 0.085), HA * 1.0, 0.055, cuff, sc_=(1, 0.97, 1))]
    if pom: o.append(sphere((0, 0.02, HC.z + 0.43), 0.085, (1, 1, 1), pom))
    return o
def braids(col):
    o = []
    for sx in (-1, 1):
        for k in range(4):
            o.append(sphere((sx * (0.33 + 0.03 * k), 0.04, HC.z - 0.08 - 0.08 * k), 0.048 - 0.004 * k, (1, 1, 1), col, seg=20))
        o.append(sphere((sx * 0.45, 0.04, HC.z - 0.41), 0.022, (1, 1, 1), '#F2C14E', seg=12))
    return o
def pigtails(col, tie):
    o = []
    for sx in (-1, 1):
        o.append(sphere((sx * 0.37, 0.06, HC.z + 0.12), 0.034, (1, 1, 1), tie, seg=16))
        o.append(sphere((sx * 0.44, 0.07, HC.z + 0.05), 0.085, (1, 1, 1.1), col, seg=28))
        o.append(sphere((sx * 0.49, 0.08, HC.z - 0.06), 0.06, (1, 1, 1.1), col, seg=24))
    return o
def ponytail(col, tie):
    o = [sphere((0, 0.4, HC.z + 0.13), 0.04, (1, 1, 1), tie, seg=16)]
    for k, (y, z, r) in enumerate(((0.45, 0.06, 0.075), (0.47, -0.04, 0.068), (0.47, -0.14, 0.058), (0.45, -0.23, 0.045))):
        o.append(sphere((0.02 * k, y, HC.z + z), r, (1, 1, 1.15), col, seg=24))
    return o
def bob_hair(col):
    return [sphere((0, 0.06, HC.z - 0.05), 1.0, (0.39, 0.32, 0.3), col, seg=48)]
def long_hair(col):
    return [sphere((0, 0.1, HC.z - 0.16), 1.0, (0.38, 0.22, 0.4), col, seg=48)]
def beard(col):
    o = [sphere((0, -0.2, HC.z - 0.19), 1.0, (0.26, 0.14, 0.17), col, seg=40),
         sphere((0, face_y(0, HC.z - 0.11, 0.02), HC.z - 0.11), 1.0, (0.1, 0.03, 0.032), col, seg=24)]
    o.append(arc((0, face_y(0, HC.z - 0.15, 0.06), HC.z - 0.15), 0.028, 0.006, '#F4E6DA', False))
    return o
def mustache(col, big=False):
    o = []
    z = HC.z - 0.104
    w = 0.062 if big else 0.048
    for sx in (-1, 1):
        x = sx * w * 0.8
        o.append(sphere((x, face_y(x, z, 0.008), z), 1.0, (w, 0.02, 0.018 if not big else 0.022), col,
                        rot=(0, sx * 16, 0), line=0.006, seg=24))
    return o
def fur_hat(col):
    return [sphere((0, 0.02, HC.z + 0.16), 1.0, (HA * 1.18, HB * 1.22, HCZ * 0.8), col, seg=56),
            ring((0, -0.005, HC.z + 0.095), HA * 1.06, 0.068, col, shade=0.85)]
def trapper(col, inner):
    o = fur_hat(col)
    for sx in (-1, 1):
        o.append(sphere((sx * HA * 0.98, 0.01, HC.z - 0.1), 1.0, (0.075, 0.12, 0.19), col, seg=32))
        o.append(sphere((sx * HA * 0.95, -0.035, HC.z - 0.1), 1.0, (0.048, 0.075, 0.13), inner, seg=24, line=0))
    return o
def spikes(col):
    o = []
    for x, z in ((-0.13, HC.z + 0.33), (0.0, HC.z + 0.37), (0.13, HC.z + 0.33)):
        o.append(sphere((x, 0.02, z), 1.0, (0.065, 0.065, 0.085), col, seg=24))
    return o
def flat_cap(col):
    return [sphere((0, 0.03, HC.z + 0.17), 1.0, (HA * 1.08, HB * 1.12, HCZ * 0.55), col, seg=56),
            sphere((0, -0.26, HC.z + 0.165), 1.0, (0.21, 0.13, 0.026), col, shade=0.8, rot=(-8, 0, 0))]
def peaked_cap(col, band, brim, badge):
    return [sphere((0, 0.02, HC.z + 0.25), 1.0, (HA * 1.12, HB * 1.16, 0.12), col, seg=56),
            ring((0, 0.0, HC.z + 0.17), HA * 0.98, 0.05, band, sc_=(1, 0.96, 0.8)),
            sphere((0, -0.29, HC.z + 0.13), 1.0, (0.2, 0.12, 0.022), brim, rot=(-12, 0, 0)),
            sphere((0, -0.355, HC.z + 0.22), 0.03, (1, 0.45, 1), badge, line=0.006, seg=16)]
def pagri(col):
    """Rajasthani-style turban: a domed wrap with two folds."""
    return [sphere((0, 0.01, HC.z + 0.15), 1.0, (HA * 1.12, HB * 1.16, HCZ * 0.78), col, seg=56),
            ring((0, 0.0, HC.z + 0.08), HA * 1.02, 0.06, col, sc_=(1, 0.97, 1), shade=0.85),
            ring((0, 0.02, HC.z + 0.19), HA * 0.97, 0.05, col, sc_=(1, 0.97, 1), rot=(-14, 0, 0), shade=0.85)]
def dupatta(col):
    """A shawl over the head, framing the face, falling behind the shoulders."""
    return [shell((0, 0.03, HC.z + 0.03), (HA * 1.16, HB * 1.2, HCZ * 1.14), col, cut_y=-0.14, cut_z=HC.z + 0.2),
            sphere((0, 0.12, HC.z - 0.3), 1.0, (0.33, 0.15, 0.3), col, shade=0.85)]
def hood(col):
    return [shell((0, 0.04, HC.z + 0.02), (HA * 1.2, HB * 1.22, HCZ * 1.16), col, cut_y=-0.12, cut_z=HC.z + 0.22)]
def band_over(col, cup, cup_sc, band_r=0.022):
    """Headband with a cup over each ear (earmuffs, headphones)."""
    o = [arc((0, 0.0, HC.z - 0.03), HA * 1.1, band_r, col, True, line=0.008, shade=0.9, sy=1.2)]
    for sx in (-1, 1):
        o.append(sphere((sx * HA * 1.02, 0.0, HC.z - 0.03), 1.0, cup_sc, cup, seg=32))
    return o
def earmuffs(col, band):
    return band_over(band, col, (0.07, 0.09, 0.1))
def headphones(col):
    return band_over(col, col, (0.055, 0.085, 0.095), band_r=0.026)

SPECS = [
    # ---- the original six
    dict(name='c1', coat='#F5EAD3', pants='#2F3448', boots='#8A5A3C', scarf='#EE8E8B', glasses='#C9A04E', buttons='#9C6B4E',
         coat_len=0.06, head_extra=[lambda: hair_cap('#C8C6C8'), lambda: bun('#C8C6C8')]),
    dict(name='c2', coat='#F7A3B5', pants='#2F3448', boots='#8A5A3C', cuff='#F5EAD3', puffer=True, zip=True, mitten='#F2C14E',
         coat_len=0.08, head_extra=[lambda: hair_cap('#7A4E36'), lambda: braids('#7A4E36'), lambda: beanie('#F4C542', '#E7B533', '#F4C542')]),
    dict(name='c3', coat='#2E3558', pants='#2F3448', boots='#2E3558', glasses='#4A3A36', collar='#F3E6CC',
         stripes=('#F3E6CC', 3.0), head_extra=[lambda: hair_cap('#7A4E36', back=0.03, top=0.08), lambda: spikes('#7A4E36')]),
    dict(name='c4', coat='#F08A3C', pants='#2F3448', boots='#8A5A3C', puffer=True, zip=True, beard=True,
         head_extra=[lambda: beard('#7A4E36'), lambda: beanie('#9EA3A8', '#8C9196')]),
    dict(name='c5', coat='#5E86B8', pants='#2F3448', boots='#2F3448', cuff='#F5EAD3', buttons='#2E3558', collar='#5E86B8',
         coat_len=0.1, head_extra=[lambda: bob_hair('#7A4E36'), lambda: fur_hat('#E6D6BC')]),
    dict(name='c6', coat='#5E86B8', pants='#2F3448', boots='#2F3448', zip=True, mitten='#7A4E36', collar='#F5EAD3',
         coat_len=0.12, head_extra=[lambda: long_hair('#7A4E36'), lambda: trapper('#F7F1E6', '#5E86B8')]),
    # ---- new NPCs
    # n07 little boy: red bobble beanie, green puffer
    dict(name='n07', skin='wheat', scale=0.8, coat='#5BAF7A', pants='#2F3448', boots='#8A5A3C', puffer=True, zip=True,
         mitten='#E0565B', coat_len=0.04, head_extra=[lambda: hair_cap('#2B2220'), lambda: beanie('#E0565B', '#C9474C', '#F5EAD3')]),
    # n08 grandpa: flat cap, white moustache, long brown coat, green scarf
    dict(name='n08', skin='tan', coat='#8B5E3C', pants='#3B3B45', boots='#4A3328', buttons='#4A3328', scarf='#4E8A6E',
         glasses='#4A3A36', coat_len=0.14, head_extra=[lambda: hair_cap('#D8D4CE', top=0.03), lambda: mustache('#EDEAE4', big=True),
                                                        lambda: flat_cap('#7B6A58')]),
    # n09 office worker: navy long coat, red tie, briefcase
    dict(name='n09', skin='wheat', coat='#2F3A5C', pants='#2A2E3A', boots='#2A2420', collar='#F3F0EA', tie='#C8443C',
         briefcase='#7A5236', coat_len=0.12, head_extra=[lambda: side_part('#2B2220')]),
    # n10 student girl: ponytail, pink earmuffs, lilac puffer, yellow backpack
    dict(name='n10', skin='fair', coat='#9C8BD0', pants='#2F3448', boots='#F5EAD3', puffer=True, zip=True, mitten='#F5EAD3',
         cuff='#F5EAD3', backpack='#F2C14E', coat_len=0.06,
         head_extra=[lambda: hair_cap('#6A4430'), lambda: ponytail('#6A4430', '#F28FB0'), lambda: earmuffs('#F7B2C8', '#E77FA2')]),
    # n11 Paddylands village woman: mustard dupatta, teal kurta, white salwar, bindi
    dict(name='n11', skin='brown', coat='#2E8B7A', pants='#F3EEE2', boots='#8A3C2C', coat_len=0.18,
         bindi='#C8343C', bag='#C8643C',
         head_extra=[lambda: hair_cap('#231B19'), lambda: dupatta('#E9B949')]),
    # n12 Paddylands village man: orange pagri, moustache, white kurta, brown shawl
    dict(name='n12', skin='tan', coat='#F5F1E6', pants='#F5F1E6', boots='#6B3F2A', shawl='#9A6A44', coat_len=0.18,
         head_extra=[lambda: hair_cap('#231B19', top=0.02), lambda: mustache('#231B19', big=True), lambda: pagri('#F08A3C')]),
    # n13 train conductor: peaked cap, navy coat with gold buttons
    dict(name='n13', skin='wheat', coat='#26304F', pants='#26304F', boots='#1F1A18', buttons='#D9B24C', collar='#F3F0EA',
         coat_len=0.08, head_extra=[lambda: hair_cap('#2B2220', top=0.03), lambda: mustache('#2B2220'),
                                    lambda: peaked_cap('#26304F', '#1F2640', '#1F1A18', '#D9B24C')]),
    # n14 teen: grey hoodie with hood up, red headphones, white sneakers
    dict(name='n14', skin='fair', coat='#7E8A99', pants='#2F3448', boots='#F2F2F2', zip=True, collar='#7E8A99',
         head_extra=[lambda: hair_cap('#8A5A3C', top=0.03), lambda: hood('#7E8A99'), lambda: headphones('#D9534F')]),
    # n15 aunty: low bun, maroon shawl, cream coat, handbag, bindi
    dict(name='n15', skin='wheat', coat='#EADCC6', pants='#5A3A44', boots='#5A3A2C', buttons='#9C6B4E', shawl='#9C3B4A',
         coat_len=0.14, bindi='#C8343C', bag='#9C3B4A', glasses='#C9A04E',
         head_extra=[lambda: hair_cap('#231B19'), lambda: bun('#231B19', low=True)]),
    # n16 little girl: black pigtails with red ties, red puffer, white mittens
    dict(name='n16', skin='brown', scale=0.78, coat='#E0565B', pants='#2F3448', boots='#F2C14E', puffer=True, zip=True,
         mitten='#F5EAD3', cuff='#F5EAD3', coat_len=0.04,
         head_extra=[lambda: hair_cap('#231B19'), lambda: pigtails('#231B19', '#E0565B')]),
]
for s in SPECS:
    if s.get('stripes'): s['coat_mat'] = toon(s['coat'], 0.9, s['stripes'])

# ------------------------------------------------------------------ animation
def pose_idle(rig, t, blink):
    br = math.sin(2 * math.pi * t / 2.0)
    rig['root'].location = (0, 0, 0)
    rig['body'].location = (0, 0, 0.34 + 0.004 * br)
    rig['body'].rotation_euler = (0, math.radians(1.2 * math.sin(2 * math.pi * t / 4.0)), 0)
    for o in rig['coat']: o.scale.z = o['sz0'] * (1 + 0.012 * br)
    rig['head'].rotation_euler = (math.radians(1.5 * math.sin(2 * math.pi * t / 2.0 - 0.6)), math.radians(3 * math.sin(2 * math.pi * t / 4.0 - 0.4)), 0)
    rig['arm_l'].rotation_euler = (0, math.radians(-2 * br), 0)
    rig['arm_r'].rotation_euler = (0, math.radians(2 * br), 0)
    for h in rig['legs']: h.rotation_euler = (0, 0, 0)
    for o in rig['eyes_open']: o.hide_render = blink
    for o in rig['eyes_shut']: o.hide_render = not blink

def pose_walk(rig, ph):
    s = math.sin(ph)
    rig['root'].location = (0, 0, 0.022 * abs(math.cos(ph)))
    rig['body'].location = (0, 0, 0.34)
    rig['body'].rotation_euler = (math.radians(4), math.radians(-3 * s), math.radians(-4 * s))
    for o in rig['coat']: o.scale.z = o['sz0']
    rig['head'].rotation_euler = (math.radians(-2), math.radians(2.5 * s), math.radians(3 * s))
    rig['legs'][0].rotation_euler = (math.radians(-28 * s), 0, 0)
    rig['legs'][1].rotation_euler = (math.radians(28 * s), 0, 0)
    rig['arm_l'].rotation_euler = (math.radians(26 * s), 0, math.radians(-6))
    rig['arm_r'].rotation_euler = (math.radians(-26 * s), 0, math.radians(6))
    for o in rig['eyes_open']: o.hide_render = False
    for o in rig['eyes_shut']: o.hide_render = True

def render(cam, w, h, path):
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = w, h
    sc.render.filepath = path
    bpy.ops.render.render(write_still=True)

IDLE_N = 6 if FAST else 24
WALK_N = 8
FW, FH = 176, 248

for spec in SPECS:
    if ONLY and spec['name'] not in ONLY: continue
    rig = build(spec)
    for o in rig['coat']: o['sz0'] = o.scale.z
    d = os.path.join(OUT, spec['name']); os.makedirs(d, exist_ok=True)
    for i in range(IDLE_N):
        t = i / 12.0
        blink = i in (14, 15) or (FAST and i == 3)
        pose_idle(rig, t, blink)
        render(CAM_FRONT, FW, FH, os.path.join(d, 'idle_%02d.png' % i))
        render(CAM_SIT, FW, FW, os.path.join(d, 'sit_%02d.png' % i))
    for i in range(WALK_N):
        ph = 2 * math.pi * i / WALK_N
        pose_walk(rig, ph)
        render(CAM_FRONT, FW, FH, os.path.join(d, 'walk_%02d.png' % i))
        render(CAM_SIDE, FW, FH, os.path.join(d, 'side_%02d.png' % i))
    print('done', spec['name'], flush=True)
    for o in rig['all']:
        bpy.data.objects.remove(o, do_unlink=True)
