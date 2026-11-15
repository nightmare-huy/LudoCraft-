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

    # 32px pocket slot machine: brass housing, three red sevens, lever and coin tray.
    # GUI remains texture-free; this is only the item icon held by the player.
    image = Image.new('RGBA', (32, 32))
    from PIL import ImageDraw
    draw = ImageDraw.Draw(image)
    draw.rectangle((3, 4, 25, 28), fill='#2a1626', outline='#100e19')
    draw.rectangle((5, 2, 23, 4), fill='#ffd66a', outline='#7a4023')
    draw.rectangle((4, 5, 24, 26), fill='#9c5429')
    draw.rectangle((5, 5, 23, 25), fill='#eeb64b')
    draw.line((5, 5, 23, 5), fill='#fff0a6')
    draw.rectangle((6, 7, 22, 19), fill='#231b30', outline='#7d4028')
    for x in (7, 12, 17):
        draw.rectangle((x, 9, x + 3, 17), fill='#eadab7')
        draw.line((x, 9, x + 3, 9), fill='#fff8dd')
        seven = ['111', '001', '010', '010', '010']
        for dy, row in enumerate(seven):
            for dx, pixel in enumerate(row):
                if pixel == '1': draw.point((x + dx, 11 + dy), fill='#bd2549')
    draw.line((7, 8, 20, 8), fill='#66ecdc')
    draw.rectangle((7, 21, 13, 23), fill='#342034', outline='#7d4028')
    draw.rectangle((16, 21, 21, 23), fill='#146c65')
    draw.line((17, 21, 20, 21), fill='#68f5c5')
    draw.rectangle((7, 26, 21, 29), fill='#100e19')
    draw.rectangle((8, 26, 20, 27), fill='#795143')
    draw.line((9, 28, 19, 28), fill='#d99b38')
    draw.rectangle((24, 14, 28, 17), fill='#593c3d', outline='#100e19')
    draw.rectangle((27, 7, 28, 15), fill='#b9a1b9')
    draw.line((27, 8, 27, 14), fill='#fff2cd')
    draw.rectangle((26, 4, 29, 7), fill='#84223f', outline='#251329')
    draw.rectangle((27, 4, 28, 5), fill='#ff6683')
    draw.point((5, 6), fill='#ffffff')
    draw.point((23, 6), fill='#fff0a6')
    draw.point((5, 24), fill='#fff0a6')
    draw.point((23, 24), fill='#fff0a6')
    image.save(ROOT / 'item/pocket_ludka.png')
