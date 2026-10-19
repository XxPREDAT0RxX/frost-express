"""Little Express: a 3D chibi passenger ("Dadi") rendered to transparent sprite frames.

Run headless:  blender -b -P blender/dadi.py -- <out_dir>
Everything is built from simple shapes; every frame is posed in Python (own easing, two-bone IK
for the arms) and rendered with Cycles using flat toon materials and an inverted-hull outline.
"""
import bpy, math, sys, os
from mathutils import Vector, Quaternion

OUT = sys.argv[sys.argv.index('--') + 1] if '--' in sys.argv else '/tmp/dadi'
os.makedirs(OUT, exist_ok=True)
FPS, SECONDS = 12, 6.0
W, H = 384, 480

# ---------- scene ----------
bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'
sc.cycles.device = 'CPU'
sc.cycles.samples = 24
sc.cycles.use_denoising = False
sc.cycles.max_bounces = 2
sc.render.film_transparent = True
sc.render.resolution_x, sc.render.resolution_y = W, H
sc.render.filter_size = 1.2
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'
sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world
world.use_nodes = True
world.node_tree.nodes['Background'].inputs[1].default_value = 0.0

cam_data = bpy.data.cameras.new('cam'); cam_data.type = 'ORTHO'; cam_data.ortho_scale = 1.32
cam = bpy.data.objects.new('cam', cam_data); sc.collection.objects.link(cam); sc.camera = cam
cam.location = (0, -6, 1.25); cam.rotation_euler = (math.radians(84), 0, 0)

def hexc(h, k=1.0):
    h = h.lstrip('#'); c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return [(x * k) ** 2.2 for x in c] + [1]   # sRGB -> linear

MATS = {}
def toon(name, col, shade=0.84):
    """Flat colour with a soft darker band toward the silhouette (two-tone cel look)."""
    if name in MATS: return MATS[name]
    m = bpy.data.materials.new(name); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear()
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    lw = nt.nodes.new('ShaderNodeLayerWeight'); lw.inputs['Blend'].default_value = 0.45
    ramp = nt.nodes.new('ShaderNodeValToRGB'); ramp.color_ramp.interpolation = 'CONSTANT'
    ramp.color_ramp.elements[0].position = 0.0; ramp.color_ramp.elements[0].color = (0, 0, 0, 1)
    ramp.color_ramp.elements[1].position = 0.62; ramp.color_ramp.elements[1].color = (1, 1, 1, 1)
    e1 = nt.nodes.new('ShaderNodeEmission'); e1.inputs['Color'].default_value = hexc(col)
    e2 = nt.nodes.new('ShaderNodeEmission'); e2.inputs['Color'].default_value = hexc(col, shade)
    mix = nt.nodes.new('ShaderNodeMixShader')
    nt.links.new(lw.outputs['Facing'], ramp.inputs['Fac'])
    nt.links.new(ramp.outputs['Color'], mix.inputs['Fac'])
    nt.links.new(e1.outputs[0], mix.inputs[1]); nt.links.new(e2.outputs[0], mix.inputs[2])
    nt.links.new(mix.outputs[0], out.inputs['Surface'])
    MATS[name] = m; return m

def outline_mat():
    if 'outline' in MATS: return MATS['outline']
    m = bpy.data.materials.new('outline'); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear()
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    geo = nt.nodes.new('ShaderNodeNewGeometry')
    em = nt.nodes.new('ShaderNodeEmission'); em.inputs['Color'].default_value = hexc('#5B3A2E')
    tr = nt.nodes.new('ShaderNodeBsdfTransparent')
    mix = nt.nodes.new('ShaderNodeMixShader')
    nt.links.new(geo.outputs['Backfacing'], mix.inputs['Fac'])
    nt.links.new(em.outputs[0], mix.inputs[1]); nt.links.new(tr.outputs[0], mix.inputs[2])
    nt.links.new(mix.outputs[0], out.inputs['Surface'])
    MATS['outline'] = m; return m

def finish(o, mat, line=0.012, smooth=True):
    o.data.materials.append(mat)
    if smooth:
        for p in o.data.polygons: p.use_smooth = True
    if line > 0:
        o.data.materials.append(outline_mat())
        md = o.modifiers.new('ol', 'SOLIDIFY')
        md.thickness = line; md.offset = 1.0; md.use_flip_normals = True
        md.use_rim = False; md.material_offset = 1
    return o

def sphere(loc, r, scale=(1, 1, 1), mat=None, line=0.012, seg=48):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=seg // 2, radius=r, location=loc)
    o = bpy.context.object; o.scale = scale
    return finish(o, mat, line / (sum(scale) / 3.0))   # outline width stays in world units

def cyl(r, mat, line=0.012):
    bpy.ops.mesh.primitive_cylinder_add(vertices=40, radius=r, depth=1.0)
    o = bpy.context.object; o.rotation_mode = 'QUATERNION'
    return finish(o, mat, line)

def arc(loc, R, r, mat, upper, rot_z=0.0):
    """A thin curved stroke facing the camera: upper=True gives an ∩ (happy closed eye), False a ∪ (smile)."""
    bpy.ops.mesh.primitive_torus_add(major_radius=R, minor_radius=r, major_segments=40, minor_segments=8)
    o = bpy.context.object
    import bmesh
    bm = bmesh.new(); bm.from_mesh(o.data)
    kill = [v for v in bm.verts if (v.co.y < -0.0005 if upper else v.co.y > 0.0005)]
    bmesh.ops.delete(bm, geom=kill, context='VERTS'); bm.to_mesh(o.data); bm.free()
    o.location = loc; o.rotation_euler = (math.radians(90), 0, rot_z)
    return finish(o, mat, 0)

def parent(child, par):
    bpy.context.view_layer.update()
    child.parent = par; child.matrix_parent_inverse = par.matrix_world.inverted()

def empty(name, loc):
    e = bpy.data.objects.new(name, None); e.location = loc; sc.collection.objects.link(e); return e

# ---------- palette ----------
SKIN, HAIR, COAT, SCARF = toon('skin', '#F7D7BE'), toon('hair', '#C4C2C4', 0.8), toon('coat', '#F6EBD5'), toon('scarf', '#EE8E8B', 0.82)
BROWN, EYE, GOLD = toon('btn', '#9C6B4E'), toon('eye', '#3A2622', 1.0), toon('gold', '#C9A04E', 0.85)
SLEEVE = toon('sleeve', '#EADBBE', 0.84)
BLUSH, CUP, TEA = toon('blush', '#F4A6A0', 1.0), toon('cup', '#A9DCC6', 0.84), toon('tea', '#8A5A3C', 1.0)

# ---------- body ----------
BODY_C = Vector((0, 0, 0.20)); BODY_R = (0.40, 0.31, 0.46)
body = sphere(BODY_C, 1.0, BODY_R, COAT, 0.012)
buttons = []
for z in (0.42, 0.28, 0.14):
    t = (z - BODY_C.z) / BODY_R[2]; y = -BODY_R[1] * math.sqrt(max(0, 1 - t * t))
    buttons.append(sphere((0.0, y - 0.005, z), 0.026, (1, 0.5, 1), BROWN, 0.006, 24))
bpy.ops.mesh.primitive_torus_add(major_radius=0.19, minor_radius=0.075, major_segments=48, minor_segments=16, location=(0, -0.01, 0.60))
scarf = bpy.context.object; scarf.scale = (1.1, 0.95, 0.9); finish(scarf, SCARF, 0.012)
tail = sphere((0.11, -0.27, 0.40), 1.0, (0.065, 0.03, 0.17), SCARF, 0.012)
tail.rotation_euler = (math.radians(-12), 0, math.radians(-8))
neck_items = [scarf, tail]

# ---------- head (all parts follow HeadPivot) ----------
NECK = Vector((0, 0, 0.60))
hp = empty('HeadPivot', NECK)
HC = Vector((0, 0, 0.86)); HR = 0.27
head = sphere(HC, HR, (1.06, 0.97, 0.94), SKIN, 0.012, 96)
hair = sphere(HC + Vector((0, 0.05, 0.075)), HR, (1.12, 0.98, 0.84), HAIR, 0.012, 96)
bun = sphere(HC + Vector((0, 0.10, 0.30)), 0.11, (1, 1, 0.9), HAIR, 0.012)
ear_l = sphere(HC + Vector((-0.28, 0.0, -0.03)), 0.05, (0.6, 1, 1), SKIN, 0.012, 24)
ear_r = sphere(HC + Vector((0.28, 0.0, -0.03)), 0.05, (0.6, 1, 1), SKIN, 0.012, 24)
fy = lambda x, dz: -math.sqrt(max(0, (HR * 0.97) ** 2 - (x / 1.06) ** 2 - (dz / 0.94) ** 2))
EZ = -0.03
eyes_open = [sphere((sx * 0.085, fy(0.085, EZ) - 0.004, HC.z + EZ), 0.026, (1, 0.45, 1.2), EYE, 0, 24) for sx in (-1, 1)]
eyes_shut = [arc((sx * 0.085, fy(0.085, EZ) - 0.006, HC.z + EZ - 0.012), 0.03, 0.007, EYE, True) for sx in (-1, 1)]
smile = arc((0, fy(0, -0.10) - 0.006, HC.z - 0.085), 0.03, 0.006, EYE, False)
blush = [sphere((sx * 0.155, fy(0.155, -0.075) - 0.002, HC.z - 0.075), 0.042, (1, 0.25, 0.7), BLUSH, 0, 24) for sx in (-1, 1)]
glasses = []
for sx in (-1, 1):
    bpy.ops.mesh.primitive_torus_add(major_radius=0.058, minor_radius=0.008, major_segments=40, minor_segments=8,
                                     location=(sx * 0.085, fy(0.085, EZ) - 0.03, HC.z + EZ), rotation=(math.radians(90), 0, 0))
    glasses.append(finish(bpy.context.object, GOLD, 0))
bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.006, depth=0.06, location=(0, fy(0, EZ) - 0.035, HC.z + EZ + 0.01), rotation=(0, math.radians(90), 0))
glasses.append(finish(bpy.context.object, GOLD, 0))
for o in [head, hair, bun, ear_l, ear_r, smile] + eyes_open + eyes_shut + blush + glasses:
    parent(o, hp)

# ---------- cup ----------
cp = empty('CupPivot', (0, -0.42, 0.34))
bpy.ops.mesh.primitive_cylinder_add(vertices=40, radius=0.068, depth=0.11, location=cp.location)
cup = finish(bpy.context.object, CUP, 0.01)
bpy.ops.mesh.primitive_cylinder_add(vertices=40, radius=0.058, depth=0.004, location=cp.location + Vector((0, 0, 0.052)))
tea = finish(bpy.context.object, TEA, 0)
bpy.ops.mesh.primitive_torus_add(major_radius=0.035, minor_radius=0.011, location=cp.location + Vector((0.085, 0, 0.005)), rotation=(math.radians(90), 0, 0))
handle = finish(bpy.context.object, CUP, 0.008)
for o in (cup, tea, handle): parent(o, cp)

# ---------- arms ----------
ARM_R, L1, L2 = 0.062, 0.21, 0.20
arms = {}
for side in (-1, 1):
    arms[side] = dict(
        sh=Vector((side * 0.29, -0.02, 0.50)),
        shs=sphere((0, 0, 0), ARM_R + 0.004, (1, 1, 1), SLEEVE, 0.012, 32),
        up=cyl(ARM_R, SLEEVE), lo=cyl(ARM_R * 0.92, SLEEVE),
        el=sphere((0, 0, 0), ARM_R, (1, 1, 1), SLEEVE, 0.012, 32),
        hand=sphere((0, 0, 0), 0.05, (1, 1, 1), SKIN, 0.012, 32),
    )
    arms[side]['shs'].location = arms[side]['sh']

def seg(o, a, b):
    d = b - a
    o.location = (a + b) / 2
    o.scale = (1, 1, max(d.length, 1e-4))
    o.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(d.normalized())

def ik(S, T, pole):
    """Two-bone IK: elbow position for shoulder S reaching target T, bending toward pole."""
    d = T - S; dist = min(max(d.length, 1e-4), L1 + L2 - 1e-4)
    dn = d.normalized()
    a = (L1 * L1 - L2 * L2 + dist * dist) / (2 * dist)
    h = math.sqrt(max(L1 * L1 - a * a, 0))
    p = pole - dn * pole.dot(dn)
    p = p.normalized() if p.length > 1e-6 else Vector((0, 0, -1))
    E = S + dn * a + p * h
    T2 = S + dn * dist
    return E, T2

# ---------- animation ----------
def sm(x): x = min(max(x, 0.0), 1.0); return x * x * x * (x * (6 * x - 15) + 10)
def back(x, s=1.4):
    x = min(max(x, 0.0), 1.0) - 1; return x * x * ((s + 1) * x + s) + 1
def rng(t, a, b): return (t - a) / (b - a)

C_REST = Vector((0, -0.43, 0.33))
C_SIP = Vector((0, -0.40, 0.66))
N = int(FPS * SECONDS)
for f in range(N):
    t = f / FPS
    br = math.sin(2 * math.pi * t / 3.0)
    brl = math.sin(2 * math.pi * t / 3.0 - 0.6)
    dip = sm(rng(t, 1.6, 1.9)) * (1 - sm(rng(t, 1.9, 2.2)))
    lift = sm(rng(t, 1.9, 2.8))
    drink = sm(rng(t, 2.9, 3.7))
    ret = back(rng(t, 3.8, 4.8))
    s = lift * (1 - ret)
    ahh = math.sin(math.pi * min(max(rng(t, 4.6, 5.4), 0), 1))
    # chest breathes; the head rides on top of it
    body.scale = (BODY_R[0] * (1 + 0.006 * br), BODY_R[1], BODY_R[2] * (1 + 0.014 * br))
    lift_z = BODY_R[2] * 0.014 * br * 0.6
    for o in neck_items + buttons:
        o['z0'] = o.get('z0', o.location.z); o.location.z = o['z0'] + lift_z * (o['z0'] - BODY_C.z) / BODY_R[2]
    hp.location = NECK + Vector((0, 0, lift_z + 0.004 * brl - 0.02 * ahh))
    pitch = 9 * s - 14 * drink * s + 7 * ahh + 1.2 * brl      # nod toward the cup, tip back to drink, contented nod after
    roll = 2.2 * math.sin(2 * math.pi * t / 6.0) * (1 - s)
    hp.rotation_euler = (math.radians(pitch), math.radians(roll), 0)
    # eyes: two quick blinks, closed and happy while drinking
    shut = (0.85 < t < 0.97) or (5.3 < t < 5.42) or (2.95 < t < 3.85)
    for o in eyes_open: o.hide_render = shut
    for o in eyes_shut: o.hide_render = not shut
    # cup on an eased path (small dip before the lift), tipped toward the mouth while drinking
    C = C_REST.lerp(C_SIP, s) + Vector((0, 0, -0.03 * dip + 0.006 * brl))
    cp.location = C
    cp.rotation_euler = (math.radians(-28 * drink * s), 0, 0)
    for side in (-1, 1):
        A = arms[side]
        S = A['sh'] + Vector((0, 0, lift_z * 0.6))
        A['shs'].location = S
        T = C + Vector((side * 0.085, -0.005, -0.005))
        pole = Vector((side * 1.0, 0.6, -0.9))
        E, T2 = ik(S, T, pole)
        seg(A['up'], S, E); seg(A['lo'], E, T2)
        A['el'].location = E; A['hand'].location = T2
    sc.frame_set(1)
    sc.render.filepath = os.path.join(OUT, f'f{f:03d}.png')
    bpy.ops.render.render(write_still=True)
    print('frame', f, flush=True)
