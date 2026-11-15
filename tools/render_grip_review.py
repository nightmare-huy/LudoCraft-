"""Compare the published 1.5.3 frame against genuine vanilla and dev-candidate Punchy frames."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
root = Path(__file__).resolve().parents[1] / 'docs/previews/punchy-grip-review'
previous = Path(__file__).resolve().parents[1] / 'docs/previews/v1.5.4/punchy/punchy-after-first-right.png'
f=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',24)
small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
sheet=Image.new('RGB',(1520,1720),'#151c27');d=ImageDraw.Draw(sheet)
d.text((22,16),'Сверка хвата с алмазной киркой · Punchy 3.2b',font=f,fill='#f3d591')
d.text((22,51),'Настройки стандартные; реальные кадры из игрового клиента, не макеты.',font=small,fill='#c6d2e3')
frames=[('punchy-grip-vanilla-reference.png','Алмазная кирка — эталон',False),
        (previous,'LudoCraft 1.5.4 — до дополнительного сдвига',False),
        ('punchy-grip-match-diamond-size.png','Проба: ещё 0,5 px внутрь ладони',False),
        ('punchy-grip-match-diamond-size-left.png','Проверка той же позы левой руки',True)]
for i,(filename,label,left) in enumerate(frames):
 x=i%2*760;y=95+i//2*790
 d.text((x+12,y),label,font=f,fill='white')
 im=Image.open(filename if isinstance(filename,Path) else root/filename).convert('RGB')
 crop=im.crop((0 if left else 580,160,380 if left else 960,540))
 sheet.paste(crop.resize((760,760),Image.Resampling.NEAREST),(x,y+32))
d.text((22,1690),'Тестовый кандидат: размер/угол/сдвиг изменены только для первого лица; JAR 1.5.3 не тронут.',font=small,fill='#c6d2e3')
sheet.save(root/'grip-review.png')
