"""Unpublished grip review: equal-size crops of actual Punchy screenshots, no retouching."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1] / 'docs/previews/punchy-grip-review'
font = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
f = ImageFont.truetype(font, 24); small = ImageFont.truetype(font, 20)
sheet = Image.new('RGB', (1520, 1720), '#151c27'); draw = ImageDraw.Draw(sheet)
draw.text((22, 16), 'Пробный хват · Punchy 3.2b · НЕ РЕЛИЗ', font=f, fill='#f3d591')
draw.text((22, 51), 'Фрагменты настоящих игровых снимков ×2. Полные кадры сохранены рядом.', font=small, fill='#c6d2e3')
frames = [('vanilla-reference', 'Обычная алмазная кирка — ориентир', False),
          ('released', '1.5.3 — хват слишком близко к головке', False),
          ('proposed', 'Пробный вариант — хват за рукоять', False),
          ('proposed-left', 'Тот же вариант — левая рука', True)]
for i,(name,label,left) in enumerate(frames):
    x=i%2*760; y=95+i//2*790
    draw.text((x+14,y),label,font=f,fill='white')
    image=Image.open(root/f'punchy-grip-{name}.png').convert('RGB')
    crop=image.crop((0 if left else 580,160,380 if left else 960,540))
    sheet.paste(crop.resize((760,760),Image.Resampling.NEAREST),(x,y+32))
draw.text((22,1690),'В опубликованный JAR это положение пока не внесено. Модель и текстура не изменялись.',font=small,fill='#c6d2e3')
sheet.save(root/'grip-review.png')
