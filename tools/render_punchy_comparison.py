"""Caption actual Punchy 3.2b test frames. No synthetic/retouched gameplay."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1]/'docs/previews/v1.5.3'
font = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
f = ImageFont.truetype(font,23); small = ImageFont.truetype(font,20)
sheet = Image.new('RGB',(1920,1900),'#151c27'); d=ImageDraw.Draw(sheet)
d.text((24,16),'LudoCraft 1.5.3 · Совместимость с Punchy 3.2b · NeoForge 1.21.1',font=f,fill='#f3d591')
d.text((24,52),'Настоящий Minecraft с установленным Punchy, стандартные настройки · не макеты',font=small,fill='#c6d2e3')
frames=[('punchy-before/punchy-after-first-right.png','До — LudoCraft 1.5.2 + Punchy 3.2b'),
        ('punchy/punchy-after-first-right.png','После — отдельный профиль только для этой кирки'),
        ('punchy/punchy-after-first-left.png','Левая рука с Punchy'),
        ('punchy/punchy-looking-down.png','Взгляд вниз перед добычей'),
        ('punchy/punchy-mining-middle.png','Добыча камня в выживании — нажатие атаки'),
        ('punchy/punchy-mining-recovered.png','После добычи — возвращение в обычное положение')]
for i,(file,title) in enumerate(frames):
 x=i%2*960;y=95+i//2*590
 d.text((x+16,y),title,font=f,fill='white');image=Image.open(root/file).convert('RGB');assert image.size==(960,540)
 sheet.paste(image,(x,y+38))
d.text((24,1870),'Исходные кадры не ретушированы. До: творческий тест; после: выживание с каменной мишенью.',font=small,fill='#c6d2e3')
sheet.save(root/'punchy-comparison.png')
