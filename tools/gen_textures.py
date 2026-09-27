"""Generates the spell icons (16x16) and the Mana Sickness effect icon (18x18) as pixel art.

Run from the project root:  python3 tools/gen_textures.py
Each sprite is a square character grid; '.' is transparent, every other character
maps to a colour in that sprite's palette (0xRRGGBB, or 0xAARRGGBB for translucent
pixels). Edit a grid and re-run to tweak the art.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"

SPARKLE = {"W": 0xFFFFFFFF, "c": 0xFFCFF4FF, "b": 0xC0A8E6FF, "f": 0x7090D8FF}
SHARD = {"w": 0xFFFFFFFF, "l": 0xFFC8F0FF, "b": 0xFF7CC4F0, "d": 0xFF3C80C0}

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
    "spell/icicle": ({
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
    # Texture for the icicle's 3D model (models/spell/icicle.json), so it lives in the block atlas.
    "spell/frost_shield": ({
        "k": 0x1E4F7A, "d": 0x2F86C8, "b": 0x5FB8F0, "l": 0xA8E6FF, "w": 0xFFFFFF,
    }, [
        "................",
        "......kkkk......",
        "....kkllllkk....",
        "...kllbbbbllk...",
        "..klbbblbbbblk..",
        "..klbblwlbbblk..",
        ".klbbbbwbbbbblk.",
        ".klblwwwwwlbblk.",
        ".klbbbbwbbbbblk.",
        ".klbblwlbbbbdlk.",
        "..klbblbbbbdlk..",
        "..kldbbbbbddlk..",
        "...kldddddddk...",
        "....kkddddkk....",
        "......kkkk......",
        "................",
    ]),
    "block/icicle": ({
        "w": 0xF2FFFFFF, "l": 0xE6D2F6FF, "b": 0xDCA8E4FF, "d": 0xD27CC8F2, "k": 0xD8508CD2,
    }, [
        "wlbbdlwlbbdklwlb",
        "wlbdklwlbddklwlb",
        "lbbdklwlbdkblwlb",
        "lbddklwlbdkblllb",
        "lbdkbllwbdkblllb",
        "wbdkbllwbdkbwllb",
        "wbdkblwlbdkbwlbd",
        "wbdkblwlbddbwlbd",
        "lbddblwlbddbwlbd",
        "lbbdblwllbdbwlbd",
        "llbdblwllbdklwbd",
        "wlbdklwllbdklwbd",
        "wlbdklwlbbdklwbd",
        "wlbddkwlbbdklwbb",
        "llbbdkwlbddklwbb",
        "llbbdkwlbddklwlb",
    ]),
    # ---- particles (textures/particle/) ----
    # Frost sparkle: a 4-point glint, shrinking frame by frame (played over the particle's life).
    "particle/frost_sparkle_0": (SPARKLE, [
        "...f....",
        "...c....",
        "..bWb...",
        "fcWWWcf.",
        "..bWb...",
        "...c....",
        "...f....",
        "........",
    ]),
    "particle/frost_sparkle_1": (SPARKLE, [
        "........",
        "...f....",
        "...c....",
        ".fcWcf..",
        "...c....",
        "...f....",
        "........",
        "........",
    ]),
    "particle/frost_sparkle_2": (SPARKLE, [
        "........",
        "........",
        "...c....",
        "..cWc...",
        "...c....",
        "........",
        "........",
        "........",
    ]),
    "particle/frost_sparkle_3": (SPARKLE, [
        "........",
        "........",
        "........",
        "...W....",
        "........",
        "........",
        "........",
        "........",
    ]),
    # Ice shard: three debris variants, one picked at random per particle.
    "particle/ice_shard_0": (SHARD, [
        "........",
        "....w...",
        "...wl...",
        "..wlb...",
        "..lbd...",
        "...d....",
        "........",
        "........",
    ]),
    "particle/ice_shard_1": (SHARD, [
        "........",
        "..w.....",
        "..lw....",
        "...lb...",
        "...bbd..",
        "....d...",
        "........",
        "........",
    ]),
    "particle/ice_shard_2": (SHARD, [
        "........",
        "........",
        "..wwl...",
        "..lbbd..",
        "...dd...",
        "........",
        "........",
        "........",
    ]),
    "mob_effect/frozen": ({
        "k": 0x1E4F7A, "d": 0x3C80C0, "b": 0x7CC4F0, "l": 0xC8F0FF, "w": 0xFFFFFF,
    }, [
        "..................",
        "..................",
        "....kkkkkkkkkk....",
        "...kwwllllllbbk...",
        "...kwlllllllbdk...",
        "...klllwwllbbdk...",
        "...kllwlllbbbdk...",
        "...kllllllbbbdk...",
        "...kllllbbbbbdk...",
        "...klllbbbbbddk...",
        "...kllbbbbbbddk...",
        "...klbbbbbbdddk...",
        "...kbbbbbbddddk...",
        "...kbbbbbdddddk...",
        "...kddddddddddk...",
        "....kkkkkkkkkk....",
        "..................",
        "..................",
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
                color = palette[char]
                alpha = color >> 24 & 0xFF if color > 0xFFFFFF else 255
                image.putpixel((x, y), (color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, alpha))
    return image


def frost_mist(frame, frames=4, size=16):
    """A soft, slightly lumpy cold puff; later frames are wider and fainter (smooth gradients
    can't be drawn as a character grid, so this one is procedural)."""
    rng = np.random.default_rng(100 + frame)
    y, x = np.mgrid[0:size, 0:size] + 0.5
    radius = np.hypot(x - size / 2, y - size / 2)
    sigma = size * (0.18 + 0.05 * frame)
    lumps = 1 + 0.25 * rng.standard_normal((size, size))
    alpha = np.exp(-(radius / sigma) ** 2) * lumps * (0.75 - 0.15 * frame)
    rgba = np.zeros((size, size, 4), dtype=np.uint8)
    rgba[..., 0], rgba[..., 1], rgba[..., 2] = 232, 246, 255
    rgba[..., 3] = np.clip(alpha * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def main():
    for name, (palette, grid) in SPRITES.items():
        path = ASSETS / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        render(palette, grid).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for frame in range(4):
        path = ASSETS / f"particle/frost_mist_{frame}.png"
        frost_mist(frame).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


if __name__ == "__main__":
    main()
