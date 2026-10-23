"""Renders a few of the scenery animations (blender/scenery_anim.py), for quick looks at a style change.

    blender -b -P blender/scenery_anim_sample.py -- <out_dir>
"""
import os, sys
if '--only' not in sys.argv: sys.argv += ['--only', 'tree_round,tree_tall,campfire,flowers']
HERE = os.path.dirname(os.path.abspath(__file__))
exec(compile(open(os.path.join(HERE, 'scenery_anim.py')).read(), os.path.join(HERE, 'scenery_anim.py'), 'exec'))
