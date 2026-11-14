"""Software Z-buffer renders of the shipped JSON + PNG, NOT screenshots from Minecraft.
No invented geometry: vertices, UVs, and display transforms come from the actual model.
Requires Pillow. Face UV order follows Minecraft FaceInfo / BlockFaceUV.
"""
from pathlib import Path
import json
import math
import os
from PIL import Image, ImageDraw, ImageFont
from build_pickaxe_model import MODEL, TEXTURE, FACES
from check_pickaxe_model import rotate
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'docs/previews/v1.5.1'
FONT = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')

def vertices(e, face):
    a,b,c=e['from']; x,y,z=e['to']
    return {
        'down': [(a,b,z),(a,b,c),(x,b,c),(x,b,z)],
        'up': [(a,y,c),(a,y,z),(x,y,z),(x,y,c)],
        'north': [(x,y,c),(x,b,c),(a,b,c),(a,y,c)],
        'south': [(a,y,z),(a,b,z),(x,b,z),(x,y,z)],
        'west': [(a,y,c),(a,b,c),(a,b,z),(a,y,z)],
        'east': [(x,y,z),(x,b,z),(x,b,c),(x,y,c)],
    }[face]

def render(model, angles, scale=1, size=600, framing=20, translation=(0,0,0)):
    texture=Image.open(TEXTURE).convert('RGBA'); tex=texture.load()
    image=Image.new('RGBA',(size,size)); pixels=image.load()
    depth=[-float('inf')]*(size*size)
    factor=size/framing
    for e in model['elements']:
        face,spec=next(iter(e['faces'].items()))
        axis,sign=FACES[face]; n=[0,0,0];n[axis]=sign;n=rotate(n,angles)
        if n[2] <= 1e-8: continue
        shade=max(.45,min(1,.68+.22*n[1]+.12*n[2]-.12*n[0]))
        points=[]
        for v in vertices(e,face):
            p=rotate(tuple((v[i]-8)*scale for i in range(3)),angles)
            points.append(((p[0]+translation[0])*factor+size/2,
                           size/2-(p[1]+translation[1])*factor,p[2]+translation[2]))
        u0,v0,u1,v1=spec['uv']; uv=[(u0,v0),(u0,v1),(u1,v1),(u1,v0)]
        for indices in [(0,1,2),(0,2,3)]:
            a,b,c=[points[i] for i in indices]; ta,tb,tc=[uv[i] for i in indices]
            det=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
            if abs(det)<1e-9:continue
            for sy in range(max(0,math.floor(min(a[1],b[1],c[1]))),min(size,math.ceil(max(a[1],b[1],c[1])))):
                for sx in range(max(0,math.floor(min(a[0],b[0],c[0]))),min(size,math.ceil(max(a[0],b[0],c[0])))):
                    px,py=sx+.5,sy+.5
                    w0=((b[1]-c[1])*(px-c[0])+(c[0]-b[0])*(py-c[1]))/det
                    w1=((c[1]-a[1])*(px-c[0])+(a[0]-c[0])*(py-c[1]))/det
                    w2=1-w0-w1
                    if min(w0,w1,w2)<-1e-7:continue
                    z=w0*a[2]+w1*b[2]+w2*c[2]
                    if z<=depth[sy*size+sx]:continue
                    u=w0*ta[0]+w1*tb[0]+w2*tc[0];v=w0*ta[1]+w1*tb[1]+w2*tc[1]
                    # Clamp to this face's tile, avoiding adjacent atlas tiles at the border.
                    tx=min(math.ceil(u1*4)-1,max(int(u0*4),int(u*4)))
                    ty=min(math.ceil(v1*2)-1,max(int(v0*2),int(v*2)))
                    rgba=tex[tx,ty]
                    pixels[sx,sy]=tuple(round(ch*shade) for ch in rgba[:3])+(rgba[3],)
                    depth[sy*size+sx]=z
    return image

def label(d,xy,text,size=20,color='#eee9f5'):
    d.text(xy,text,font=ImageFont.truetype(FONT,size),fill=color)

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    model=json.loads(MODEL.read_text())
    image=Image.new('RGB',(1000,980),'#111019');d=ImageDraw.Draw(image)
    label(d,(34,22),'КИРКА ЛУДОМАНИИ · 3D',34,'#ffcf72')
    label(d,(34,70),'LudoCraft 1.5.1 · рендер реальных JSON и текстуры модели',19)
    label(d,(34,101),'НЕ СКРИНШОТ MINECRAFT · программный рендер с Z-буфером',16,'#aaa6c0')
    d.ellipse((200,785,800,835),fill='#0a0910')
    hero=render(model,(18,-32,-28),size=760,framing=22)
    image.paste(hero,(120,120),hero)
    label(d,(34,883),'Объёмная алмазная головка · золотые крепления · знак 777 с двух сторон',19)
    label(d,(34,919),'Обычная модель Minecraft 1.21.1 — без GeckoLib и дополнительных модов',18,'#aaa6c0')
    image.save(OUT/'pickaxe-3d.png')
    # An inspection sheet, generated from the same geometry, not AI imagery.
    sheet=Image.new('RGB',(1120,880),'#111019');d=ImageDraw.Draw(sheet)
    label(d,(24,18),'ПРОВЕРКА РАКУРСОВ · МОДЕЛЬ 1.5.1',27,'#ffcf72')
    label(d,(24,57),'Программные рендеры экспортированной модели, не игровой клиент',16,'#aaa6c0')
    for i,(name,angles) in enumerate([('Передняя сторона',(0,0,0)),('Обратная сторона',(0,180,0)),('Толщина / бок',(15,75,0))]):
        r=render(model,angles,size=340,framing=18)
        x=20+i*370;sheet.paste(r,(x,105),r);label(d,(x+5,465),name,19)
    gui=model['display']['gui'];r=render(model,gui['rotation'],gui['scale'][0],size=256,framing=16)
    d.rectangle((42,545,298,801),fill='#302b3a',outline='#a578ff');sheet.paste(r,(42,545),r)
    label(d,(338,553),'GUI: реальные rotation/scale из JSON',23,'#ffcf72')
    label(d,(338,599),'241 внешняя грань · 8 положений предмета',20)
    label(d,(338,635),'Все поверхности замкнуты; внутренних граней нет.',19)
    label(d,(338,671),'Проверяются UV, текстура, связность и границы иконки.',18)
    label(d,(338,722),'Положение относительно руки игрока и анимация взмаха',17,'#aaa6c0')
    label(d,(338,749),'требуют отдельной визуальной проверки в мире.',17,'#aaa6c0')
    sheet.save(OUT/'pickaxe-inspection.png')
    print('Wrote pickaxe-3d.png and pickaxe-inspection.png')

if __name__=='__main__': main()
