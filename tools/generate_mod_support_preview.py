"""1.4.0 documentation infographic; NOT a game screenshot or GUI mockup.
Requires Pillow; values mirror LudkaModValues and its regression tests.
"""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/previews/v1.4.0/mod-support-overview.png'
FONT = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
BG, PANEL, GOLD, WHITE, MUTED = '#111019', '#1d1b29', '#ffcf72', '#eeedf6', '#aaa6c0'
im = Image.new('RGB', (1120, 1110), BG)
d = ImageDraw.Draw(im)

def text(x, y, value, size=20, color=WHITE):
    d.text((x, y), value, font=ImageFont.truetype(FONT, size), fill=color)

def panel(y, title, lines, color='#aa83f3'):
    d.rounded_rectangle((32, y, 1088, y+166), radius=15, fill=PANEL)
    d.rectangle((32, y+17, 36, y+149), fill=color)
    text(54, y+15, title, 23, color)
    for i, line in enumerate(lines):
        text(54, y+57+i*31, line, 19)

text(32, 22, 'LUDOCRAFT 1.4.0', 35, GOLD)
text(32, 72, 'ЛУДКА × ТЕХНОМОДЫ', 24)
text(32, 110, 'ИНФОГРАФИКА ОЦЕНОК · НЕ СКРИНШОТ И НЕ МАКЕТ GUI', 17, MUTED)
panel(155, '01 / AllTheOres + AllTheModium', [
    'Олово: самородок 1 → слиток 16 → блок 144',
    'AllTheModium 4096 → Vibranium 8192 → Unobtainium 16384',
    'Разные оценки металлов, сплавов, форм, инструментов и брони.'
], GOLD)
panel(337, '02 / Mekanism + Generators / Tools · Ender IO', [
    'Схемы Mekanism: Basic 32 → Advanced 128 → Elite 512 → Ultimate 2048',
    'Ender IO: Basic capacitor 64 → Double layer 256 → Octadic 1024',
    'Машины, фабрики, QIO, генераторы и MekaSuit имеют отдельные оценки.'
], '#73c8ed')
panel(519, '03 / AE2 + MEGACells · Applied Mekanistics', [
    'AE2 item cell: 1k = 384  /  16k = 1152  /  256k = 4224',
    'MEGACells item cell: 1m = 8320  /  16m = 18560  /  256m = 30128',
    'Процессоры, корпуса, компоненты, chemical и portable cells — отдельно.'
], '#ab8afa')
panel(701, '04 / ExtendedAE · AdvancedAE · общие теги других модов', [
    'Extended pattern provider 2048 · AdvancedAE quantum processor 1024',
    'Общие c:-теги: слитки, пыль, самородки, пластины, шестерни, блоки…',
    'Датапак приоритетнее профиля. Creative/debug-объекты исключены.'
], '#7fd9aa')
text(34, 892, 'Алмаз → AE2 item cell 1k: 16.6666%  ·  Выигрыш: 1 пустая ячейка', 22, GOLD)
text(34, 936, 'Цены — условный баланс, не расчёт рецептов. Потолок: 32767.', 19, MUTED)
text(34, 967, 'Содержимое, заряд и модули ставки не повышают шанс и не копируются.', 19, MUTED)
text(34, 1008, 'Правила тестируются отдельно; совместный запуск модпака не проверен.', 18, MUTED)
text(34, 1042, 'Minecraft 1.21.1 / NeoForge · Подробности: docs/mod-support.md', 18, MUTED)
OUT.parent.mkdir(parents=True, exist_ok=True)
im.save(OUT)
print(OUT.relative_to(ROOT))
