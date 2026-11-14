"""Documentation infographic, not a Minecraft screenshot. Requires Pillow."""
from pathlib import Path
import os
from PIL import Image, ImageDraw, ImageFont
ROOT = Path(__file__).resolve().parents[1]
FONT = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
im = Image.new('RGB', (1120, 1050), '#111019')
d = ImageDraw.Draw(im)
def text(x, y, s, size=20, color='#eeedf6'):
    d.text((x, y), s, font=ImageFont.truetype(FONT, size), fill=color)
def panel(y, title, lines, color):
    d.rounded_rectangle((32,y,1088,y+170),radius=15,fill='#1d1b29')
    text(54,y+15,title,24,color)
    for i,line in enumerate(lines): text(54,y+60+i*31,line,19)
text(32,22,'LUDOCRAFT 1.5.0',35,'#ffcf72')
text(32,72,'FTB EVOLUTION 1.47.0 · ПЕРВЫЙ КОНСЕРВАТИВНЫЙ ПРОФИЛЬ',23)
text(32,112,'ИНФОГРАФИКА · НЕ СКРИНШОТ · ПОЛНАЯ СБОРКА НЕ ЗАПУСКАЛАСЬ',17,'#aaa6c0')
panel(155,'01 / Ресурсы и техноцели',[
    'Железо 8 · Алмаз 32 · Незерит 512 · AE2 controller 2048 · Cell 1k 640',
    'Алмаз → controller: 6.2500% → 0.7812%',
    'Алмаз → cell 1k: 16.6666% → 2.5000%'
],'#ffcf72')
panel(341,'02 / Защита прогрессии',[
    'Не выдаём скрытые дубликаты, руды, сырьё и compressedblocks.',
    'Кастомные ftbevolution-компоненты / chaos / антиматерия защищены.',
    'Это не стадии квестов. Ограничения и исключения перечислены в README.'
],'#73c8ed')
panel(527,'03 / Без переоценки неизвестных вещей',[
    'Неоценённый модовый предмет: ставка ≤32 / награда ≥512.',
    'Известные профили и override-теги не подпадают под этот fallback.',
    'Большая прочность или EPIC-цвет не делают неизвестную ставку дорогой.'
],'#ab8afa')
panel(713,'04 / Включается отдельно для выбранного мира',[
    'JAR 1.5.0 → mods клиента и сервера. ZIP профиля → <мир>/datapacks/.',
    '/reload → переоткрыть Лудку → заголовок «EVOLUTION 1.47.0».',
    'Без ZIP — универсальный режим. Теги цен имеют высший приоритет.'
],'#7fd9aa')
text(34,910,'Основа: официальный manifest 125/100598 и просмотренные KubeJS-скрипты.',19,'#aaa6c0')
text(34,944,'Не полный аудит экономики. Сохраняется потолок цены 32767.',19,'#aaa6c0')
text(34,978,'Инструкция и ограничения: docs/balance/ftb-evolution-1.47.0.md',19,'#ffcf72')
out=ROOT/'docs/previews/v1.5.0/evolution-balance.png'
out.parent.mkdir(parents=True,exist_ok=True)
im.save(out)
print(out.relative_to(ROOT))
