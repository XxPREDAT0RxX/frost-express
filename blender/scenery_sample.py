"""Renders a few scenery items with blender/scenery.py, for quick looks at a style change.

    blender -b -P blender/scenery_sample.py -- <out_dir>
"""
import os, sys
SAMPLE = 'tree_round,tree_tall,v_house_a,v_well,logs,cliff,bush,rock_a'
if '--only' not in sys.argv: sys.argv += ['--only', SAMPLE]
HERE = os.path.dirname(os.path.abspath(__file__))
exec(compile(open(os.path.join(HERE, 'scenery.py')).read(), os.path.join(HERE, 'scenery.py'), 'exec'))
