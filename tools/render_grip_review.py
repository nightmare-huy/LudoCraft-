"""Unpublished grip review: equal-size crops of actual Punchy screenshots, no retouching."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1] / 'docs/previews/punchy-grip-review'
font = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
f = ImageFont.truetype(font, 24); small = ImageFont.truetype(font, 20)
sheet = Image.new('RGB', (1520, 1720), '#151c27'); draw = ImageDraw.Draw(sheet)
draw.text((22, 16), 'Пробный хват · Punchy 3.2b · НЕ РЕЛИЗ', font=f, fill='#f3d591')
draw.text((22, 51), 'Одинаковые игровые ракурсы, реальные кадры. Полные снимки лежат рядом.', font=small, fill='#c6d2e3')
frames = [('vanilla-reference', 'Алмазная кирка — обычный хват в Punchy', False),
          ('released', 'LudoCraft 1.5.3 — исходный профиль', False),
          ('proposed', 'Проба: больше размер, точка хвата сдвинута на рукоять', False),
          ('proposed-left', 'Проба того же профиля в левой руке', True)]
for i,(name,label,left) in enumerate(frames):
    x=i%2*760; y=95+i//2*790
    draw.text((x+14,y),label,font=f,fill='white')
    image=Image.open(root/f'punchy-grip-{name}.png').convert('RGB')
    crop=image.crop((0 if left else 580,160,380 if left else 960,540))
    sheet.paste(crop.resize((760,760),Image.Resampling.NEAREST),(x,y+32))
draw.text((22,1690),'Пробная настройка: масштаб 1,15; offZ +0,35; offX ±0,08. JAR не менялся.',font=small,fill='#c6d2e3')
sheet.save(root/'grip-review.png')
