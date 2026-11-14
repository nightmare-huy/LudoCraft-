"""Illustrative GUI mockups based on 1.3.0 layout. NOT a Minecraft renderer/screenshot.
Requires Pillow and DejaVu Sans (override LUDKA_PREVIEW_FONT if needed).
"""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/previews/v1.3.0'
S, OX, OY = 3, 24, 80
BG, NEON, GOLD = '#16161d', '#a578ff', '#ffd76a'
GREEN, RED = '#27bb7c', '#b63853'
COLORS = ['#b8bfcc', '#67d99d', '#65b8ff', '#c28aff', GOLD]
FONT = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')

def make():
    im = Image.new('RGB', (948, 874), '#0e0d14')
    d = ImageDraw.Draw(im)
    d.text((24, 17), 'МАКЕТ ПО КОДУ  ·  НЕ СКРИНШОТ ИЗ MINECRAFT', font=ImageFont.truetype(FONT, 23), fill=GOLD)
    d.text((24, 48), 'LudoCraft 1.3.0 — размеры и цвета по коду GUI; иконки условные.', font=ImageFont.truetype(FONT, 16), fill='#a7a2b9')
    return im, d

def rect(d, x,y,w,h,color):
    d.rectangle((OX+x*S,OY+y*S,OX+(x+w)*S-1,OY+(y+h)*S-1),fill=color)
def frame(d,x,y,w,h,color,fill=BG):
    rect(d,x,y,w,h,color);rect(d,x+1,y+1,w-2,h-2,fill)
def text(d,x,y,t,color='#ada8bf',center=False,size=8):
    f=ImageFont.truetype(FONT,size*S)
    width=d.textlength(t,font=f)
    d.text((OX+x*S-(width/2 if center else 0),OY+y*S-3),t,font=f,fill=color)
def btn(d,x,y,w,h,label,selected=False):
    frame(d,x,y,w,h,GOLD if selected else NEON,'#211c2c');text(d,x+w/2,y+4,label,'#f1ebff',True,size=7)
def icon(im,d,x,y,kind):
    p=Image.new('RGBA',(16,16));q=ImageDraw.Draw(p)
    if kind=='pocket':
        p=Image.open(ROOT/'src/main/resources/assets/ludocraft/textures/item/pocket_ludka.png').resize((16,16),Image.Resampling.NEAREST)
    elif kind=='diamond':
        q.polygon([(4,2),(11,2),(14,6),(8,14),(1,6)],fill='#3ebdc6',outline='#1a667e')
        q.polygon([(4,3),(10,3),(7,12),(2,6)],fill='#a3fff0')
    elif kind=='dirt':
        q.rectangle((1,2,14,14),fill='#79543a');q.rectangle((1,2,14,4),fill='#a37d56')
        for a,b in [(3,7),(9,8),(5,12),(12,5)]:q.rectangle((a,b,a+1,b+1),fill='#4b3828')
    elif kind=='elytra':
        q.polygon([(6,2),(2,5),(1,14),(7,10)],fill='#c1b5d7',outline='#645b7e')
        q.polygon([(9,2),(13,5),(14,14),(8,10)],fill='#a49abc',outline='#645b7e')
    elif kind=='star':
        q.polygon([(8,0),(10,6),(15,8),(10,10),(8,15),(6,10),(0,8),(6,6)],fill='#fff0a6',outline='#cb8d39')
    elif kind=='egg':
        q.ellipse((4,1,12,14),fill='#292033',outline='#9e55af');q.rectangle((6,5,7,6),fill='#b875d7')
    elif kind=='mace':
        q.line((3,14,10,5),fill='#91694a',width=3);q.rectangle((6,1,14,7),fill='#9498b1',outline='#d0d2e4')
    else:
        color={'iron':'#bdc9cb','gold':'#f4c54d','emerald':'#3fd582','netherite':'#686371'}.get(kind,'#aaa0cf')
        q.polygon([(3,4),(12,3),(15,9),(11,12),(1,11),(1,7)],fill=color,outline='#302738')
        q.line((3,5,12,4),fill='#e4ddec')
    im.paste(p.resize((48,48),Image.Resampling.NEAREST),(OX+x*S,OY+y*S),p.resize((48,48),Image.Resampling.NEAREST))

def main():
    im,d=make();frame(d,0,0,300,240,NEON);rect(d,1,1,298,24,'#211c30')
    text(d,150,8,'КАРМАННАЯ ЛУДКА',GOLD,True)
    text(d,16,27,'ВХОД');text(d,250,27,'ВЫХОД');text(d,150,27,'Эпический',COLORS[3],True)
    frame(d,22,36,20,20,NEON,'#0e0e14');frame(d,258,36,20,20,GOLD,'#0e0e14')
    icon(im,d,24,38,'diamond');icon(im,d,142,38,'netherite')
    text(d,150,55,'Незеритовый слиток',GOLD,True)
    btn(d,86,66,128,17,'Выбрать награду')
    text(d,10,84,'Ставка: 128');text(d,233,84,'Цель: 2048')
    frame(d,9,95,282,16,'#61536f',RED);rect(d,10,96,280*0.03125,14,GREEN)
    rect(d,9,93,3,20,'#ffffff')
    text(d,10,115,'0');text(d,150,115,'Шанс: 3.1250%',GOLD,True);text(d,272,115,'100')
    btn(d,104,128,92,20,'ДЕПНУТЬ',True);text(d,68,151,'Инвентарь','#777286')
    for row in range(3):
        for col in range(9):frame(d,68+col*18,161+row*18,18,18,'#353142','#101016')
    for col in range(9):frame(d,68+col*18,217,18,18,'#353142','#101016')
    icon(im,d,69,218,'pocket');icon(im,d,87,218,'dirt');icon(im,d,105,218,'iron')
    d.text((24,824),'Пример: один алмаз (128) → один незеритовый слиток (2048).',font=ImageFont.truetype(FONT,18),fill='#b8bfcc')
    d.text((24,849),'Основное меню. Результат появится справа только при победе.',font=ImageFont.truetype(FONT,16),fill='#888197')
    im.save(OUT/'main-menu-mockup.png')

def catalogue():
    im,d=make();frame(d,0,0,300,240,NEON);text(d,140,10,'КАТАЛОГ НАГРАД',GOLD,True)
    btn(d,272,5,18,18,'×');frame(d,12,31,276,18,'#666173','#0a0910');text(d,16,35,'Поиск: название или mod:id','#777286',size=7)
    for i,t in enumerate(['Все','Обыч.','Необыч.','Редкий','Эпик','Легенд.']):btn(d,10+i*47,55,45,18,t,i==0)
    items=[('egg',24576),('netherite',18432),('mace',16384),('elytra',12288),('star',8192),('gold',4096),('netherite',3072),('netherite',2944),('netherite',2688),('netherite',2560),('netherite',2432),('netherite',2304),('netherite',2048),('diamond',1152),('emerald',864),('diamond',512),('diamond',384),('gold',288),('iron',144),('diamond',128),('emerald',96),('iron',64),('gold',32),('iron',16),('iron',8),('dirt',4),('dirt',2),('dirt',1)]
    for i,(kind,value) in enumerate(items):
        tier=4 if value>=4096 else 3 if value>=512 else 2 if value>=64 else 1 if value>=8 else 0
        x,y=10+(i%8)*35,80+(i//8)*30
        frame(d,x,y,33,28,GOLD if value==2048 else COLORS[tier],'#211c2c')
        icon(im,d,x+8,y+2,kind);text(d,x+16,y+19,str(value),COLORS[tier],True,size=7)
    btn(d,10,213,38,18,'<');btn(d,252,213,38,18,'>');text(d,150,217,'Страница 1 / …','#ada8bf',True)
    d.text((24,824),'Каталог: поиск, пять категорий редкости, страницы и цена под иконкой.',font=ImageFont.truetype(FONT,18),fill='#b8bfcc')
    d.text((24,849),'Иллюстративный набор. В игре список и настоящие иконки зависят от установленных модов.',font=ImageFont.truetype(FONT,16),fill='#888197')
    im.save(OUT/'catalogue-mockup.png')

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True)
    main(); catalogue()
    print(OUT)
