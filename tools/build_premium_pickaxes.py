"""Export two original closed-voxel premium pickaxe meshes and 64x32 atlases."""
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_pickaxe_model as base
from PIL import Image, ImageDraw

ASSETS = base.ASSETS
GRID = base.GRID
FACES = base.FACES
DISPLAY = base.DISPLAY

VIP_MATS = ['obsidian', 'grip', 'gold', 'gold_light', 'screen', 'screen_light', 'frame', 'green']
VIP_COLORS = ['#171923', '#282d38', '#d6a53e', '#ffe18b', '#075a35', '#13e993', '#b67920', '#9dffbd']
VIP_BOXES = [
 ('obsidian',(7.25,1,7.25),(8.75,13,8.75)), ('grip',(7,2,7),(9,9,9)),
 ('gold',(6.7,.5,6.7),(9.3,1.8,9.3)), ('gold_light',(6.9,.5,6.9),(9.1,.8,9.1)),
 ('gold',(6.5,3,6.5),(9.5,3.5,9.5)), ('gold',(6.5,8,6.5),(9.5,8.5,9.5)),
 ('gold',(6,12,6),(10,14,10)), ('gold_light',(6.25,13.75,6.25),(9.75,14.25,9.75)),
 # Wide dark casino-machine housing and gilded side shoulders.
 ('obsidian',(2.25,10.5,6.5),(13.75,14.25,9.5)),
 ('frame',(1.5,11,6.25),(3.5,13.75,9.75)), ('frame',(12.5,11,6.25),(14.5,13.75,9.75)),
 ('gold',(3,10.75,6.25),(13,11.5,9.75)), ('gold_light',(4,14,6.5),(12,14.5,9.5)),
 # Bright green 777 display, front/back so the sign reads from either side.
 ('screen',(4,11.5,5.9),(12,13.5,6.2)), ('screen',(4,11.5,9.8),(12,13.5,10.1)),
 ('green',(4.25,11.75,5.85),(11.75,12,6.2)), ('green',(4.25,11.75,9.8),(11.75,12,10.15)),
]

HR_MATS = ['amethyst', 'white', 'gold', 'gold_light', 'roller', 'roller_light', 'crown', 'deep_purple']
HR_COLORS = ['#7145a7', '#f4f1e9', '#d6a239', '#fff0aa', '#6428a1', '#df9cff', '#ffe08a', '#382354']
HR_BOXES = [
 ('amethyst',(7.25,.75,7.25),(8.75,13,8.75)), ('deep_purple',(7,2,7),(9,8.75,9)),
 ('gold',(6.7,.5,6.7),(9.3,1.8,9.3)), ('gold_light',(6.9,.5,6.9),(9.1,.8,9.1)),
 ('white',(7,9.5,7),(9,12,9)), ('gold',(6.5,8.5,6.5),(9.5,9,9.5)),
 # white-and-gold machine body, jewel side caps, three radiant drums
 ('white',(2,10,6.5),(14,14.5,9.5)), ('gold',(2.5,9.75,6.25),(13.5,10.5,9.75)),
 ('gold',(3,14.25,6.5),(13,14.75,9.5)),
 ('amethyst',(1.5,10.5,6.5),(3.5,13.5,9.5)), ('amethyst',(12.5,10.5,6.5),(14.5,13.5,9.5)),
 ('roller',(4,11,5.9),(6.25,13.75,6.2)), ('roller',(6.9,11,5.9),(9.1,13.75,6.2)), ('roller',(9.75,11,5.9),(12,13.75,6.2)),
 ('roller',(4,11,9.8),(6.25,13.75,10.1)), ('roller',(6.9,11,9.8),(9.1,13.75,10.1)), ('roller',(9.75,11,9.8),(12,13.75,10.1)),
 ('roller_light',(4.2,11.25,5.8),(6.05,11.5,6.25)), ('roller_light',(7.1,11.25,5.8),(8.9,11.5,6.25)), ('roller_light',(9.95,11.25,5.8),(11.8,11.5,6.25)),
 ('roller_light',(4.2,11.25,9.75),(6.05,11.5,10.2)), ('roller_light',(7.1,11.25,9.75),(8.9,11.5,10.2)), ('roller_light',(9.95,11.25,9.75),(11.8,11.5,10.2)),
 # crown silhouette floating just above the casing
 ('gold',(5.5,14.5,6.75),(10.5,15.25,9.25)),
 ('crown',(5.25,15,7),(6,16,9)), ('crown',(7.25,15,7),(8.75,16,9)), ('crown',(10,15,7),(10.75,16,9)),
 ('gold_light',(5.5,14.5,6.75),(10.5,14.8,9.25)),
]

def write_variant(name, materials, colors, boxes, digit_style):
    base.MATERIALS, base.COLORS, base.BOXES = materials, colors, boxes
    model = base.make_model()
    model['credit'] = 'Original LudoCraft premium 3D pickaxe mesh; tools/build_premium_pickaxes.py'
    model['textures']['surface'] = f'ludocraft:item/{name}_3d'
    model['textures']['particle'] = f'ludocraft:item/{name}_3d'
    model_path = ASSETS/'models/item'/f'{name}.json'
    texture_path = ASSETS/'textures/item'/f'{name}_3d.png'
    model_path.write_text(__import__('json').dumps(model, indent=2)+'\n')
    image = Image.new('RGBA',(64,32)); draw = ImageDraw.Draw(image)
    for i,(mat,color) in enumerate(zip(materials,colors)):
        x,y=(i%4)*16,(i//4)*16
        draw.rectangle((x,y,x+15,y+15),fill=color)
        draw.line((x,y,x+15,y), fill='#ffffff' if mat in ('gold_light','roller_light','screen_light','crown') else color)
        if mat in ('screen','roller'):
            draw.rectangle((x+1,y+1,x+14,y+14),fill='#083b28' if mat=='screen' else '#4d207e',outline='#a8ffd0' if mat=='screen' else '#f0b7ff')
            if digit_style=='777' and mat=='screen':
                for dx in (2,6,10):
                    for yy in (3,4,5,8,9,10): draw.rectangle((x+dx,y+yy,x+dx+1,y+yy+1),fill='#5bffac')
            elif mat=='roller':
                draw.line((x+4,y+3,x+12,y+3),fill='#fff0ff',width=2)
                draw.line((x+5,y+8,x+11,y+8),fill='#b978e8',width=2)
        elif mat in ('gold','gold_light','crown'):
            draw.line((x,y+15,x+15,y+15),fill='#916020')
            for px in (3,8,13): draw.point((x+px,y+4),fill='#fff2bd')
        elif mat in ('amethyst','deep_purple','obsidian','grip'):
            for yy in (4,9,14): draw.line((x+1,y+yy,x+14,y+yy-2),fill='#a47bc9' if mat!='grip' else '#454b57')
    texture_path.parent.mkdir(parents=True,exist_ok=True); image.save(texture_path)
    print(f'{name}: {len(model["elements"])} exposed faces')

if __name__=='__main__':
    write_variant('vip_gamblers_pickaxe',VIP_MATS,VIP_COLORS,VIP_BOXES,'777')
    write_variant('highroller_pickaxe',HR_MATS,HR_COLORS,HR_BOXES,'rollers')
