"""Renders only the extra scenery animations of blender/projected.py (houses, well, flags, boats, lights...).

    blender -b -P blender/projected_more.py -- <out_dir>
"""
import os, sys
if '--only' not in sys.argv:
    sys.argv += ['--only', 'v_house_a,v_well,tent,lighthouse,ship,buoy,snowman,crane,v_paddy,v_bed,v_haystack,lamp']
HERE = os.path.dirname(os.path.abspath(__file__))
exec(compile(open(os.path.join(HERE, 'projected.py')).read(), os.path.join(HERE, 'projected.py'), 'exec'))
