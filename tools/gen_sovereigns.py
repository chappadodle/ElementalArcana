#!/usr/bin/env python3
"""Generates the sanctum art (docs/superpowers/specs/2026-10-03-sanctums-and-sovereigns-design.md) as
pixel art:

- the four Sovereigns (64x64 entity textures laid out for client/SovereignModel), calm and enraged:
  a mask (each element its own face), a see-through shell, a core, a crown of shards, hands,
  orbiting plates and a fading tail;
- the Seal, for each element: side and top, dim (sealed) and lit (awake or restored); the top
  carries the element's old alchemical sign (fire and air point up, water and earth down; air and
  earth are crossed);
- the Sovereign Heart: a gold setting, and a grey heart gem the game tints with its element;
- the Hollow (docs/superpowers/specs/2026-10-03-the-hollow-design.md): a Sovereign gone black, with
  a toothed maw for a mouth, in each of its four forms (its eyes, core and teeth burn in that
  form's colour); the Prime Key and the Heart of the Prime.

Run from the project root:  python3 tools/gen_sovereigns.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


def faces(u, v, w, h, d):
    """The six face rectangles (x, y, width, height) of a model cube's texture at (u, v): top,
    bottom, then right, front, left and back."""
    return [
        (u + d, v, w, d), (u + d + w, v, w, d),
        (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h),
    ]


def put(img, x, y, color, alpha=255):
    img[y, x] = (*color, alpha)


def fill(img, rect, color, alpha=255):
    x, y, w, h = rect
    img[y:y + h, x:x + w] = (*color, alpha)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# Each Sovereign: its mask's stone or metal (dark, main, light), brows, accent, eye glow, eye
# white-hot, mouth glow; its shell (body, edge, alphas); core (rim, hot); crown (base, tip);
# plates (main, detail); and how its face is marked.
SOVEREIGNS = {
    "fire": dict(k=(30, 14, 12), m=(62, 32, 28), l=(104, 58, 44), B=(150, 40, 20), A=(255, 140, 40), E=(255, 170, 40),
                 w=(255, 245, 200), G=(255, 110, 30), body=(255, 110, 30), edge=(255, 190, 90), alpha=58, edge_alpha=170,
                 rim=(255, 170, 60), hot=(255, 248, 210), crown=(255, 120, 30), tip=(255, 235, 150),
                 plate=(110, 36, 18), detail=(255, 130, 40)),
    "water": dict(k=(60, 104, 124), m=(196, 226, 232), l=(240, 250, 252), B=(60, 140, 170), A=(80, 190, 215), E=(80, 200, 255),
                  w=(230, 250, 255), G=(60, 160, 220), body=(48, 132, 255), edge=(150, 210, 255), alpha=58, edge_alpha=165,
                  rim=(110, 190, 255), hot=(240, 252, 255), crown=(90, 210, 230), tip=(225, 255, 255),
                  plate=(80, 150, 140), detail=(150, 225, 210)),
    "wind": dict(k=(110, 140, 130), m=(232, 238, 234), l=(255, 255, 255), B=(130, 190, 170), A=(140, 225, 195), E=(140, 255, 205),
                 w=(240, 255, 250), G=(110, 215, 175), body=(120, 222, 178), edge=(215, 255, 236), alpha=46, edge_alpha=155,
                 rim=(170, 245, 210), hot=(255, 255, 255), crown=(170, 245, 215), tip=(255, 255, 255),
                 plate=(238, 244, 240), detail=(170, 210, 195)),
    "earth": dict(k=(46, 38, 32), m=(108, 94, 80), l=(148, 132, 112), B=(66, 56, 46), A=(110, 150, 62), E=(255, 186, 86),
                  w=(255, 240, 190), G=(255, 160, 60), body=(200, 150, 80), edge=(255, 205, 130), alpha=56, edge_alpha=170,
                  rim=(255, 180, 80), hot=(255, 244, 196), crown=(255, 186, 90), tip=(255, 240, 190),
                  plate=(118, 108, 98), detail=(108, 150, 58)),
}

# The face, 10x12 (the mask's front): k shadow, m main, l light, B brows, E eyes, w their hot
# centre, G the mouth's glow, A the element's marks. Each element then marks it its own way.
FACE = [
    "mmmmmmmmmm",
    "mmllllllmm",
    "mlmmmmmmlm",
    "mBBBmmBBBm",
    "mkEEkkEEkm",
    "mkEwkkwEkm",
    "mmkkmmkkmm",
    "mmmmllmmmm",
    "mmmmllmmmm",
    "mkGGGGGGkm",
    "mmkkkkkkmm",
    "kmmmmmmmmk",
]
MARKS = {
    # Vulkhar: horns of flame at the brow, burning streaks down the cheeks.
    "fire": {(0, 0): "A", (2, 0): "A", (7, 0): "A", (9, 0): "A", (1, 1): "A", (8, 1): "A",
             (1, 6): "A", (8, 6): "A", (1, 7): "A", (8, 7): "A", (1, 8): "G", (8, 8): "G"},
    # Thalassa: a wave across the brow, tear-like lines under the eyes.
    "water": {(1, 0): "A", (3, 0): "A", (5, 0): "A", (7, 0): "A", (2, 1): "A", (4, 1): "A", (6, 1): "A", (8, 1): "A",
              (2, 6): "A", (7, 6): "A", (2, 7): "A", (7, 7): "A", (2, 8): "A", (7, 8): "A"},
    # Caelum: a swirl on the brow, and an open mouth, blowing.
    "wind": {(3, 1): "A", (4, 1): "A", (5, 1): "A", (6, 2): "A", (5, 2): "A", (3, 2): "l",
             (2, 9): "m", (7, 9): "m", (3, 10): "G", (4, 10): "G", (5, 10): "G", (6, 10): "G"},
    # Orvald: cracks through the stone, and moss at the chin.
    "earth": {(3, 1): "k", (2, 2): "k", (6, 6): "k", (7, 7): "k", (7, 8): "k", (1, 11): "A", (3, 11): "A", (4, 11): "A",
              (6, 11): "A", (8, 11): "A", (0, 10): "A", (9, 10): "A"},
}
# Enraged: the cracks and marks catch fire with the element's glow.
RAGE_MARKS = {
    "fire": {(4, 2): "G", (5, 2): "G", (4, 1): "w", (5, 1): "w"},
    "water": {(4, 2): "G", (5, 2): "G"},
    "wind": {(4, 2): "G", (3, 2): "G"},
    "earth": {(3, 1): "G", (2, 2): "G", (6, 6): "G", (7, 7): "G", (7, 8): "G"},
}


def shell(img, rect, rng, c, alpha_bonus):
    x, y, w, h = rect
    for py in range(h):
        for px in range(w):
            border = px in (0, w - 1) or py in (0, h - 1)
            color = c["edge"] if border else c["body"]
            a = c["edge_alpha"] if border else c["alpha"] + int(rng.integers(-12, 13))
            if not border and rng.random() < 0.06:
                color, a = c["edge"], c["edge_alpha"]
            put(img, x + px, y + py, color, max(0, min(255, a + alpha_bonus)))


# The Hollow: its void stone, and each form's burning colour (glow, and a deeper shade).
FORM_GLOW = {"fire": ((255, 140, 40), (200, 60, 10)), "water": ((80, 190, 255), (30, 90, 200)),
             "wind": ((140, 255, 200), (50, 170, 120)), "earth": ((255, 186, 86), (170, 110, 40))}
HOLLOW_FACE = [
    "kmmmmmmmmk",
    "mmmllllmmm",
    "mlmmmmmmlm",
    "mBBmmmmBBm",
    "mEEkmmkEEm",
    "mEwkmmkwEm",
    "mmkmmmmkmm",
    "mkGkGkGkGm",
    "mGkGkGkGkm",
    "mkGGGGGGkm",
    "mGkGkGkGkm",
    "kmkmkmkmkk",
]


def hollow_palette(form):
    glow, deep = FORM_GLOW[form]
    return dict(k=(8, 6, 12), m=(26, 18, 36), l=(54, 40, 72), B=(64, 30, 96), A=glow, E=glow, w=(255, 255, 255), G=glow,
                body=(34, 12, 56), edge=(120, 60, 180), alpha=120, edge_alpha=190, rim=deep, hot=glow,
                crown=(40, 20, 60), tip=glow, plate=(20, 12, 30), detail=glow)


def sovereign(name, enraged, seed, palette=None, face=None):
    c = dict(palette or SOVEREIGNS[name])
    if enraged:
        c["E"] = c["w"]
        c["hot"] = (255, 255, 255)
        c["rim"] = mix(c["rim"], (255, 255, 255), 0.35)
        c["B"] = mix(c["B"], c["G"], 0.5)
    rng = np.random.default_rng(seed)
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    # Shell: 10x10x10 at (0, 0).
    for rect in faces(0, 0, 10, 10, 10):
        shell(img, rect, rng, c, 30 if enraged else 0)
    # Core: 6x6x6 at (40, 0), a rim of its element round a white-hot middle.
    for x, y, w, h in faces(40, 0, 6, 6, 6):
        fill(img, (x, y, w, h), c["rim"])
        fill(img, (x + 1, y + 1, w - 2, h - 2), c["hot"])
    # Mask: 10x12x2 at (0, 20): the face in front, its stone everywhere else.
    for i, (x, y, w, h) in enumerate(faces(0, 20, 10, 12, 2)):
        for py in range(h):
            for px in range(w):
                shade = c["k"] if py == h - 1 or px in (0, w - 1) and h > 2 else c["m"]
                if rng.random() < 0.12:
                    shade = mix(shade, c["l"], 0.5)
                put(img, x + px, y + py, shade)
        if i == 3:
            grid = [list(row) for row in (face or FACE)]
            for (fx, fy), ch in ({} if face else {**MARKS[name], **(RAGE_MARKS[name] if enraged else {})}).items():
                grid[fy][fx] = ch
            for py, row in enumerate(grid):
                for px, ch in enumerate(row):
                    put(img, x + px, y + py, c[ch])
    # Hands: 4x4x4 at (24, 20): its mask's stone, the palm (front) glowing.
    for i, (x, y, w, h) in enumerate(faces(24, 20, 4, 4, 4)):
        fill(img, (x, y, w, h), c["m"])
        for px in range(w):
            put(img, x + px, y + h - 1, c["k"])
        if i == 3:
            fill(img, (x + 1, y + 1, 2, 2), c["E"])
    # Crown shard: 1x5x1 at (24, 28), its element's crystal brightening to the tip.
    for x, y, w, h in faces(24, 28, 1, 5, 1):
        for py in range(h):
            put(img, x, y + py, mix(c["tip"], c["crown"], py / max(1, h - 1)))
    # Plates: 4x4x1 at (28, 28).
    for x, y, w, h in faces(28, 28, 4, 4, 1):
        fill(img, (x, y, w, h), c["plate"])
        if w == 4 and h == 4:
            for px, py in ((1, 0), (2, 1), (1, 2), (2, 3)) if name != "wind" else ((0, 1), (1, 1), (2, 2), (3, 2)):
                put(img, x + px, y + py, c["detail"])
    # Tail: 4x4x4 at (40, 20) and its 2x3x2 tip at (40, 28), fading away.
    for rect in faces(40, 20, 4, 4, 4):
        fill(img, rect, c["body"], 170 if not enraged else 200)
    for rect in faces(40, 28, 2, 3, 2):
        fill(img, rect, c["edge"], 110 if not enraged else 150)
    return Image.fromarray(img, "RGBA")


# ---- the Seal ----
SEAL_STONE = {
    "fire": ((44, 38, 44), (28, 24, 30), (255, 120, 30)),
    "water": ((92, 158, 150), (58, 112, 108), (110, 230, 255)),
    "wind": ((226, 226, 218), (186, 188, 180), (120, 240, 190)),
    "earth": ((82, 82, 92), (50, 50, 58), (255, 180, 70)),
}
# The alchemical signs: fire and air point up, water and earth down; air and earth are crossed.
SIGNS = {"fire": (True, False), "water": (False, False), "wind": (True, True), "earth": (False, True)}


def seal_side(name, lit, seed):
    stone, dark, glow = SEAL_STONE[name]
    glow = glow if lit else mix(glow, dark, 0.6)
    rng = np.random.default_rng(seed)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            n = rng.random()
            color = dark if n < 0.2 else stone
            jitter = int(rng.integers(-8, 9))
            put(img, x, y, tuple(max(0, min(255, v + jitter)) for v in color))
    for i in range(16):
        put(img, i, 0, dark)
        put(img, i, 15, dark)
        put(img, 0, i, dark)
        put(img, 15, i, dark)
    # A column of runes down the middle.
    runes = ["..x.", ".xxx", "..x.", "x..x", "xxxx", "....", ".x.x", "..x.", ".x.x", "....", "xx..", ".xx.", "..xx", "....", ]
    for row, pattern in enumerate(runes):
        for col, ch in enumerate(pattern):
            if ch == "x":
                put(img, 6 + col, 1 + row, glow)
    if lit:
        put(img, 7, 1, (255, 255, 255))
        put(img, 8, 5, (255, 255, 255))
    return Image.fromarray(img, "RGBA")


def seal_top(name, lit, seed):
    stone, dark, glow = SEAL_STONE[name]
    glow = glow if lit else mix(glow, dark, 0.6)
    bright = (255, 255, 255) if lit else glow
    rng = np.random.default_rng(seed)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            jitter = int(rng.integers(-8, 9))
            put(img, x, y, tuple(max(0, min(255, v + jitter)) for v in stone))
    up, crossed = SIGNS[name]
    for y in range(16):
        for x in range(16):
            r = np.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if 6.0 <= r < 7.0:
                put(img, x, y, glow)
            elif 6.9 <= r < 7.9:
                put(img, x, y, dark)
    # The triangle: rows 4..11, its point at the top (fire, air) or the bottom (water, earth).
    for i in range(8):
        y = 4 + i if up else 11 - i
        half = i * 0.5
        left, right = round(7.5 - half), round(8.5 + half) - 1
        if i == 7:
            for x in range(left, right + 1):
                put(img, x, y, glow)
        else:
            put(img, left, y, glow)
            put(img, right, y, glow)
    if crossed:
        y = 8 if up else 7
        for x in range(4, 12):
            put(img, x, y, bright)
    put(img, 7 if up else 8, 4 if up else 11, bright)
    return Image.fromarray(img, "RGBA")


# ---- the Sovereign Heart ----
def heart_mask():
    mask = np.zeros((16, 16), dtype=bool)
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            in_circle = np.hypot(px - 5.6, py - 6.2) <= 3.1 or np.hypot(px - 10.4, py - 6.2) <= 3.1
            in_triangle = 7.0 <= py <= 13.0 and abs(px - 8) <= (13.2 - py) * 0.95
            mask[y, x] = in_circle or in_triangle
    return mask


def dilate(mask):
    out = mask.copy()
    out[1:, :] |= mask[:-1, :]
    out[:-1, :] |= mask[1:, :]
    out[:, 1:] |= mask[:, :-1]
    out[:, :-1] |= mask[:, 1:]
    return out


def heart():
    gem = heart_mask()
    setting = dilate(gem) & ~gem
    outline = dilate(dilate(gem)) & ~dilate(gem)
    base = np.zeros((16, 16, 4), dtype=np.uint8)
    tint = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            if outline[y, x]:
                base[y, x] = (40, 26, 16, 255)
            elif setting[y, x]:
                base[y, x] = (242, 215, 116, 255) if y < 8 else (201, 162, 62, 255)
            elif gem[y, x]:
                # Light from the upper left: greys the game tints with the element.
                t = ((x - 3) + (y - 3)) / 20
                grey = int(np.clip(235 - t * 130, 110, 245))
                tint[y, x] = (grey, grey, grey, 255)
    for x, y in ((4, 4), (5, 4), (4, 5), (11, 5)):
        if gem[y, x]:
            tint[y, x] = (255, 255, 255, 255)
    return Image.fromarray(base, "RGBA"), Image.fromarray(tint, "RGBA")


def prime_key():
    """A silver key whose bow holds the four elements' gems."""
    grid = [
        "................",
        "...kkkkk........",
        "..kSfSsSk.......",
        ".kSf...wSk......",
        ".kS.....Sk......",
        ".kSa...eSk......",
        "..kSaSeSkk......",
        "...kkkkkSSk.....",
        "........kSSk....",
        ".........kSSk...",
        "..........kSSk..",
        ".........kSkSSk.",
        "........kSk.kSSk",
        ".........k...kSk",
        "..............k.",
        "................",
    ]
    palette = {"k": (40, 38, 52), "S": (214, 218, 230), "s": (255, 255, 255),
               "f": (255, 122, 31), "w": (63, 156, 255), "a": (143, 227, 192), "e": (181, 137, 90)}
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y, row in enumerate(grid):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch != ".":
                put(img, x, y, palette[ch])
    return Image.fromarray(img, "RGBA")


def prime_heart():
    """The Heart of the Prime: a heart of all four elements' colours, set in gold."""
    gem = heart_mask()
    setting = dilate(gem) & ~gem
    outline = dilate(dilate(gem)) & ~dilate(gem)
    stops = [(255, 122, 31), (255, 225, 77), (143, 227, 192), (63, 156, 255), (208, 140, 255)]
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            if outline[y, x]:
                put(img, x, y, (40, 26, 16))
            elif setting[y, x]:
                put(img, x, y, (242, 215, 116) if y < 8 else (201, 162, 62))
            elif gem[y, x]:
                t = min(0.999, max(0.0, (x + y - 5) / 20))
                i = int(t * (len(stops) - 1))
                put(img, x, y, mix(stops[i], stops[i + 1], t * (len(stops) - 1) - i))
    for x, y in ((4, 4), (5, 4), (4, 5), (11, 5), (8, 9)):
        if gem[y, x]:
            put(img, x, y, (255, 255, 255))
    return Image.fromarray(img, "RGBA")


def main():
    for i, name in enumerate(SOVEREIGNS):
        save(sovereign(name, False, 1100 + i), ASSETS / f"entity/sovereign/{name}.png")
        save(sovereign(name, True, 1100 + i), ASSETS / f"entity/sovereign/{name}_enraged.png")
        for lit in (False, True):
            suffix = "_lit" if lit else ""
            save(seal_side(name, lit, 1200 + i), ASSETS / f"block/sanctum_seal_{name}_side{suffix}.png")
            save(seal_top(name, lit, 1300 + i), ASSETS / f"block/sanctum_seal_{name}_top{suffix}.png")
    base, tint = heart()
    save(base, ASSETS / "item/sovereign_heart.png")
    save(tint, ASSETS / "item/sovereign_heart_gem.png")
    for i, form in enumerate(FORM_GLOW):
        save(sovereign(form, False, 1400 + i, hollow_palette(form), HOLLOW_FACE), ASSETS / f"entity/sovereign/hollow_{form}.png")
    save(prime_key(), ASSETS / "item/prime_key.png")
    save(prime_heart(), ASSETS / "item/prime_heart.png")


if __name__ == "__main__":
    main()
