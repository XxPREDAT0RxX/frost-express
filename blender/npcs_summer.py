"""Little Express: the warm-weather passengers (hot biomes), built with the same kit as blender/npcs.py.

    blender -b -P blender/npcs_summer.py -- <out_dir> [--fast] [--only s01,s02]

Same frames as npcs.py (idle_XX, walk_XX, side_XX, sit_XX per character folder s01..s08).
"""
import os, sys

def SPEC_HOOK(g):
    hair_cap, ponytail, pigtails, bun, side_part, spikes = g['hair_cap'], g['ponytail'], g['pigtails'], g['bun'], g['side_part'], g['spikes']
    sun_hat, cap, bucket_hat, headphones, mustache, long_hair = g['sun_hat'], g['cap'], g['bucket_hat'], g['headphones'], g['mustache'], g['long_hair']
    return [
        # s01 boy: red cap, yellow tee, blue shorts, sneakers
        dict(name='s01', skin='wheat', scale=0.82, coat='#F7D25C', pants='#4A7BC8', boots='#F2F2F2', shorts=True, short_sleeve=True,
             head_extra=[lambda: hair_cap('#2B2220'), lambda: cap('#E0565B', '#C9474C')]),
        # s02 woman: straw sun hat, light blue sundress, sandals
        dict(name='s02', skin='fair', coat='#8FC8E8', pants='#8FC8E8', boots='#C8643C', shorts=True, short_sleeve=True, sandals=True,
             coat_len=0.14, bag='#F2C14E', head_extra=[lambda: long_hair('#8A5A3C'), lambda: hair_cap('#8A5A3C'), lambda: sun_hat('#E9C98A', '#E0565B')]),
        # s03 grandpa: panama hat, flower shirt, khaki trousers, sandals
        dict(name='s03', skin='tan', coat='#F08A5C', stripes=('#F7D25C', 4.0), pants='#C9B48A', boots='#6B3F2A', short_sleeve=True, sandals=True,
             glasses='#4A3A36', head_extra=[lambda: hair_cap('#D8D4CE', top=0.02), lambda: mustache('#EDEAE4'), lambda: sun_hat('#F3E6CC', '#3B2A20')]),
        # s04 little girl: pigtails, pink tee, white skirt
        dict(name='s04', skin='brown', scale=0.78, coat='#F28FB0', pants='#F5F1E6', boots='#F2C14E', shorts=True, short_sleeve=True, sandals=True,
             coat_len=0.04, head_extra=[lambda: hair_cap('#231B19'), lambda: pigtails('#231B19', '#F7D25C')]),
        # s05 tourist: sunglasses, green cap, green tee, khaki shorts, backpack
        dict(name='s05', skin='fair', coat='#6DB45A', pants='#C9B48A', boots='#8A5A3C', shorts=True, short_sleeve=True, backpack='#E07A3F',
             sunglasses='#2B2220', head_extra=[lambda: hair_cap('#C8A060'), lambda: cap('#3F8A5A', '#2E6E45')]),
        # s06 aunty: light saree-coloured kurta, bun, handbag, bindi
        dict(name='s06', skin='brown', coat='#F7B2C8', pants='#F5E6D0', boots='#C8643C', short_sleeve=True, sandals=True, coat_len=0.18,
             bindi='#C8343C', bag='#9C3B4A', shawl='#F4C542', head_extra=[lambda: hair_cap('#231B19'), lambda: bun('#231B19', low=True)]),
        # s07 teen: bucket hat, headphones, white tee, black shorts
        dict(name='s07', skin='wheat', coat='#F5F2EA', pants='#2F3448', boots='#E0565B', shorts=True, short_sleeve=True,
             head_extra=[lambda: hair_cap('#2B2220', top=0.03), lambda: bucket_hat('#5BAF7A'), lambda: headphones('#2F3448')]),
        # s08 woman: orange top, denim shorts, sunglasses, high bun
        dict(name='s08', skin='tan', coat='#F08A3C', pants='#5E86B8', boots='#F2F2F2', shorts=True, short_sleeve=True, sandals=True,
             sunglasses='#3B2622', head_extra=[lambda: hair_cap('#3B2A20'), lambda: bun('#3B2A20')]),
    ]

HERE = os.path.dirname(os.path.abspath(__file__))
exec(compile(open(os.path.join(HERE, 'npcs.py')).read(), os.path.join(HERE, 'npcs.py'), 'exec'))
