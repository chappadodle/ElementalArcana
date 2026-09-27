"""Generates the spell icons (16x16) and the Mana Sickness effect icon (18x18) as pixel art.

Run from the project root:  python3 tools/gen_textures.py
Each sprite is a square character grid; '.' is transparent, every other character
maps to a colour in that sprite's palette. Edit a grid and re-run to tweak the art.
"""
from pathlib import Path

from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"

SPRITES = {
    "spell/fireball": ({
        "k": 0x5A1A00, "r": 0xD83A1A, "o": 0xFF8A1F, "y": 0xFFD84A, "w": 0xFFF6C8,
    }, [
        "................",
        "..r.............",
        "..ro...r........",
        "...ror..o.......",
        "...roor.or......",
        "....roooor......",
        ".....rkkkkk.....",
        "....rkoooook....",
        "....koyyyyook...",
        "...koyywwyyok...",
        "...koywwwwyok...",
        "...koywwwwyok...",
        "...koyywwyyok...",
        "....koyyyyok....",
        ".....kooook.....",
        "......kkkk......",
    ]),
    "spell/frost_shard": ({
        "k": 0x1E4F7A, "d": 0x2F86C8, "b": 0x5FB8F0, "l": 0xA8E6FF, "w": 0xFFFFFF,
    }, [
        "................",
        "..w.........kk..",
        "...........kwlk.",
        "..........kwlbk.",
        ".........kwlbk..",
        "........kwlbdk..",
        ".......kwlbdk...",
        "......kwlbdk....",
        ".....kwlbdk.....",
        "....kwlbdk......",
        "...klbbdk.......",
        "..klbddk........",
        "..kbddk......w..",
        "..kkdk..........",
        "...kk...........",
        "................",
    ]),
    "spell/tidal_wave": ({
        "k": 0x0C2E6B, "d": 0x1E5FC8, "b": 0x3B8CF6, "l": 0x8EC8FF, "w": 0xEAF6FF,
    }, [
        "................",
        "......kkkk......",
        "....kkbbllkk....",
        "...kbbllwwllk...",
        "..kbbl.kkkwlk...",
        "..kbl.k...kwk...",
        ".kbbk.....k.....",
        ".kbbk...........",
        ".kbbbk......k...",
        ".kdbbbkk...klk..",
        "kddbbbbbkkkblk..",
        "kdddbbbbbbbbbk.w",
        "kddddbbbbbbbdk..",
        ".kdddddddddddkw.",
        "..kkkkkkkkkkkk..",
        "................",
    ]),
    "spell/gale_dash": ({
        "k": 0x3C6B5A, "g": 0x9FE0C8, "p": 0xD8F5EA, "w": 0xFFFFFF,
    }, [
        "................",
        "................",
        "..wwwwwwwp......",
        ".........gp.....",
        "..........g.....",
        "....ppppp.g.....",
        "..wwwwwwwwwp....",
        "............g...",
        "......wwwwwwwp..",
        ".............g..",
        "..pppppppppppg..",
        "................",
        "....wwwwwwp.....",
        "..........g.....",
        ".......ggg......",
        "................",
    ]),
    "spell/flame_burst": ({
        "r": 0xD83A1A, "o": 0xFF8A1F, "y": 0xFFD84A,
    }, [
        "................",
        ".......y........",
        "...y..yoy..y....",
        "....yoorooy.....",
        "...yor....roy...",
        "..yor......roy..",
        ".yor........roy.",
        "..or........ro..",
        "..or........ro..",
        ".yor........roy.",
        "..yor......roy..",
        "...yor....roy...",
        "....yoorrooy....",
        "...y..yoy..y....",
        ".......y........",
        "................",
    ]),
    "spell/healing_rain": ({
        "c": 0xE8F0FF, "g": 0xA8B8D8, "b": 0x3B8CF6, "l": 0x8EC8FF, "h": 0xFF5A7A,
    }, [
        "................",
        ".....gggg.......",
        "...ggccccgg.....",
        "..gccccccccg....",
        ".gccccccccccgg..",
        ".gccccccccccccg.",
        "..gggggggggggg..",
        "................",
        "..l...l...l.....",
        "..b...b...b.h.h.",
        "..........bhhhhh",
        "...l...l...hhhhh",
        "...b...b....hhh.",
        "........l....h..",
        "........b.......",
        "................",
    ]),
    "spell/frost_nova": ({
        "l": 0xA8E6FF, "w": 0xFFFFFF,
    }, [
        "................",
        ".......l........",
        ".....w.l.w......",
        "......lll.......",
        "..l....l....l...",
        "...l...l...l....",
        "....l.lwl.l.....",
        ".lllllwwwlllll..",
        "....l.lwl.l.....",
        "...l...l...l....",
        "..l....l....l...",
        "......lll.......",
        ".....w.l.w......",
        ".......l........",
        "................",
        "................",
    ]),
    "spell/updraft": ({
        "g": 0x9FE0C8, "p": 0xD8F5EA, "w": 0xFFFFFF,
    }, [
        "................",
        ".......w........",
        "......www.......",
        ".....wpwpw......",
        "....w..w..w.....",
        ".......w........",
        "...p...p...p....",
        "..ppp..p..ppp...",
        ".p.p.p.p.p.p.p..",
        "...p...g...p....",
        "...p...g...p....",
        "...g...g...g....",
        "...g.......g....",
        "..ggg.....ggg...",
        ".g...ggggg...g..",
        "................",
    ]),
    "mob_effect/mana_sickness": ({
        "k": 0x2A1540, "d": 0x5A2A80, "p": 0x8A4FC0, "l": 0xC9A0F0,
    }, [
        "..................",
        "........kk........",
        ".......kddk.......",
        ".......kdpk.......",
        "......kddppk......",
        "......kdpplk......",
        ".....kddppllk.....",
        ".....kdpp.llk.....",
        "....kddp....lk....",
        "....kdp......k....",
        "....kd.......k....",
        "....kd..k.k..k....",
        "....kd.......k....",
        ".....kd.kkk.k.....",
        "......kd...k......",
        ".......kkkkk......",
        "..................",
        "..................",
    ]),
}

def render(palette, grid):
    size = len(grid)
    assert all(len(row) == size for row in grid), "sprites must be square"
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, char in enumerate(row):
            if char != ".":
                rgb = palette[char]
                image.putpixel((x, y), (rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255))
    return image


def main():
    for name, (palette, grid) in SPRITES.items():
        path = ASSETS / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        render(palette, grid).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


if __name__ == "__main__":
    main()
