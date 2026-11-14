"""Generate the original pixel-art textures; requires Pillow, not needed to build the mod."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ludocraft/textures'
PALETTE = {
    'K': '#2B1A0A', 'G': '#F2BE2E', 'H': '#FFF3A8', 'C': '#9DFFFA',
    'D': '#B87C12', 'c': '#26BAC4', 'E': '#78FFA0', 'r': '#DE5260',
    'e': '#14A046', 'm': '#801628', 'B': '#FAD246', 'b': '#BE8214', 'o': '#340810',
}
PIXELS = [
    '.....KKKKKKK....',
    '...KKGHHHHHHKK..',
    '..KCHDDDDDDGHHK.',
    '.KCCcKKKKKKEEHK.',
    '.KcKK......reGHK',
    '..K.......rmKDHK',
    '.........rm.KDHK',
    '........rm..KDHK',
    '.......Bb...KDHK',
    '......rm....KDHK',
    '.....rm.....KDGK',
    '....Bb.....KcHK.',
    '...rm......KCCK.',
    '..rm......KcCK..',
    '.Bb........KK...',
    'o...............',
]

if __name__ == '__main__':
    (ROOT / 'item').mkdir(parents=True, exist_ok=True)
    (ROOT / 'particle').mkdir(parents=True, exist_ok=True)
    image = Image.new('RGBA', (16, 16))
    for y, row in enumerate(PIXELS):
        assert len(row) == 16
        for x, symbol in enumerate(row):
            if symbol != '.':
                rgb = bytes.fromhex(PALETTE[symbol][1:])
                image.putpixel((x, y), (*rgb, 255))
    image.save(ROOT / 'item/gamblers_pickaxe.png')

    image = Image.new('RGBA', (8, 8))
    for x in range(2, 6):
        for y in range(1, 7):
            image.putpixel((x, y), (255, 255, 255, 255))
    image.save(ROOT / 'particle/confetti.png')

    # Pocket casino device; the actual GUI deliberately has no texture assets.
    image = Image.new('RGBA', (16, 16))
    from PIL import ImageDraw
    draw = ImageDraw.Draw(image)
    draw.rectangle((2, 1, 13, 14), fill='#16161d', outline='#a578ff')
    draw.rectangle((4, 3, 11, 9), fill='#2c233a', outline='#ffd76a')
    draw.line([(6, 5), (9, 5), (7, 8)], fill='#ffd76a', width=1)
    draw.rectangle((4, 11, 7, 12), fill='#27bb7c')
    draw.rectangle((9, 11, 11, 12), fill='#b63853')
    image.save(ROOT / 'item/pocket_ludka.png')
