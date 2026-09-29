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
    # Texture for the wind blade's crescent model (models/spell/wind_blade.json).
    "block/wind_blade": ({
        "w": 0xE0FFFFFF, "p": 0xC8E6FFF2, "g": 0xB0B8F0D8, "d": 0x9890D8C0,
    }, [
        "wwppwwppwwppwwpp",
        "wppgwppgwppgwppg",
        "ppggppggppggppgg",
        "pggdpggdpggdpggd",
        "wwppwwppwwppwwpp",
        "wppgwppgwppgwppg",
        "ppggppggppggppgg",
        "pggdpggdpggdpggd",
        "wwppwwppwwppwwpp",
        "wppgwppgwppgwppg",
        "ppggppggppggppgg",
        "pggdpggdpggdpggd",
        "wwppwwppwwppwwpp",
        "wppgwppgwppgwppg",
        "ppggppggppggppgg",
        "pggdpggdpggdpggd",
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
    # Texture for the fireball's model (models/spell/fireball.json): swirling flame, slightly translucent.
    "block/fireball": ({
        "w": 0xF4FFF6C8, "y": 0xF0FFD84A, "o": 0xE8FF8A1F, "r": 0xE0D83A1A,
    }, [
        "yoorrooyywwyoorr",
        "ooyyoorryywwyooo",
        "orryyooyywwyyoor",
        "rrooyyowwyyoorrr",
        "roowwyyowwyoorro",
        "oyywwyyoyyoorroo",
        "yyooyywwyooyyooy",
        "oorryyyywwyyoorr",
        "rroooyyyywwoorrr",
        "rooyywwoyyoorroo",
        "oyywwwoyyoorryyo",
        "yyowwyyoorryywwy",
        "oorryyoorryywwyy",
        "rroooyyoyywwyyoo",
        "ooyyoorryyooyyor",
        "yyooyyrroooyyorr",
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


def water_lance(size=16):
    """Flowing water for the Tsunami Lance model: translucent blue with bright streaks running
    along its length (the model's long axis maps to the texture's vertical)."""
    rng = np.random.default_rng(7)
    y, x = np.mgrid[0:size, 0:size]
    wave = np.sin(x * 1.3 + np.sin(y * 0.8) * 1.5) * 0.5 + 0.5
    streak = (wave > 0.82).astype(float) + 0.15 * rng.random((size, size))
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = 0.2 + 0.75 * streak
    rgba[..., 1] = 0.55 + 0.45 * streak
    rgba[..., 2] = 0.95 + 0.05 * streak
    rgba[..., 3] = np.clip(0.72 + 0.25 * streak, 0, 1)
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


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


def bubble_skin(size=16):
    """The Bubble Prison sphere's texture: mostly clear water with a few bright streaks, so the
    trapped creature shows through."""
    rng = np.random.default_rng(11)
    y, x = np.mgrid[0:size, 0:size]
    swirl = np.sin(x * 0.9 + np.sin(y * 0.7) * 1.8) * 0.5 + 0.5
    streak = (swirl > 0.9).astype(float)
    rgba = np.zeros((size, size, 4))
    rgba[..., 0] = 0.62 + 0.38 * streak
    rgba[..., 1] = 0.85 + 0.15 * streak
    rgba[..., 2] = 1.0
    rgba[..., 3] = 0.22 + 0.45 * streak + 0.06 * rng.random((size, size))
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
    path = ASSETS / "block/water_lance.png"
    water_lance().save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    essences = {
        "fire": ((255, 250, 200), (255, 140, 30), (170, 40, 10)),
        "water": ((225, 245, 255), (60, 150, 240), (20, 60, 150)),
        "ice": ((255, 255, 255), (160, 225, 255), (70, 140, 200)),
        "wind": ((245, 255, 250), (150, 225, 195), (60, 140, 110)),
    }
    for path, make in ((ASSETS / "block/bubble.png", bubble_skin), (ASSETS / "spell/bubble_prison.png", bubble_icon)):
        make().save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))
    for name, colors in essences.items():
        path = ASSETS / f"item/{name}_essence.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        essence(*colors).save(path)
        print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


if __name__ == "__main__":
    main()
