"""Side-by-side crops from actual Punchy game frames; no retouching of the rendered item."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1] / 'docs/previews/punchy-grip-review'
f=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',24)
small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
sheet=Image.new('RGB',(1520,1720),'#151c27');d=ImageDraw.Draw(sheet)
d.text((22,16),'Сверка с обычной алмазной киркой · Punchy 3.2b',font=f,fill='#f3d591')
d.text((22,51),'Референс и новый тест используют настройки Punchy и игровые кадры.',font=small,fill='#c6d2e3')
rows=[('vanilla-reference','Алмазная кирка — референс'),('released','LudoCraft 1.5.3 — прежняя поза'),('match-diamond-size','LudoCraft — та же точка хвата, масштаб 75%'),('match-diamond-size-left','Новый вариант, левая рука')]
for i,(name,label) in enumerate(rows):
 x=i%2*760;y=95+i//2*790
 d.text((x+12,y),label,font=f,fill='white')
 im=Image.open(root/f'punchy-grip-{name}.png').convert('RGB')
 crop=im.crop((580,160,960,540))
 sheet.paste(crop.resize((760,760),Image.Resampling.NEAREST),(x,y+32))
d.text((22,1690),'Пока это только тестовый кадр: опубликованный JAR не изменён.',font=small,fill='#c6d2e3')
sheet.save(root/'grip-review.png')
