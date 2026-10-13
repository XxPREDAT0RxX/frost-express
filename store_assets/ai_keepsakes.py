"""usage: ai_keepsakes.py sheet.png row first_idx   (12 icons, 4x3 grid; rows `row` (6 items) then `row+1`)
Cuts the icons, drops them into circle badges (colours sampled from the existing badge of that row)."""
import sys
from PIL import Image, ImageFilter, ImageDraw
import numpy as np
from scipy import ndimage as ndi
D='app/src/main/res/drawable-nodpi/'
sheet,row0=sys.argv[1],int(sys.argv[2])
im=Image.open(sheet).convert('RGB');a=np.array(im);bg=a[5,5].copy()
for y0,y1 in [(230,262),(464,500),(703,742)]: a[y0:y1,:]=bg
im=Image.fromarray(a);a=a.astype(int)
m=np.abs(a-bg).sum(2)>40
m=ndi.binary_closing(m,iterations=4);m=ndi.binary_fill_holes(m)
lab,n=ndi.label(m);objs=ndi.find_objects(lab)
H,W=im.height,im.width
def badge(r,icon):
    old=Image.open(D+f'col_{r}_0.webp').convert('RGBA')
    fill=old.getpixel((96,22));ring=(107,74,58,255)
    S=192;b=Image.new('RGBA',(S,S),(0,0,0,0));d=ImageDraw.Draw(b)
    d.ellipse((2,2,S-3,S-3),fill=ring);d.ellipse((7,7,S-8,S-8),fill=fill)
    sc=S*0.62/max(icon.size);ic=icon.resize((int(icon.width*sc),int(icon.height*sc)),Image.LANCZOS)
    b.alpha_composite(ic,((S-ic.width)//2,(S-ic.height)//2));return b
for i,o in enumerate(objs):
    if (o[0].stop-o[0].start)*(o[1].stop-o[1].start)<5000: continue
    cx=(o[1].start+o[1].stop)/2;cy=(o[0].start+o[0].stop)/2
    k=int(cy//(H/3))*4+int(cx//(W/4))   # 0..11 -> row0 + k//6, idx k%6
    r=row0+k//6;idx=k%6
    al=Image.fromarray(((lab==i+1)*255).astype('uint8')).filter(ImageFilter.GaussianBlur(0.8))
    ic=im.copy();ic.putalpha(al);ic=ic.crop((o[1].start,o[0].start,o[1].stop,o[0].stop))
    badge(r,ic).save(D+f'col_{r}_{idx}.webp',quality=92);print(r,idx)
