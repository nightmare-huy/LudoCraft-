"""Arrange unretouched real-client captures with labels (not a synthetic game render)."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont

root = Path(__file__).resolve().parents[1] / 'docs/previews/v1.5.2'
font_path = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
font = ImageFont.truetype(font_path, 23)
small = ImageFont.truetype(font_path, 20)
shots = [
    ('before-first-right', '1.5.1 — прежнее положение, головка за краем'),
    ('after-first-right', '1.5.2 — исправленное положение в правой руке'),
    ('after-third-front', '1.5.2 — вид спереди (F5)'),
    ('after-third-side', '1.5.2 — вид сбоку (F5)'),
    ('after-first-left', '1.5.2 — левая рука'),
    ('after-first-swing', '1.5.2 — кадр взмаха'),
]
sheet = Image.new('RGB', (1920, 1900), '#151c27')
draw = ImageDraw.Draw(sheet)
draw.text((24, 16), 'LudoCraft 1.5.2 · Исправление хвата 3D-кирки', font=font, fill='#f3d591')
draw.text((24, 52), 'Настоящие снимки Minecraft 1.21.1 / NeoForge · тестовый мир, FOV 70 · без шейдеров', font=small, fill='#c6d2e3')
for i, (shot, title) in enumerate(shots):
    x, y = (i % 2)*960, 95+(i//2)*590
    draw.text((x+16, y), title, font=font, fill='white')
    image = Image.open(root/f'hand-{shot}.png').convert('RGB')
    assert image.size == (960, 540)
    sheet.paste(image, (x,y+38))
draw.text((24, 1870), 'Кадры не ретушированы. До/после: одна геометрия, меняются только положения модели в руках.', font=small, fill='#c6d2e3')
sheet.save(root/'hand-comparison.png')
print(root/'hand-comparison.png')
