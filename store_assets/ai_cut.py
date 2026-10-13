"""Cut a grid sheet of AI-generated icons (solid mint bg) into transparent 256px dev_* sprites.
usage: ai_cut.py sheet.png name1 name2 ... (row-major order)"""
import sys
from PIL import Image, ImageFilter
import numpy as np
from scipy import ndimage as ndi
im=Image.open(sys.argv[1]).convert('RGB');names=sys.argv[2:]
a=np.array(im).astype(int);bg=a[5,5]
m=np.abs(a-bg).sum(2)>40
m=ndi.binary_closing(m,iterations=5);m=ndi.binary_fill_holes(m)
lab,n=ndi.label(m);objs=ndi.find_objects(lab)
its=[(i+1,o) for i,o in enumerate(objs) if (o[0].stop-o[0].start)*(o[1].stop-o[1].start)>6000]
rows=3 if len(names)>8 else 2;cols=4
H,W=im.height,im.width
for idx,o in its:
    cx=(o[1].start+o[1].stop)/2;cy=(o[0].start+o[0].stop)/2
    k=int(cy//(H/rows))*cols+int(cx//(W/cols))
    if k>=len(names) or names[k]=='-': continue
    mm=lab==idx
    al=Image.fromarray((mm*255).astype('uint8')).filter(ImageFilter.GaussianBlur(0.8))
    r=im.copy();r.putalpha(al);r=r.crop((o[1].start,o[0].start,o[1].stop,o[0].stop))
    S=256;sc=S*0.92/max(r.size);r=r.resize((int(r.width*sc),int(r.height*sc)),Image.LANCZOS)
    c=Image.new('RGBA',(S,S),(0,0,0,0));c.alpha_composite(r,((S-r.width)//2,(S-r.height)//2))
    c.save(f'app/src/main/res/drawable-nodpi/{names[k]}.webp',quality=92);print(names[k])
