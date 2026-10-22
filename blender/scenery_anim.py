"""Little Express: looping animations for some roadside items, built with blender/scenery.py.

    blender -b -P blender/scenery_anim.py -- <out_dir> [--only palm,turbine]

Writes <out_dir>/<name>_NN.png frames, all from one fixed camera so they line up:
trees, palms, bamboo, grass and flowers sway in the wind (the shape bends more toward the top),
the wind turbine turns, the campfire flickers, the lighthouse lamp pulses.
store_assets/make_env_sprites.py packs them into spra_<name>.webp strips.
"""
import os, math

SWAY = {   # name: (frames, sway as a fraction of the height)
    'tree_round': (12, 0.035), 'tree_tall': (12, 0.03), 'acacia': (12, 0.025), 'palm': (12, 0.05), 'v_palm': (12, 0.05),
    'v_banana': (12, 0.045), 'v_bamboo': (12, 0.05), 'jungle': (12, 0.04), 'grass_tuft': (12, 0.07), 'v_vgrass': (12, 0.07),
    'flowers': (12, 0.06), 'frost_pine_a': (12, 0.02), 'frost_pine_b': (12, 0.02), 'frost_pine_c': (12, 0.02), 'fern': (12, 0.035),
}
SPIN = {'turbine': 12}
FLICKER = {'campfire': 8, 'lighthouse': 8}

def ANIM_HOOK(g):
    from mathutils import Vector, Matrix
    ITEMS, CUR, LINE = g['ITEMS'], g['CUR'], g['LINE']
    frame_and_render, shot, clear, ONLY = g['frame_and_render'], g['shot'], g['clear'], g['ONLY']
    names = list(SWAY) + list(SPIN) + list(FLICKER)
    for name in names:
        if ONLY and name not in ONLY: continue
        fn, cam = ITEMS[name]
        LINE[0] = 0.022
        fn()
        meshes = [o for o in CUR if o.type == 'MESH']
        g['bpy'].context.view_layer.update()
        zs = [(o.matrix_world @ Vector(c)).z for o in meshes for c in o.bound_box]
        z0, z1 = min(zs), max(zs); H = z1 - z0
        n = SWAY.get(name, (SPIN.get(name) or FLICKER.get(name), 0))[0]
        amp = SWAY[name][1] * H if name in SWAY else 0.0
        el, yw = g['view'](cam)   # same angle as the stills (and the stations)
        frame_and_render(name, elev=el, yaw=yw, res=700, pad=1.06, margin=amp * 1.3, shoot=False)
        # remember the rest pose
        base = {o.name: [v.co.copy() for v in o.data.vertices] for o in meshes}
        mats = {o.name: o.matrix_world.copy() for o in meshes}
        blades = meshes[-3:] if name == 'turbine' else []
        flames = [o for o in meshes if o.name.startswith('Sphere') or o.name.startswith('Cone')][-5:] if name == 'campfire' else []
        for i in range(n):
            ph = 2 * math.pi * i / n
            if name in SWAY:
                dx = amp * math.sin(ph); dy = amp * 0.3 * math.sin(ph * 2 + 0.7)
                for o in meshes:
                    if o.get('ground'): continue
                    inv = o.matrix_world.to_3x3().inverted()
                    for v, co in zip(o.data.vertices, base[o.name]):
                        w = (o.matrix_world @ co).z
                        f = max(0.0, (w - z0) / H) ** 1.6
                        v.co = co + inv @ Vector((dx * f, dy * f, 0))
                    o.data.update()
            elif name == 'turbine':
                hub = Vector((0, -0.24, 3.45))
                a = 2 * math.pi / 3 * i / n          # 120 degrees per loop (3 blades look the same)
                R_ = Matrix.Translation(hub) @ Matrix.Rotation(a, 4, 'Y') @ Matrix.Translation(-hub)
                for o in blades: o.matrix_world = R_ @ mats[o.name]
            elif name == 'campfire':
                for k, o in enumerate(flames):
                    s = 1 + 0.18 * math.sin(ph * (2 + k % 2) + k * 1.7)
                    m = mats[o.name]; loc = m.to_translation()
                    bottom = Vector((loc.x, loc.y, 0.05))
                    o.matrix_world = Matrix.Translation(bottom) @ Matrix.Diagonal((1 / math.sqrt(s), 1 / math.sqrt(s), s, 1)) @ Matrix.Translation(-bottom) @ m
            elif name == 'lighthouse':
                for o in meshes:
                    mt = o.data.materials[0] if o.data.materials else None
                    if mt and mt.node_tree and 'Emission' in mt.node_tree.nodes and abs(o.matrix_world.to_translation().z - 2.81) < 0.05:
                        mt.node_tree.nodes['Emission'].inputs['Strength'].default_value = 1.0 + 0.6 * (0.5 + 0.5 * math.sin(ph))
            shot('%s_%02d' % (name, i))
        print('done', name, flush=True)
        clear()

HERE = os.path.dirname(os.path.abspath(__file__))
exec(compile(open(os.path.join(HERE, 'scenery.py')).read(), os.path.join(HERE, 'scenery.py'), 'exec'))
