"""Little Express: the "look out of the window" view in Blender.

    blender -b -P blender/window.py -- <out_dir> [--only cabin,meadow,snow]

Two kinds of picture:
  cabin.png         the coach interior seen from your seat (wall with two window openings, rubber seals,
                    metal frames, centre pillar, luggage rack, wooden sill with a potted plant, the headrest
                    of the seat ahead and your armrests), modelled and lit in 3D; the panes are transparent.
                    1080 x 2340, the window holes where WindowView.kt has them.
  <biome>_far.png, <biome>_mid.png, <biome>_near.png
                    seamless strips (2048 px wide) of the scenery outside, for parallax scrolling:
                    far = rolling hills with a hazy treeline, mid = the painted trees and houses of that land,
                    near = fence posts, wires and grass right beside the track.
"""
import bpy, bmesh, math, os, sys, random
from mathutils import Vector

ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
OUT = next((a for i, a in enumerate(ARGS) if not a.startswith('--') and (i == 0 or ARGS[i - 1] != '--only')), '/tmp/window')
os.makedirs(OUT, exist_ok=True)
ONLY = ARGS[ARGS.index('--only') + 1].split(',') if '--only' in ARGS else None
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, 'painted')

bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine = 'CYCLES'; sc.cycles.device = 'CPU'
sc.render.film_transparent = True
sc.view_settings.view_transform = 'Standard'
sc.render.image_settings.file_format = 'PNG'; sc.render.image_settings.color_mode = 'RGBA'
world = bpy.data.worlds.new('w'); sc.world = world; world.use_nodes = True
bg = world.node_tree.nodes['Background']
camd = bpy.data.cameras.new('cam'); camd.type = 'ORTHO'
CAM = bpy.data.objects.new('cam', camd); sc.collection.objects.link(CAM); sc.camera = CAM
PX = 0.01
CUR = []

def lin(h, k=1.0):
    h = h.lstrip('#'); return [min(1.0, int(h[i:i + 2], 16) / 255 * k) ** 2.2 for i in (0, 2, 4)] + [1]

def reg(o): CUR.append(o); return o

def clear():
    for o in CUR: bpy.data.objects.remove(o, do_unlink=True)
    CUR.clear()

def shot(name, w, h, samples):
    sc.cycles.samples = samples
    sc.render.resolution_x, sc.render.resolution_y = w, h
    sc.render.filepath = os.path.join(OUT, name + '.png')
    bpy.ops.render.render(write_still=True)
    print('done', name, flush=True)

# ------------------------------------------------------------------ materials
MATS = {}
def mat(col, rough=0.6, sheen=0.0, metal=0.0, emit=None, alpha=1.0, bump=0.0, bump_scale=30.0):
    key = (col, rough, sheen, metal, emit, alpha, bump, bump_scale)
    if key in MATS: return MATS[key]
    m = bpy.data.materials.new('m'); m.use_nodes = True
    nt = m.node_tree; b = nt.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = lin(col); b.inputs['Roughness'].default_value = rough
    b.inputs['Metallic'].default_value = metal
    if sheen: b.inputs['Sheen Weight'].default_value = sheen
    if emit: b.inputs['Emission Color'].default_value = lin(emit); b.inputs['Emission Strength'].default_value = 2.0
    if alpha < 1: b.inputs['Alpha'].default_value = alpha
    if bump > 0:   # fine fabric / paint grain
        n = nt.nodes.new('ShaderNodeTexNoise'); n.inputs['Scale'].default_value = bump_scale; n.inputs['Detail'].default_value = 6
        bp = nt.nodes.new('ShaderNodeBump'); bp.inputs['Strength'].default_value = bump; bp.inputs['Distance'].default_value = 0.01
        nt.links.new(n.outputs['Fac'], bp.inputs['Height']); nt.links.new(bp.outputs['Normal'], b.inputs['Normal'])
    MATS[key] = m; return m

def image_mat(name):
    """A painted sprite as an unlit picture with its own transparency."""
    m = bpy.data.materials.new('img'); m.use_nodes = True
    nt = m.node_tree; nt.nodes.clear(); L = nt.links.new
    out = nt.nodes.new('ShaderNodeOutputMaterial')
    tx = nt.nodes.new('ShaderNodeTexImage'); tx.image = bpy.data.images.load(os.path.join(SRC, name + '.png')); tx.extension = 'CLIP'
    em = nt.nodes.new('ShaderNodeEmission'); L(tx.outputs['Color'], em.inputs['Color'])
    tr = nt.nodes.new('ShaderNodeBsdfTransparent'); mx = nt.nodes.new('ShaderNodeMixShader')
    L(tx.outputs['Alpha'], mx.inputs[0]); L(tr.outputs[0], mx.inputs[1]); L(em.outputs[0], mx.inputs[2])
    L(mx.outputs[0], out.inputs['Surface'])
    return m, tx.image.size

# ------------------------------------------------------------------ shapes
def rbox(loc, size, m, bevel=0.05, segs=4):
    bpy.ops.mesh.primitive_cube_add(size=1, location=loc)
    o = bpy.context.object; o.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    if bevel > 0:
        b = o.modifiers.new('bv', 'BEVEL'); b.width = min(bevel, min(size) * 0.49); b.segments = segs
    o.data.materials.append(m)
    for p in o.data.polygons: p.use_smooth = True
    return reg(o)

def cut(o, cutter):
    md = o.modifiers.new('cut', 'BOOLEAN'); md.operation = 'DIFFERENCE'; md.object = cutter; md.solver = 'EXACT'
    cutter.hide_render = True
    return o

def cyl(loc, r, h, m, verts=32, r2=None):
    bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r, radius2=r if r2 is None else r2, depth=h, location=loc)
    o = bpy.context.object; o.data.materials.append(m)
    for p in o.data.polygons: p.use_smooth = True
    return reg(o)

def billboard(name, x, z, height, y=0.0, flip=False):
    """A painted sprite standing with its bottom edge at (x, z), [height] units tall, facing the camera (-Y)."""
    m, (w, h) = image_mat(name)
    wd = height * w / h
    bpy.ops.mesh.primitive_plane_add(size=1, location=(x, y, z + height / 2), rotation=(math.radians(90), 0, 0))
    o = bpy.context.object; o.scale = (wd * (-1 if flip else 1), height, 1)
    o.data.materials.append(m)
    return reg(o)

# ------------------------------------------------------------------ the cabin
W_PX, H_PX = 1080, 2340
def cabin():
    W, H = W_PX * PX, H_PX * PX
    # screen (x right, z up) with the origin at the bottom-left corner of the picture; the camera looks along +Y
    def S(fx, fy):   # fractions of the screen from the top-left, like WindowView.kt
        return fx * W, H - fy * H
    wall = mat('#EADFC8', rough=0.75, bump=0.08, bump_scale=60)
    wainscot = mat('#D3C19C', rough=0.7, bump=0.1, bump_scale=40)
    gasket = mat('#2F3842', rough=0.45)
    frame = mat('#8A96A4', rough=0.3, metal=0.8)
    wood = mat('#B88A52', rough=0.5, bump=0.15, bump_scale=8)
    wood_d = mat('#8E6A3E', rough=0.5)
    fabric = mat('#5F86AE', rough=0.95, sheen=0.4, bump=0.25, bump_scale=120)
    fabric_d = mat('#465F7E', rough=0.95, sheen=0.3, bump=0.25, bump_scale=120)
    arm = mat('#3A4552', rough=0.6, bump=0.12, bump_scale=60)
    brass = mat('#C9A04E', rough=0.3, metal=0.9)
    mx, top, bottom = 0.055, 0.145, 0.72
    rr = 0.045 * W; pill = 0.028
    panes = [(mx, 0.5 - pill), (0.5 + pill, 1 - mx)]
    # the wall, with two rounded openings
    wl = rbox((W / 2, 0.15, H / 2), (W * 1.2, 0.3, H * 1.2), wall, bevel=0)
    for (a, b) in panes:
        x0, z1 = S(a, top); x1, z0 = S(b, bottom)
        c = rbox(((x0 + x1) / 2, 0.15, (z0 + z1) / 2), (x1 - x0, 1.0, z1 - z0), wall, bevel=rr, segs=8)
        cut(wl, c)
        # rubber seal and metal frame: rounded rings set into the opening
        g = rbox(((x0 + x1) / 2, 0.02, (z0 + z1) / 2), (x1 - x0 + 0.5, 0.14, z1 - z0 + 0.5), gasket, bevel=rr + 0.25, segs=8)
        gc = rbox(((x0 + x1) / 2, 0.02, (z0 + z1) / 2), (x1 - x0 - 0.02, 1.0, z1 - z0 - 0.02), gasket, bevel=rr, segs=8)
        cut(g, gc)
        f = rbox(((x0 + x1) / 2, -0.04, (z0 + z1) / 2), (x1 - x0 + 0.12, 0.06, z1 - z0 + 0.12), frame, bevel=rr + 0.06, segs=8)
        fc = rbox(((x0 + x1) / 2, -0.04, (z0 + z1) / 2), (x1 - x0 - 0.1, 1.0, z1 - z0 - 0.1), frame, bevel=rr - 0.05, segs=8)
        cut(f, fc)
        # a faint pane of glass: mostly clear, catching a little light
        bpy.ops.mesh.primitive_plane_add(size=1, location=((x0 + x1) / 2, 0.2, (z0 + z1) / 2), rotation=(math.radians(90), 0, 0))
        gl = reg(bpy.context.object); gl.scale = (x1 - x0, z1 - z0, 1)
        gm = bpy.data.materials.new('glass'); gm.use_nodes = True; nt = gm.node_tree; nt.nodes.clear()
        o_ = nt.nodes.new('ShaderNodeOutputMaterial'); tr = nt.nodes.new('ShaderNodeBsdfTransparent')
        em = nt.nodes.new('ShaderNodeEmission'); em.inputs['Color'].default_value = (1, 1, 1, 1); em.inputs['Strength'].default_value = 1
        lw = nt.nodes.new('ShaderNodeTexCoord'); grad = nt.nodes.new('ShaderNodeTexGradient'); grad.gradient_type = 'DIAGONAL'
        ramp = nt.nodes.new('ShaderNodeValToRGB'); ramp.color_ramp.elements[0].position = 0.42; ramp.color_ramp.elements[0].color = (0, 0, 0, 1)
        ramp.color_ramp.elements[1].position = 0.5; ramp.color_ramp.elements[1].color = (0.13, 0.13, 0.13, 1)
        e3 = ramp.color_ramp.elements.new(0.62); e3.color = (0, 0, 0, 1)
        mx_ = nt.nodes.new('ShaderNodeMixShader')
        nt.links.new(lw.outputs['UV'], grad.inputs['Vector']); nt.links.new(grad.outputs['Fac'], ramp.inputs['Fac'])
        nt.links.new(ramp.outputs['Color'], mx_.inputs[0]); nt.links.new(tr.outputs[0], mx_.inputs[1]); nt.links.new(em.outputs[0], mx_.inputs[2])
        nt.links.new(mx_.outputs[0], o_.inputs['Surface']); gl.data.materials.append(gm)
    # lower wainscot band with a rail
    rbox((W / 2, -0.03, (1 - 0.91) * H), (W * 1.2, 0.08, 0.18 * H), wainscot, bevel=0.02)
    rbox((W / 2, -0.09, (1 - 0.82) * H), (W * 1.2, 0.08, 0.08), wood_d, bevel=0.03)
    # centre pillar rivets
    for fy in (top + 0.03, bottom - 0.03):
        x, z = S(0.5, fy); bpy.ops.mesh.primitive_uv_sphere_add(radius=0.05, location=(x, -0.02, z)); s_ = reg(bpy.context.object); s_.data.materials.append(brass)
    # luggage rack above the windows: a wooden bar on brass brackets with a rail
    x0, z = S(mx - 0.02, top - 0.04)
    rbox((W / 2, -0.25, z), (W - 2 * x0, 0.5, 0.18 * H / 23.4 * 3), wood, bevel=0.06)
    rbox((W / 2, -0.52, z - 0.06), (W - 2 * x0, 0.05, 0.05), brass, bevel=0.02)
    for fx in (mx + 0.04, 1 - mx - 0.04):
        x, _ = S(fx, 0); rbox((x, -0.15, z - 0.35), (0.08, 0.3, 0.5), brass, bevel=0.03)
    # the sill: a deep wooden ledge below the panes
    _, zs = S(0, bottom + 0.012)
    rbox((W / 2, -0.35, zs - 0.25), (W - 2 * (mx - 0.03) * W, 0.75, 0.5), wood, bevel=0.06)
    # a potted plant on the sill (pot modelled, the plant is the painted bush)
    px, _ = S(1 - mx - 0.06, 0)
    cyl((px, -0.35, zs + 0.2), 0.26, 0.42, mat('#C8643C', rough=0.55, bump=0.1), r2=0.32)
    cyl((px, -0.35, zs + 0.42), 0.33, 0.06, mat('#A84E2E', rough=0.6))
    billboard('bush', px, zs + 0.38, 0.75, y=-0.4)
    # the seat ahead (headrest low in the view) and your armrests
    hx, hz = S(0.5, 0.86)
    rbox((hx, -1.2, hz - 2.4), (0.46 * W, 0.7, 4.6), fabric_d, bevel=0.7, segs=10)            # seat back
    rbox((hx, -1.45, hz - 2.2), (0.4 * W, 0.4, 4.1), fabric, bevel=0.6, segs=10)              # its padded face
    rbox((hx, -1.5, hz + 0.05), (0.3 * W, 0.6, 0.9), fabric, bevel=0.4, segs=10)              # headrest cushion
    rbox((hx, -1.82, hz + 0.05), (0.26 * W, 0.04, 0.08), mat('#F3EEE2', rough=0.9), bevel=0.02)  # white antimacassar band
    for dx in (-0.12, 0.12):                                                                   # piping down the back
        rbox((hx + dx * W, -1.68, hz - 2.2), (0.03, 0.04, 3.6), fabric_d, bevel=0.015)
    for sx in (0.02, 0.98):
        x, _ = S(sx, 0)
        rbox((x, -2.0, 1.0), (1.5, 1.2, 2.6), arm, bevel=0.4, segs=6)
        rbox((x, -2.05, 2.25), (1.55, 1.25, 0.18), mat('#4C5A68', rough=0.5), bevel=0.08)
    # light: daylight pouring in through the windows, a warm lamp from above, soft fill
    bpy.ops.object.light_add(type='AREA', location=(W / 2, 6, H * 0.55), rotation=(math.radians(90), 0, math.radians(180)))
    l1 = reg(bpy.context.object); l1.data.energy = 3500; l1.data.size = W; l1.data.size_y = H * 0.6; l1.data.shape = 'RECTANGLE'; l1.data.color = (1.0, 0.97, 0.9)
    bpy.ops.object.light_add(type='AREA', location=(W * 0.3, -8, H * 0.95), rotation=(math.radians(60), 0, 0))
    l2 = reg(bpy.context.object); l2.data.energy = 900; l2.data.size = 14; l2.data.color = (1.0, 0.88, 0.72)
    bpy.ops.object.light_add(type='AREA', location=(W / 2, -12, H * 0.4), rotation=(math.radians(90), 0, 0))
    l3 = reg(bpy.context.object); l3.data.energy = 1400; l3.data.size = 14; l3.data.color = (0.85, 0.9, 1.0)
    bg.inputs[0].default_value = (0.6, 0.62, 0.66, 1); bg.inputs[1].default_value = 0.25
    sc.cycles.max_bounces = 4; sc.cycles.use_denoising = True
    camd.ortho_scale = H
    CAM.location = (W / 2, -30, H / 2); CAM.rotation_euler = (math.radians(90), 0, 0)
    shot('cabin', W_PX, H_PX, 96)
    clear()

# ------------------------------------------------------------------ scenery strips
T_PX = 2048
# in the order of the game's lands (ZONES in TrainScene / VIEW_PALETTES in WindowView)
BIOMES = {
    'meadow': dict(hills=('#9FD08A', '#6FB05C'), haze='#D6F1FF', far_tree='tree_tall',
                   mid=[('tree_round', 2.4), ('tree_tall', 2.6), ('tree_round', 2.1), ('bush', 1.1), ('v_house_b', 1.7), ('tree_tall', 2.2), ('bush', 0.9)],
                   near=[('grass_tuft', 0.55), ('bush', 0.6), ('grass_tuft', 0.45)], post='#8A6A4A'),
    'pine': dict(hills=('#6E9E63', '#4E7E48'), haze='#CDEAF6', far_tree='tree_tall',
                 mid=[('tree_tall', 2.8), ('pine_a', 2.4), ('tree_tall', 2.3), ('fern', 0.9), ('boulder', 0.8), ('tree_tall', 2.6), ('logs', 0.8)],
                 near=[('fern', 0.6), ('grass_tuft', 0.5), ('boulder', 0.4)], post='#7A5A44'),
    'savanna': dict(hills=('#CDB06A', '#B89A52'), haze='#FBEFC8', far_tree='acacia',
                    mid=[('acacia', 2.2), ('dry_bush', 0.8), ('boulder', 0.9), ('acacia', 1.9), ('grass_tuft', 0.7), ('tent', 1.2)],
                    near=[('grass_tuft', 0.6), ('dry_bush', 0.55), ('grass_tuft', 0.5)], post='#9A7A54'),
    'desert': dict(hills=('#D6A368', '#C08A52'), haze='#FBE6C0', far_tree='cactus',
                   mid=[('cactus', 2.0), ('mesa', 1.9), ('cactus_ball', 0.8), ('boulder', 0.8), ('cactus', 1.6), ('dry_bush', 0.7)],
                   near=[('dry_bush', 0.55), ('cactus_ball', 0.45), ('boulder', 0.4)], post='#8E6A48'),
    'rainforest': dict(hills=('#5E9E78', '#3E7E58'), haze='#DAF2E6', far_tree='palm',
                       mid=[('palm', 2.7), ('jungle', 1.6), ('v_banana', 2.1), ('fern', 1.0), ('tree_round', 2.2), ('palm', 2.4)],
                       near=[('fern', 0.7), ('jungle', 0.7), ('fern', 0.55)], post='#6E5238'),
    'snow': dict(hills=('#CAD8E4', '#AFC2D2'), haze='#EAF4FB', far_tree='frost_pine_c',
                 mid=[('frost_pine_a', 2.7), ('frost_pine_b', 2.2), ('snowman', 1.2), ('frost_pine_c', 1.8), ('dead_b', 2.2), ('snow_bush', 1.0), ('logs', 0.9)],
                 near=[('frost_shrub', 0.6), ('rock_a', 0.5), ('snow_bush', 0.55)], post='#7A5A44'),
    'village': dict(hills=('#8EC27A', '#6BA85A'), haze='#E2F3E4', far_tree='v_palm',
                    mid=[('v_palm', 2.8), ('v_house_a', 1.7), ('v_banana', 2.0), ('v_haystack', 1.3), ('v_bamboo', 2.3), ('v_house_b', 1.7), ('v_well', 1.1)],
                    near=[('v_vgrass', 0.6), ('v_vbush', 0.6), ('v_pots', 0.45)], post='#8A6A4A'),
}

def periodic_height(x, T, seed, amp, waves=(1, 2, 3, 5)):
    rnd = random.Random(seed)
    return sum(amp / (i + 1) * math.sin(2 * math.pi * k * x / T + rnd.uniform(0, 6.28)) for i, k in enumerate(waves))

def hills_strip(name, b, seed):
    T = T_PX * PX; Hh = 4.0
    cols = [b['hills'][0], b['hills'][1]]
    for layer, (col, base, amp, yd) in enumerate(((cols[0], 1.6, 0.5, 2.0), (cols[1], 1.0, 0.65, 0.0))):
        # a ridge mesh, a little deep, so the light shades its slopes
        bm = bmesh.new(); n = 256; prev = None
        for i in range(n + 1):
            x = T * i / n
            top = base + amp * (0.5 + 0.5 * periodic_height(x, T, seed + layer, 1.0) / 1.6)
            vs = [bm.verts.new((x, yd + d, z)) for d, z in ((0, 0), (0, top), (1.5, top + 0.3), (1.5, 0))]
            if prev:
                for a in range(3): bm.faces.new((prev[a], vs[a], vs[a + 1], prev[a + 1]))
            prev = vs
        me = bpy.data.meshes.new('h'); bm.to_mesh(me); bm.free()
        o = bpy.data.objects.new('h', me); sc.collection.objects.link(o); reg(o)
        for p in me.polygons: p.use_smooth = True
        haze = 0.45 if layer == 0 else 0.15
        c = [a * (1 - haze) + bb * haze for a, bb in zip(lin(col), lin(b['haze']))]
        m = bpy.data.materials.new('hill'); m.use_nodes = True; bs = m.node_tree.nodes['Principled BSDF']
        bs.inputs['Base Color'].default_value = c; bs.inputs['Roughness'].default_value = 1.0
        o.data.materials.append(m)
        # a hazy treeline along the far ridge: little painted trees, faded
        if layer == 0:
            rnd = random.Random(seed)
            tree = b['far_tree']
            for k in range(40):
                x = T * (k + rnd.uniform(-0.3, 0.3)) / 40
                top = base + amp * (0.5 + 0.5 * periodic_height(x, T, seed + layer, 1.0) / 1.6)
                for dx in (-T, 0, T):
                    bb_ = billboard(tree, x + dx, top - 0.08, rnd.uniform(0.28, 0.42), y=yd - 0.05)
    bpy.ops.object.light_add(type='SUN', location=(0, -5, 10), rotation=(math.radians(50), math.radians(-35), 0))
    s = reg(bpy.context.object); s.data.energy = 3.2; s.data.color = (1, 0.97, 0.9)
    bg.inputs[0].default_value = lin(b['haze']); bg.inputs[1].default_value = 0.9
    sc.cycles.max_bounces = 2; sc.cycles.use_denoising = True
    camd.ortho_scale = T
    CAM.location = (T / 2, -30, Hh / 2); CAM.rotation_euler = (math.radians(90), 0, 0)
    shot('%s_far' % name, T_PX, int(Hh / PX), 32)
    clear()

def items_strip(name, b, which, seed, Hh, ground, count, jitter):
    """Painted sprites spaced along a seamless strip (each one also copied one strip-width left and right)."""
    T = T_PX * PX
    rnd = random.Random(seed)
    items = b[which]
    placed = []
    for k in range(count):
        n, hgt = items[k % len(items)]
        x = T * (k + rnd.uniform(-jitter, jitter)) / count
        placed.append((rnd.uniform(0, 0.3), x, n, hgt * rnd.uniform(0.85, 1.12), rnd.random() < 0.5))
    for depth, x, n, hgt, fl in sorted(placed, reverse=True):
        for dx in (-T, 0, T):
            billboard(n, x + dx, ground - depth * 0.6, hgt, y=depth, flip=fl and not n.startswith('v_house'))
    if which == 'near':   # fence: posts and two wires right by the track
        wood = mat(b['post'], rough=0.8, bump=0.3, bump_scale=12)
        wire = mat('#5A4636', rough=0.6)
        for k in range(14):
            x = T * k / 14
            for dx in (-T, 0, T):
                rbox((x + dx, -0.6, ground + 0.55), (0.12, 0.12, 1.1), wood, bevel=0.02)
                rbox((x + dx, -0.6, ground + 1.12), (0.16, 0.16, 0.05), wood, bevel=0.02)
        for z in (ground + 0.95, ground + 0.6):
            rbox((T / 2, -0.67, z), (T * 3, 0.02, 0.02), wire, bevel=0)
        bpy.ops.object.light_add(type='SUN', location=(0, -5, 10), rotation=(math.radians(50), math.radians(-35), 0))
        s = reg(bpy.context.object); s.data.energy = 3.5
        bg.inputs[0].default_value = (0.8, 0.85, 0.9, 1); bg.inputs[1].default_value = 0.8
    sc.cycles.max_bounces = 2; sc.cycles.use_denoising = which == 'near'
    camd.ortho_scale = T
    CAM.location = (T / 2, -30, Hh / 2); CAM.rotation_euler = (math.radians(90), 0, 0)
    shot('%s_%s' % (name, which), T_PX, int(Hh / PX), 16 if which == 'mid' else 32)
    clear()

def clouds():
    """Four fluffy cumulus clouds: white puffs lit from above, bluish underneath, with the art's brown outline."""
    for n in range(4):
        rnd = random.Random(70 + n)
        m_top = mat('#FFFFFF', rough=1.0); m_low = mat('#DCE7F2', rough=1.0)
        puffs = []
        cnt = 5 + n
        for i in range(cnt):
            x = (i - (cnt - 1) / 2) * 0.55 + rnd.uniform(-0.12, 0.12)
            r = 0.55 * (1 - abs(x) / (cnt * 0.4)) + rnd.uniform(0.05, 0.2)
            bpy.ops.mesh.primitive_uv_sphere_add(radius=max(0.25, r), location=(x, 0, max(0.25, r) * 0.55 + rnd.uniform(-0.05, 0.1)), segments=32, ring_count=16)
            o = reg(bpy.context.object); o.data.materials.append(m_top)
            for p_ in o.data.polygons: p_.use_smooth = True
            puffs.append(o)
        rbox((0, 0, 0.12), (cnt * 0.55 + 0.4, 0.9, 0.3), m_low, bevel=0.14)    # flat, shaded base
        bpy.ops.object.light_add(type='SUN', rotation=(math.radians(25), math.radians(-20), 0))
        s_ = reg(bpy.context.object); s_.data.energy = 3.0
        bg.inputs[0].default_value = (0.62, 0.74, 0.9, 1); bg.inputs[1].default_value = 1.0
        sc.cycles.max_bounces = 3; sc.cycles.use_denoising = True
        camd.ortho_scale = cnt * 0.55 + 1.6
        CAM.location = (0, -30, 0.6); CAM.rotation_euler = (math.radians(90), 0, 0)
        shot('cloud%d' % (n + 1), 640, 280, 48)
        clear()

JOBS = [('cabin', cabin), ('clouds', clouds)]

for bname, b in BIOMES.items():
    JOBS.append((bname, lambda bname=bname, b=b: (hills_strip(bname, b, 11),
                                                    items_strip(bname, b, 'mid', 21, 3.4, 0.35, 11, 0.3),
                                                    items_strip(bname, b, 'near', 31, 1.8, 0.2, 16, 0.25))))
for name, job in JOBS:
    if ONLY and name not in ONLY: continue
    job()
