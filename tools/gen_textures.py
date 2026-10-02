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
    "item/arcanist_journal": ({
        "k": 0x2A1630, "c": 0x5A2E6A, "l": 0x8A4FA0, "p": 0xEDE3C8, "g": 0xFFD84A, "w": 0xFFFFFF,
    }, [
        "................",
        "..kkkkkkkkkkkk..",
        "..kclllllllcpk..",
        "..kclllllllcpk..",
        "..kclllglllcpk..",
        "..kcllgwgllcpk..",
        "..kclgwwwglcpk..",
        "..kcllgwgllcpk..",
        "..kclllglllcpk..",
        "..kclllllllcpk..",
        "..kclllllllcpk..",
        "..kclllllllcpk..",
        "..kcccccccccpk..",
        "..kkkkkkkkkkkk..",
        "................",
        "................",
    ]),
    "spell/boulder": ({
        "k": 0x3A332C, "d": 0x6B6258, "b": 0x948A7C, "l": 0xBDB3A3, "w": 0xE6DED0,
    }, [
        "................",
        "................",
        "......kkkk......",
        "....kkblllkk....",
        "...kbbllwwlbk...",
        "..kbblwwlllbbk..",
        "..kblwwllbbbbk..",
        ".kbbllllbbbddbk.",
        ".kbbbbbbbbbddbk.",
        ".kdbbbkbbbbdddk.",
        ".kddbbkkbbbdddk.",
        "..kddbbkbbddkk..",
        "..kkdddbbddkk...",
        "...kkkddddkk....",
        ".....kkkkk......",
        "................",
    ]),
    "spell/stone_skin": ({
        "k": 0x3A332C, "d": 0x6B6258, "b": 0x948A7C, "l": 0xBDB3A3, "w": 0xE6DED0,
    }, [
        "................",
        "......kkkk......",
        "....kkllllkk....",
        "...kllbbbbllk...",
        "..klbbblbbbblk..",
        "..klbblwlbbblk..",
        ".klbbbbwbbbbblk.",
        ".klbkbwwwwlkblk.",
        ".klbbkbwbbkbblk.",
        ".klbblkkkbbbdlk.",
        "..klbbbkbbbdlk..",
        "..kldbbbbbddlk..",
        "...kldddddddk...",
        "....kkddddkk....",
        "......kkkk......",
        "................",
    ]),
    "spell/tremor": ({
        "k": 0x3A332C, "d": 0x6B6258, "b": 0x948A7C, "l": 0xBDB3A3, "e": 0x8A5F3A,
    }, [
        "................",
        "................",
        ".......l..l.....",
        "......bl.bl.....",
        ".....bbl.dbl....",
        "....kbdl..kdl...",
        "...kkd.k...kd...",
        "..kk.d.k....kk..",
        "eeekkeekkeeekkee",
        "eeeekkekeeekeeee",
        "kkkkkkkkkkkkkkkk",
        "eeeeekeeeekeeeee",
        "eeekeeeeeeeekeee",
        "kkkkkkkkkkkkkkkk",
        "................",
        "................",
    ]),
    "spell/wind_blade": ({
        "k": 0x3C6B5A, "g": 0x9FE0C8, "p": 0xD8F5EA, "w": 0xFFFFFF,
    }, [
        "................",
        "..........kk....",
        "...........wk...",
        "............wk..",
        "............pwk.",
        ".............pk.",
        ".............pk.",
        "......g......gk.",
        ".....gp.....gpk.",
        "....gpw....gpk..",
        "...gpw....gpk...",
        "..kpw...kgpk....",
        "..kkwwwwppk.....",
        "....kkkkkk......",
        "................",
        "................",
    ]),
    # Wet: a single fat water drop with a highlight.
    "mob_effect/wet": ({
        "k": 0x1A3F8C, "d": 0x2A6FD0, "b": 0x4FA8F0, "l": 0xB8E4FF, "w": 0xFFFFFF,
    }, [
        "..................",
        "........kk........",
        "........kk........",
        ".......kbbk.......",
        ".......kbbk.......",
        "......kbbbbk......",
        "......kwbbbk......",
        ".....kwlbbbbk.....",
        ".....klbbbbbk.....",
        "....kbbbbbbbdk....",
        "....kbbbbbbbdk....",
        "....kbbbbbbddk....",
        "....kbbbbbbddk....",
        ".....kbbbbddk.....",
        ".....kdbbdddk.....",
        "......kkddkk......",
        "........kk........",
        "..................",
    ]),
    # Riptide: a ring of water curling around itself.
    "mob_effect/riptide": ({
        "k": 0x1A3F8C, "d": 0x2A6FD0, "b": 0x4FA8F0, "l": 0xB8E4FF, "w": 0xFFFFFF,
    }, [
        "..................",
        "......lllll.......",
        "....lbbbbbbbl.....",
        "...lbbd...dbbw....",
        "..lbd.......dbw...",
        "..bd.........db...",
        ".lb...kddd....bl..",
        ".bd..kd..dk...db..",
        ".bd..d..l.d...db..",
        ".bd..d..bbd...db..",
        ".bd...kdd.....db..",
        ".lb...........bl..",
        "..bd.........db...",
        "..wbd.......dbl...",
        "...wbbd...dbbl....",
        ".....lbbbbbbl.....",
        ".......lllll......",
        "..................",
    ]),
    "mob_effect/airborne": ({
        "k": 0x3C6B5A, "g": 0x9FE0C8, "p": 0xD8F5EA, "w": 0xFFFFFF,
    }, [
        "..................",
        "........ww........",
        ".......wwww.......",
        "......ww..ww......",
        "........ww........",
        "........ww........",
        "..................",
        "...ppppppppppp....",
        ".....gggggggggg...",
        "..................",
        "....pppppppppp....",
        "......gggggggg....",
        "..................",
        ".....pppppppp.....",
        ".......gggggg.....",
        "..................",
        "..................",
        "..................",
    ]),
    # Hydro Jet: a pressurized stream shooting from bottom-left to top-right, bursting into spray.
    "spell/hydro_jet": ({
        "w": 0xFFFFFFFF, "l": 0xFFB8E4FF, "b": 0xFF4FA8F0, "d": 0xFF2A6FD0, "k": 0xFF1A3F8C,
    }, [
        "..........l.w.l.",
        "............lw.w",
        ".........w.lbbl.",
        "...........bbwbl",
        "..........bwlbw.",
        ".........bwlbd.l",
        "........bwlbd...",
        ".......bwlbd..w.",
        "......bwlbd.....",
        ".....bwlbd......",
        "....bwlbd.......",
        "...bwlbd........",
        "..kblbd.........",
        ".kdbbd..........",
        "kddkd...........",
        "kkk.............",
    ]),
    "particle/ember_0": ({"w": 0xFFFFFFFF, "y": 0xFFFFE89A, "o": 0x90FFB040}, [
        "........",
        "........",
        "...o....",
        "..oyo...",
        "...o....",
        "........",
        "........",
        "........",
    ]),
    "particle/ember_1": ({"w": 0xFFFFFFFF, "y": 0xFFFFE89A, "o": 0x90FFB040}, [
        "........",
        "........",
        "........",
        "...y....",
        "........",
        "........",
        "........",
        "........",
    ]),
    "particle/ember_2": ({"o": 0xC0FFB040}, [
        "........",
        "........",
        "........",
        "...o....",
        "........",
        "........",
        "........",
        "........",
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


def wind_streak(frame, frames=4, size=16):
    """A curved stroke of wind: a thin arc that is bright at its head and fades along its tail.
    Later frames are shorter and fainter. Procedural, since it needs smooth alpha."""
    image = np.zeros((size, size, 4), dtype=np.float64)
    center = np.array([size * 0.5, size * 0.95])
    radius = size * 0.6
    length = np.pi * (0.55 - 0.1 * frame)
    for step in range(200):
        t = step / 199
        angle = -np.pi / 2 - length / 2 + length * t
        point = center + radius * np.array([np.cos(angle), np.sin(angle)])
        strength = t ** 1.5 * (1 - 0.22 * frame)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                x, y = int(point[0]) + dx, int(point[1]) + dy
                if 0 <= x < size and 0 <= y < size:
                    falloff = 1.0 if dx == 0 and dy == 0 else 0.25
                    image[y, x, 3] = max(image[y, x, 3], strength * falloff)
    image[..., 0:3] = 1.0
    return Image.fromarray((np.clip(image, 0, 1) * 255).astype(np.uint8), "RGBA")


def hydro_drop(frame, frames=4, size=8):
    """A water droplet: a soft blue blob with a bright highlight; later frames shrink and fade."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    radius = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    scale = 1.0 - 0.18 * frame
    alpha = np.clip(1.2 - (radius / scale) ** 2, 0, 1) * (0.9 - 0.12 * frame)
    highlight = np.exp(-((x - size * 0.38) ** 2 + (y - size * 0.35) ** 2) / (size * 0.12) ** 2)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = 0.35 + 0.65 * highlight
    rgba[..., 1] = 0.7 + 0.3 * highlight
    rgba[..., 2] = 1.0
    rgba[..., 3] = np.clip(alpha + 0.3 * highlight * (alpha > 0), 0, 1)
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def essence(core, glow, edge, size=16):
    """A glowing crystal shard: a bright core fading to the element's color, a darker rim and a
    couple of sparkle pixels. core/glow/edge are (r, g, b)."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    cx, cy = size / 2, size / 2
    # Diamond (crystal) shape, a little taller than wide.
    shape = np.abs(x - cx) / (size * 0.34) + np.abs(y - cy) / (size * 0.46)
    inside = shape <= 1.0
    t = np.clip(shape, 0, 1)
    rgba = np.zeros((size, size, 4))
    for c in range(3):
        channel = core[c] * (1 - t) ** 1.5 + glow[c] * (1 - (1 - t) ** 1.5)
        channel = np.where(t > 0.8, edge[c], channel)
        rgba[..., c] = channel / 255
    rgba[..., 3] = np.where(inside, 1.0, 0.0)
    # Highlight streak and sparkles.
    for px, py in ((6, 5), (7, 4), (5, 7), (11, 3), (3, 12)):
        rgba[py, px, :3] = 1.0
        rgba[py, px, 3] = 1.0 if inside[py, px] else 0.85
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


def catalyst(core, glow, edge, size=16):
    """A Catalyst: a small corked vial of the element's light, a bright glow in a darker glass rim. core/glow/edge are (r, g, b)."""
    img = np.zeros((size, size, 4))
    y, x = np.mgrid[0:size, 0:size] + 0.5
    cx, cy = size / 2, size * 0.62
    # The flask body: a round belly.
    belly = ((x - cx) / (size * 0.34)) ** 2 + ((y - cy) / (size * 0.32)) ** 2
    neck = (np.abs(x - cx) <= 2.2) & (y >= size * 0.2) & (y <= size * 0.46)
    inside = (belly <= 1.0) | neck
    t = np.clip(belly, 0, 1)
    for c in range(3):
        channel = core[c] * (1 - t) ** 1.5 + glow[c] * (1 - (1 - t) ** 1.5)
        channel = np.where(t > 0.72, edge[c], channel)
        img[..., c] = channel / 255
    img[..., 3] = np.where(inside, 1.0, 0.0)
    # The cork and a highlight.
    cork = (np.abs(x - cx) <= 2.2) & (y >= size * 0.1) & (y < size * 0.22)
    img[cork] = [0.55, 0.38, 0.22, 1.0]
    for px, py in ((5, 9), (5, 10), (6, 8), (10, 7)):
        img[py, px] = [1, 1, 1, 1 if inside[py, px] else 0.9]
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA")


def stone_texture(seed, base, light, dark, cracks=True, size=16):
    """A pixel-art chunk of stone for Earth's projectiles: speckled blocks of three shades with a few
    dark cracks, in the style of the game's own stone. base/light/dark are (r, g, b)."""
    rng = np.random.default_rng(seed)
    noise = rng.random((size, size))
    # Chunky: smooth the noise over 2x2 blocks so it reads as stones, not static.
    blocky = noise.reshape(size // 2, 2, size // 2, 2).mean(axis=(1, 3))
    blocky = np.kron(blocky, np.ones((2, 2)))
    rgba = np.zeros((size, size, 4))
    rgba[..., 3] = 1.0
    for c in range(3):
        channel = np.where(blocky < 0.38, dark[c], np.where(blocky > 0.64, light[c], base[c]))
        # A little per-pixel speckle on top.
        channel = channel + (noise - 0.5) * 14
        rgba[..., c] = np.clip(channel, 0, 255) / 255
    if cracks:
        for _ in range(3):
            x, y = rng.integers(2, size - 2, size=2)
            for _ in range(rng.integers(3, 6)):
                rgba[y, x, :3] = np.array(dark) * 0.55 / 255
                x = int(np.clip(x + rng.integers(-1, 2), 0, size - 1))
                y = int(np.clip(y + rng.integers(0, 2), 0, size - 1))
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


def bubble_icon(size=16):
    """Spell icon: a round water bubble with a highlight and a little creature shadow inside."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - 8, y - 8.5)
    rgba = np.zeros((size, size, 4))
    inside = r <= 6.6
    rim = (r > 5.4) & inside
    rgba[inside] = (0.55, 0.8, 1.0, 0.45)
    rgba[rim] = (0.25, 0.55, 0.95, 1.0)
    shadow = (np.abs(x - 8) <= 1.6) & (y >= 7.5) & (y <= 11.5)
    rgba[shadow & inside & ~rim] = (0.12, 0.22, 0.4, 0.9)
    for px, py in ((5, 5), (6, 4), (4, 6), (7, 4)):
        rgba[py, px] = (1.0, 1.0, 1.0, 1.0)
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


# ---- Fireball looks (see docs/superpowers/specs/2026-09-29-fireball-vfx-design.md) ----

def tile_noise(size, seed, periods=(4, 8, 16)):
    """Smooth value noise that tiles seamlessly on a size x size square (fractal, 0..1)."""
    rng = np.random.default_rng(seed)
    out = np.zeros((size, size))
    total, amp = 0.0, 1.0
    for n in periods:
        grid = rng.random((n, n))
        xs = np.arange(size) * n / size
        x0 = np.floor(xs).astype(int) % n
        x1 = (x0 + 1) % n
        t = xs - np.floor(xs)
        t = t * t * (3 - 2 * t)
        ty, tx = np.meshgrid(t, t, indexing="ij")
        a = grid[np.ix_(x0, x0)]
        b = grid[np.ix_(x0, x1)]
        c = grid[np.ix_(x1, x0)]
        d = grid[np.ix_(x1, x1)]
        out += amp * ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty)
        total += amp
        amp *= 0.55
    return out / total


def color_ramp(stops, value):
    """stops: [(position 0..1, (r, g, b)), ...] -> an RGB array (0..1) for each value."""
    value = np.clip(value, 0, 1)
    rgb = np.zeros(value.shape + (3,))
    for c in range(3):
        rgb[..., c] = np.interp(value, [p for p, _ in stops], [col[c] / 255 for _, col in stops])
    return rgb


def even_stops(*colors):
    return [(i / (len(colors) - 1), c) for i, c in enumerate(colors)]


def fireball_strip(stops, seed, frames=16, size=16, style="flame"):
    """An animated fireball texture: frames stacked vertically. The noise scrolls up one pixel a
    frame (flames rising) and tiles, so the loop is seamless; the centre of each face is hottest."""
    noise = tile_noise(size, seed)
    seams = tile_noise(size, seed + 1, periods=(2, 4))
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    strip = np.zeros((size * frames, size, 4))
    for f in range(frames):
        n = np.roll(noise, -f, axis=0)
        heat = np.clip(1.15 - r * 0.8 + (n - 0.5) * 0.6, 0, 1)
        rgb = color_ramp(stops, heat)
        alpha = np.full((size, size), 0.93)
        if style == "meteor":
            # Dark rock with lava showing through the cracks, slowly shifting.
            m = np.roll(seams, -(f // 2), axis=0)
            crack = np.abs(m - 0.5) < 0.07
            rock = color_ramp(even_stops((28, 20, 18), (62, 44, 36), (96, 70, 54)), n)
            rgb = np.where(crack[..., None] | (r[..., None] < 0.3), rgb, rock)
            alpha[:] = 1.0
        elif style == "cluster":
            # A dark bomb shell split by a glowing cross-shaped seam, pulsing like a fuse.
            pulse = 0.7 + 0.3 * np.sin(f / frames * 2 * np.pi)
            seam = (np.abs(x - size / 2) < 1.1) | (np.abs(y - size / 2) < 1.1)
            core = r < 0.3
            shell = color_ramp(even_stops((34, 25, 25), (60, 44, 42)), n)
            glow = color_ramp(even_stops((255, 120, 30), (255, 210, 90)), n * pulse + 0.2) * pulse
            rgb = np.where((seam | core)[..., None], glow, shell)
            alpha[:] = 1.0
        strip[f * size:(f + 1) * size, :, :3] = rgb
        strip[f * size:(f + 1) * size, :, 3] = alpha
    return Image.fromarray((np.clip(strip, 0, 1) * 255).astype(np.uint8), "RGBA")


def ice_strip(stops, seed, alpha=0.88, style="ice", frames=16, size=16):
    """An animated icicle texture: frames stacked vertically. Ice grain streaks along the icicle, the
    edges catch the light, and a bright glint band sweeps across once per loop (seamless). Styles
    add a glowing core ("core"), bright length-wise streaks ("needle"), pulsing glowing cracks
    ("crack") or dark storm ice with drifting snow ("storm")."""
    rng = np.random.default_rng(seed)
    noise = tile_noise(size, seed)
    # Streaky grain: the noise smeared along one axis.
    grain = sum(np.roll(noise, k, axis=0) for k in range(-3, 4)) / 7
    seams = tile_noise(size, seed + 1, periods=(2, 4))
    y, x = np.mgrid[0:size, 0:size] + 0.5
    edge = np.abs(x - size / 2) / (size / 2)
    specks = rng.random((size, size)) < 0.06
    strip = np.zeros((size * frames, size, 4))
    for f in range(frames):
        t = f / frames
        value = 0.45 + (grain - 0.5) * 0.9 + 0.25 * edge ** 3
        # The glint: a bright diagonal band crossing once per loop.
        centre = t * 3.0 - 0.5
        band = np.exp(-(((x + y) / size - centre) / 0.12) ** 2)
        rgb = color_ramp(stops, value)
        a = np.full((size, size), alpha)
        if style == "core":
            pulse = 0.9 + 0.1 * np.sin(t * 2 * np.pi)
            core = np.clip(1.3 * np.exp(-((x - size / 2) / 3.2) ** 2), 0, 1) * pulse
            rgb = rgb * (1 - core[..., None]) + np.array([0.75, 1.0, 1.0]) * core[..., None]
            a = np.maximum(a, core)
        elif style == "needle":
            streak = (np.abs(x - 5.5) < 0.6) | (np.abs(x - 10.5) < 0.6)
            rgb = np.where(streak[..., None], np.array([1.0, 1.0, 1.0]), rgb)
        elif style == "crack":
            pulse = 0.75 + 0.25 * np.sin(t * 2 * np.pi)
            crack = np.abs(seams - 0.5) < 0.06
            glow = np.array([0.55, 1.0, 1.0]) * pulse
            rgb = np.where(crack[..., None], glow, rgb)
            a = np.where(crack, 1.0, a)
        elif style == "storm":
            falling = np.roll(specks, f, axis=0)
            rgb = np.where(falling[..., None], np.array([0.95, 0.97, 1.0]), rgb)
            a = np.where(falling, 1.0, a)
        rgb = np.clip(rgb + band[..., None] * 0.55, 0, 1)
        strip[f * size:(f + 1) * size, :, :3] = rgb
        strip[f * size:(f + 1) * size, :, 3] = a
    return Image.fromarray((np.clip(strip, 0, 1) * 255).astype(np.uint8), "RGBA")


# Icicle looks (IcicleSpell): (colour ramp, seed, alpha, style).
ICICLE_LOOKS = {
    "icicle_frost1": (even_stops((150, 180, 205), (190, 215, 235), (225, 240, 250), (245, 250, 255)), 0.92, "ice"),
    "icicle_frost2": (even_stops((70, 140, 210), (120, 190, 240), (185, 230, 255), (235, 250, 255)), 0.82, "ice"),
    "icicle_frost3": (even_stops((20, 60, 150), (40, 110, 200), (90, 170, 240), (180, 225, 255)), 0.86, "ice"),
    "icicle_frost4": (even_stops((120, 190, 235), (180, 230, 250), (225, 248, 255), (255, 255, 255)), 0.78, "core"),
    "icicle_piercing": (even_stops((140, 220, 255), (190, 240, 255), (235, 252, 255), (255, 255, 255)), 0.9, "needle"),
    "icicle_shatter": (even_stops((30, 80, 160), (60, 130, 210), (110, 180, 240), (190, 235, 255)), 0.9, "crack"),
    "icicle_winter": (even_stops((30, 40, 60), (55, 70, 95), (85, 105, 135), (130, 150, 180)), 0.95, "storm"),
    "icicle_lance": (even_stops((60, 150, 220), (120, 200, 245), (190, 240, 255), (255, 255, 255)), 0.85, "core"),
}


FIREBALL_LOOKS = {
    "fireball_heat1": (even_stops((90, 10, 5), (200, 40, 10), (255, 110, 20), (255, 170, 60)), "flame"),
    "fireball_heat2": (even_stops((140, 25, 5), (240, 90, 15), (255, 170, 40), (255, 230, 120)), "flame"),
    "fireball_heat3": (even_stops((200, 60, 10), (255, 150, 30), (255, 225, 110), (255, 250, 220)), "flame"),
    "fireball_heat4": (even_stops((230, 110, 20), (255, 200, 80), (255, 245, 200), (255, 255, 255)), "flame"),
    "fireball_cluster": (even_stops((200, 60, 10), (255, 140, 30), (255, 220, 120), (255, 250, 220)), "cluster"),
    "fireball_meteor": (even_stops((160, 30, 5), (240, 90, 15), (255, 170, 40), (255, 220, 120)), "meteor"),
    "fireball_sun": (even_stops((255, 170, 40), (255, 220, 110), (255, 250, 210), (255, 255, 255)), "flame"),
    "fireball_phoenix": (even_stops((120, 10, 30), (220, 30, 40), (255, 120, 40), (255, 215, 90)), "flame"),
}


def glow_sprite(size=64):
    """A soft round glow for additive halos: white in the middle fading to black (black adds nothing)."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    v = np.clip(1 - r, 0, 1) ** 2.2
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def flare(frame, frames=4, size=8):
    """The trail's glowing blob, in grey (the particle tints it; black adds nothing when blended
    additively): a round pixel blob in three brightness steps. Later frames are smaller and
    dissolve pixel by pixel."""
    rng = np.random.default_rng(300 + frame)
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    reach = 1.0 - 0.15 * frame
    v = np.where(r < 0.35 * reach, 1.0, np.where(r < 0.7 * reach, 0.7, np.where(r < reach, 0.4, 0.0)))
    v[rng.random((size, size)) < 0.12 * frame] = 0.0
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = (v > 0) * 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def spark(frame, size=8):
    """A spark streak along the sprite's x axis (the particle turns it to face its motion):
    a white-hot middle fading toward both ends, with a dim pixel above and below the middle."""
    image = np.zeros((size, size, 4))
    length = 7 if frame == 0 else 5
    start = (size - length) // 2
    mid = size // 2
    for i in range(length):
        t = abs((i + 0.5) / length - 0.5) * 2
        image[mid, start + i, 0:3] = 1.0 - 0.6 * t
        image[mid, start + i, 3] = 1.0
    for dy in (-1, 1):
        image[mid + dy, mid - 1:mid + 1, 0:3] = 0.35
        image[mid + dy, mid - 1:mid + 1, 3] = 1.0
    return Image.fromarray((image * 255).astype(np.uint8), "RGBA")


FEATHERS = [
    [
        "......w.",
        ".....wl.",
        "....wlg.",
        "...wlg..",
        "..wlg...",
        ".llg....",
        ".gg.....",
        "g.......",
    ],
    [
        "........",
        ".....wl.",
        "...wwlg.",
        "..wllg..",
        ".wlgg...",
        ".lg.....",
        "g.......",
        "........",
    ],
]
FEATHER_PALETTE = {"w": 0xFFFFFF, "l": 0xB0B0B0, "g": 0x606060}

CINDERS = [
    ["........", "...ab...", "..abbc..", "..bbcy..", "...cy...", "........", "........", "........"],
    ["........", "........", "..ab....", "..bby...", "...c....", "........", "........", "........"],
    ["........", "...a....", "..abb...", "..bbbc..", "..acy...", "...y....", "........", "........"],
]
CINDER_PALETTE = {"a": 0x2A1A14, "b": 0x4A2E22, "c": 0xFF8C32, "y": 0xFFD27A}


def shockwave(size=16):
    """A shockwave ring in grey (tinted by the particle): a bright pixel ring at the edge with a
    dimmer inner band, empty in the middle."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    v = np.where((r > 0.8) & (r <= 0.97), 1.0, np.where((r > 0.62) & (r <= 0.8), 0.45, 0.0))
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = (v > 0) * 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def corona(size=16):
    """A big soft round glow in four stepped rings, brightest in the middle."""
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2)
    v = np.select([r < 0.3, r < 0.55, r < 0.8, r < 1.0], [1.0, 0.7, 0.4, 0.2], 0.0)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = (v > 0) * 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def cracks(size=16, seed=7):
    """Glowing cracks radiating from the middle: random jagged lines, bright where they meet."""
    rng = np.random.default_rng(seed)
    v = np.zeros((size, size))
    for arm in range(7):
        angle = arm / 7 * 2 * np.pi + rng.uniform(-0.3, 0.3)
        pos = np.array([size / 2, size / 2], dtype=float)
        for step in range(int(size * 0.45)):
            angle += rng.uniform(-0.5, 0.5)
            pos += (np.cos(angle), np.sin(angle))
            px, py = int(pos[0]), int(pos[1])
            if 0 <= px < size and 0 <= py < size:
                v[py, px] = max(v[py, px], 1.0 - step / (size * 0.5))
    v[size // 2 - 1:size // 2 + 1, size // 2 - 1:size // 2 + 1] = 1.0
    v = np.where(v > 0, np.clip(v, 0.35, 1.0), 0.0)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = (v > 0) * 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def scorch(size=16, seed=11):
    """A dark burn mark: sooty black in the middle, ragged and thinning toward the edge."""
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2) + rng.uniform(-0.12, 0.12, (size, size))
    alpha = np.select([r < 0.45, r < 0.75, r < 0.95], [0.9, 0.65, 0.35], 0.0)
    shade = np.select([r < 0.45, r < 0.75], [0.06, 0.12], 0.18)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = shade
    rgba[..., 3] = alpha
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def frost(size=32, seed=17):
    """A frost mark: six-armed frost crystals branching out from the middle, with scattered
    speckle, pale blue-white, thinning toward the edge."""
    rng = np.random.default_rng(seed)
    a = np.zeros((size, size))
    c = size / 2

    def stroke(x0, y0, angle, length, strength):
        for step in range(int(length)):
            x = int(x0 + np.cos(angle) * step)
            y = int(y0 + np.sin(angle) * step)
            if 0 <= x < size and 0 <= y < size:
                a[y, x] = max(a[y, x], strength * (1 - 0.5 * step / length))

    for arm in range(6):
        angle = arm * np.pi / 3 + rng.uniform(-0.1, 0.1)
        length = size * 0.45
        stroke(c, c, angle, length, 1.0)
        # Fronds branching off each arm at 60 degrees, both sides.
        for k in range(2, int(length), 3):
            bx = c + np.cos(angle) * k
            by = c + np.sin(angle) * k
            frond = (length - k) * 0.4
            stroke(bx, by, angle + np.pi / 3, frond, 0.75)
            stroke(bx, by, angle - np.pi / 3, frond, 0.75)
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - c, y - c) / c
    speckle = (rng.random((size, size)) < 0.12) * rng.uniform(0.25, 0.6, (size, size))
    a = np.maximum(a, speckle * (r < 0.95))
    a *= np.clip(1.15 - r, 0, 1)
    a = np.where(a > 0.12, a, 0)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = 0.88
    rgba[..., 1] = 0.95
    rgba[..., 2] = 1.0
    rgba[..., 3] = np.clip(a, 0, 1)
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def wind_slash(style, width=64, height=16, seed=140):
    """A Wind Blade's slash texture, grey (the look tints it) on black (black adds nothing, it's
    drawn additively). U runs along the arc and tiles, so the streaks can flow; V runs across it:
    the cutting edge along the top, fading to nothing behind. Stepped into five levels, pixel-art
    style. Styles: soft (a wisp), sharp (a crisp edge, fine streaks), storm (swirls), lines (just
    razor edges)."""
    noise = tile_noise(width, seed, periods=(4, 8))
    u, v = np.mgrid[0:height, 0:width][1], np.mgrid[0:height, 0:width][0]
    across = v / (height - 1)
    # Wind lines: each row is dashed by its own slice of noise, sparser further back from the edge.
    row = noise[(v * 5 + 3) % width, u]
    dashes = row > (0.42 + 0.25 * across)
    if style == "soft":
        edge = np.exp(-(v / 2.5) ** 2) * 0.8
        body = np.where(dashes, (1 - across) ** 1.3 * 0.6, 0.0)
    elif style == "sharp":
        edge = np.where(v <= 1, 1.0, 0.0)
        body = np.where(dashes & (v >= 3) & (v % 2 == 1), (1 - across) ** 1.5 * 0.8, 0.0)
    elif style == "storm":
        edge = np.where(v <= 1, 0.75, 0.0)
        body = np.where(dashes, (1 - across) ** 1.1 * (0.35 + 0.4 * (row > 0.7)), 0.12 * (1 - across))
    else:
        edge = np.where(v <= 1, 1.0, 0.0)
        body = np.where((v == 4) & dashes, 0.5, 0.0)
    value = np.clip(np.maximum(edge, body), 0, 1)
    value = np.round(value * 4) / 4
    rgba = np.zeros((height, width, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = value
    rgba[..., 3] = 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def wind_funnel(size=16, seed=150):
    """The Tempest Edge funnel's wind, grey on black (drawn additively): diagonal streaks of air
    twisting around it, some brighter, in four steps."""
    noise = tile_noise(size, seed, periods=(2, 4))
    y, x = np.mgrid[0:size, 0:size]
    bands = ((x + y // 2) % 6) / 5.0
    value = np.where(bands < 0.35, 0.9, np.where(bands < 0.6, 0.45, 0.1)) * (0.6 + 0.4 * noise)
    value = np.round(np.clip(value, 0, 1) * 3) / 3
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = value
    rgba[..., 3] = 1.0
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def wind_cut(size=32):
    """A cut mark: three pale, thin slashes, tapering at both ends. It's drawn turned to run along
    the blade, so everything stays inside the inner circle (radius 0.35) to never reach the corners."""
    a = np.zeros((size, size))
    c = size / 2
    for offset, half, strength in ((-3, 8, 0.7), (0, 10, 0.95), (3, 6, 0.6)):
        row = int(c + offset)
        for x in range(size):
            d = abs(x + 0.5 - c) / half
            if d < 1:
                a[row, x] = max(a[row, x], strength * (1 - d ** 2))
    a = np.where(a > 0.15, a, 0)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = 0.96
    rgba[..., 1] = 1.0
    rgba[..., 2] = 0.98
    rgba[..., 3] = np.round(np.clip(a, 0, 1) * 4) / 4
    return Image.fromarray((rgba * 255).astype(np.uint8), "RGBA")


def wet(size=32, seed=170):
    """A wet patch: a dark, damp blotch in stepped pixel shades, ragged at the edge, with a few
    lighter glints of standing water."""
    rng = np.random.default_rng(seed)
    noise = tile_noise(size, seed, periods=(4, 8))
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - size / 2, y - size / 2) / (size / 2) + (noise - 0.5) * 0.35
    alpha = np.select([r < 0.5, r < 0.8, r < 0.95], [0.75, 0.55, 0.3], 0.0)
    shade = np.where(rng.random((size, size)) < 0.04, 0.45, 0.08 + 0.06 * noise)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = shade * 0.8
    rgba[..., 1] = shade * 0.9
    rgba[..., 2] = shade * 1.2
    rgba[..., 3] = alpha
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


def main():
    for name, (palette, grid) in SPRITES.items():
        path = ASSETS / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        render(palette, grid).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for frame in range(4):
        for name, make in (("frost_mist", frost_mist), ("wind_streak", wind_streak)):
            path = ASSETS / f"particle/{name}_{frame}.png"
            make(frame).save(path)
            print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
        path = ASSETS / f"particle/hydro_drop_{frame}.png"
        hydro_drop(frame).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for frame in range(4):
        path = ASSETS / f"particle/flare_{frame}.png"
        flare(frame).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for frame in range(2):
        path = ASSETS / f"particle/spark_{frame}.png"
        spark(frame).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for name, make in (("shockwave", shockwave), ("corona", corona)):
        path = ASSETS / f"particle/{name}.png"
        make().save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    # Marks painted onto blocks (client/decal/Decals): bigger, since they span several blocks.
    for name, make in (("cracks", cracks), ("scorch", scorch), ("frost", frost), ("wet", wet)):
        path = ASSETS / f"misc/{name}.png"
        make(32).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for name, sprites, palette in (("feather", FEATHERS, FEATHER_PALETTE), ("cinder", CINDERS, CINDER_PALETTE)):
        for frame, grid in enumerate(sprites):
            path = ASSETS / f"particle/{name}_{frame}.png"
            render(palette, grid).save(path)
            print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    essences = {
        "fire": ((255, 250, 200), (255, 140, 30), (170, 40, 10)),
        "water": ((225, 245, 255), (60, 150, 240), (20, 60, 150)),
        "ice": ((255, 255, 255), (160, 225, 255), (70, 140, 200)),
        "wind": ((245, 255, 250), (150, 225, 195), (60, 140, 110)),
        "earth": ((250, 235, 205), (190, 140, 85), (95, 62, 38)),
    }
    for path, make in ((ASSETS / "spell/bubble_prison.png", bubble_icon),):
        make().save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for i, (name, (stops, style)) in enumerate(FIREBALL_LOOKS.items()):
        path = ASSETS / f"block/{name}.png"
        fireball_strip(stops, 60 + i, style=style).save(path)
        path.with_suffix(".png.mcmeta").write_text('{"animation": {"frametime": 2, "interpolate": true}}\n')
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for i, (name, (stops, alpha, style)) in enumerate(ICICLE_LOOKS.items()):
        path = ASSETS / f"block/{name}.png"
        ice_strip(stops, 120 + i, alpha=alpha, style=style).save(path)
        path.with_suffix(".png.mcmeta").write_text('{"animation": {"frametime": 2, "interpolate": true}}\n')
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for path, make in ((ASSETS / "block/wind_funnel.png", wind_funnel), (ASSETS / "misc/wind_cut.png", wind_cut)):
        make().save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for style in ("soft", "sharp", "storm", "lines"):
        path = ASSETS / f"misc/wind_slash_{style}.png"
        wind_slash(style).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    path = ASSETS / "misc/glow.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    glow_sprite().save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for name, (seed, base, light, dark) in {
        "boulder": (301, (128, 122, 114), (160, 153, 142), (88, 83, 78)),
        "boulder_bedrock": (302, (96, 92, 98), (128, 124, 132), (58, 56, 62)),
        "stone_shard": (303, (140, 132, 120), (176, 168, 154), (96, 90, 82)),
    }.items():
        stone_path = ASSETS / f"block/{name}.png"
        stone_texture(seed, base, light, dark).save(stone_path)
        print("wrote", stone_path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for name, colors in essences.items():
        if name != "ice":
            catalyst_path = ASSETS / f"item/{name}_catalyst.png"
            catalyst(*colors).save(catalyst_path)
            print("wrote", catalyst_path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
        path = ASSETS / f"item/{name}_essence.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        essence(*colors).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


if __name__ == "__main__":
    main()
