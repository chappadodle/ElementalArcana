#!/usr/bin/env python3
"""Generates the Arcanist's art and buildings (docs/superpowers/specs/2026-10-02-arcanist-design.md):

- the Arcanist's clothes, as villager and zombie-villager profession layers (64x64, the villager
  model's texture layout): a purple robe with gold trim and a wide-brimmed hat;
- the Arcane Lectern's textures and the two items Arcanists sell (Scroll of Unbinding, Tome of
  Insight);
- the Arcanist's cottage, one structure template per village type (plains, desert, savanna, snowy,
  taiga), in that village's own wood and stone, with the village's jigsaw connectors.

Run from the project root:  python3 tools/gen_people.py
"""
import gzip
import struct
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
TEXTURES = ROOT / "assets/elementalarcana/textures"
STRUCTURES = ROOT / "data/elementalarcana/structure/village"
DATA_VERSION = 3955  # Minecraft 1.21.1

ROBE = (96, 60, 156)
ROBE_DARK = (68, 42, 118)
ROBE_LIGHT = (128, 90, 194)
GOLD = (226, 186, 74)
GOLD_DARK = (164, 124, 40)
STAR = (206, 190, 255)
BOOT = (34, 24, 30)


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ROOT.parent.parent.parent))


def faces(u, v, w, h, d):
    """A model cube's texture faces at (u, v): name -> (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


class Canvas:
    def __init__(self, size=64, seed=0):
        self.px = np.zeros((size, size, 4), dtype=np.uint8)
        self.rng = np.random.default_rng(seed)

    def cloth(self, rect, base=ROBE, dark=ROBE_DARK, light=ROBE_LIGHT, stars=0.0):
        x, y, w, h = rect
        for py in range(h):
            for px in range(w):
                n = self.rng.random()
                c = dark if n < 0.16 else light if n > 0.93 else base
                if stars and self.rng.random() < stars:
                    c = STAR
                self.px[y + py, x + px] = (*c, 255)

    def row(self, rect, offset, color):
        x, y, w, h = rect
        self.px[y + offset, x:x + w] = (*color, 255)

    def column(self, rect, offset, color, start=0):
        x, y, w, h = rect
        self.px[y + start:y + h, x + offset] = (*color, 255)

    def clear(self, rect):
        x, y, w, h = rect
        self.px[y:y + h, x:x + w] = 0

    def image(self):
        return Image.fromarray(self.px, "RGBA")


def hat(c):
    """The cap (the head's overlay) and its wide round brim (hat_rim)."""
    cap = faces(32, 0, 8, 10, 8)
    c.cloth(cap["top"], stars=0.04)
    c.px[cap["top"][1] + 3:cap["top"][1] + 5, cap["top"][0] + 3:cap["top"][0] + 5] = (*GOLD, 255)
    for name in ("right", "front", "left", "back"):
        rect = cap[name]
        c.cloth(rect, stars=0.03)
        c.row(rect, 5, GOLD)
        c.row(rect, 6, GOLD_DARK)
        if name != "back":
            # The face shows below the band.
            c.clear((rect[0], rect[1] + 7, rect[2], rect[3] - 7))
    rim = faces(30, 47, 16, 16, 1)
    for name in ("front", "back"):
        x, y, w, h = rim[name]
        for py in range(h):
            for px in range(w):
                r = np.hypot(px + 0.5 - 8, py + 0.5 - 8)
                if r > 7.9:
                    continue
                color = GOLD if r > 6.9 else ROBE_DARK if c.rng.random() < 0.15 else ROBE
                c.px[y + py, x + px] = (*color, 255)
    for name in ("top", "bottom", "right", "left"):
        c.cloth(rim[name], base=ROBE_DARK)


def robe(c, ragged=False):
    """Body and the long robe over it (the jacket), with a gold stripe, belt and hem."""
    body = faces(16, 20, 8, 12, 6)
    for rect in body.values():
        c.cloth(rect, stars=0.02)
    c.column(body["front"], 3, GOLD)
    c.column(body["front"], 4, GOLD)
    jacket = faces(0, 38, 8, 20, 6)
    for name, rect in jacket.items():
        c.cloth(rect, stars=0.025)
        if name in ("right", "front", "left", "back"):
            c.row(rect, 6, GOLD_DARK)  # the belt
            c.row(rect, rect[3] - 2, GOLD)
            c.row(rect, rect[3] - 1, GOLD_DARK)
    c.column(jacket["front"], 3, GOLD, start=7)
    c.column(jacket["front"], 4, GOLD, start=7)
    if ragged:
        # Torn hem and holes: transparent bites out of the bottom rows.
        for name in ("right", "front", "left", "back"):
            x, y, w, h = jacket[name]
            for px in range(w):
                bite = int(c.rng.integers(0, 4))
                c.px[y + h - bite:y + h, x + px] = 0
            for _ in range(3):
                hx, hy = int(c.rng.integers(0, w)), int(c.rng.integers(8, h - 4))
                c.px[y + hy, x + hx] = 0


def legs(c):
    leg = faces(0, 22, 4, 12, 4)
    for name, rect in leg.items():
        c.cloth(rect, base=ROBE_DARK, dark=(30, 18, 56), light=ROBE)
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 2, BOOT)
            c.row(rect, rect[3] - 1, BOOT)
    c.cloth(leg["bottom"], base=BOOT, dark=BOOT, light=BOOT)


def villager_arms(c):
    """The crossed arms: sleeves with gold cuffs; the hands (middle of the forearm box) stay bare."""
    side = faces(44, 22, 4, 8, 4)
    for name, rect in side.items():
        c.cloth(rect)
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 1, GOLD)
    middle = faces(40, 38, 8, 4, 4)
    for name, rect in middle.items():
        c.cloth(rect)
    for name in ("front", "top", "bottom"):
        x, y, w, h = middle[name]
        c.clear((x + 2, y, w - 4, h))
        c.px[y:y + h, x + 1] = (*GOLD, 255)
        c.px[y:y + h, x + w - 2] = (*GOLD, 255)


def zombie_arms(c):
    """The zombie villager's outstretched arms (a humanoid arm box)."""
    arm = faces(44, 38, 4, 12, 4)
    for name, rect in arm.items():
        c.cloth(rect, base=(52, 40, 84), dark=(36, 28, 60), light=(70, 60, 104))
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 4, GOLD_DARK)
            x, y, w, h = rect
            c.px[y + h - 3:y + h, x:x + w] = 0  # torn sleeve: the hands show


def arcanist_clothes():
    c = Canvas(seed=1101)
    hat(c)
    robe(c)
    legs(c)
    villager_arms(c)
    save(c.image(), TEXTURES / "entity/villager/profession/arcanist.png")
    z = Canvas(seed=1102)
    hat(z)
    robe(z, ragged=True)
    legs(z)
    zombie_arms(z)
    # Faded with the grave.
    rgb = z.px[..., :3].astype(np.float64)
    grey = rgb.mean(axis=2, keepdims=True)
    z.px[..., :3] = np.clip(rgb * 0.7 + grey * 0.3 - 8, 0, 255).astype(np.uint8)
    save(z.image(), TEXTURES / "entity/zombie_villager/profession/arcanist.png")
    meta = '{\n  "villager": {\n    "hat": "full"\n  }\n}\n'
    for kind in ("villager", "zombie_villager"):
        (TEXTURES / f"entity/{kind}/profession/arcanist.png.mcmeta").write_text(meta)


# ---- the Arcane Lectern and items (16x16) ----

WOOD = (62, 40, 46)
WOOD_DARK = (40, 26, 32)
WOOD_LIGHT = (86, 58, 62)


def planks(seed, trim=True):
    rng = np.random.default_rng(seed)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            n = rng.random()
            c = WOOD_DARK if y % 4 == 3 or n < 0.12 else WOOD_LIGHT if n > 0.9 else WOOD
            img[y, x] = (*c, 255)
    if trim:
        img[0, :] = (*GOLD, 255)
        img[15, :] = (*GOLD_DARK, 255)
    return img


def lectern_textures():
    base = planks(1201)
    save(Image.fromarray(base, "RGBA"), TEXTURES / "block/arcane_lectern_base.png")
    post = planks(1202, trim=False)
    for y in range(2, 14, 3):
        post[y, 7:9] = (*GOLD, 255)  # a column of rune marks
        post[y + 1, 6:10] = (*GOLD_DARK, 255)
    post[:, 0] = (*WOOD_DARK, 255)
    post[:, 15] = (*WOOD_DARK, 255)
    save(Image.fromarray(post, "RGBA"), TEXTURES / "block/arcane_lectern_post.png")
    desk = planks(1203)
    desk[:, 0] = (*GOLD, 255)
    desk[:, 15] = (*GOLD, 255)
    save(Image.fromarray(desk, "RGBA"), TEXTURES / "block/arcane_lectern_desk.png")

    # The open book: two cream pages, a spine, and violet runes that glow (the model is fullbright).
    rng = np.random.default_rng(1204)
    book = np.zeros((16, 16, 4), dtype=np.uint8)
    book[:, :] = (236, 224, 196, 255)
    book[:, 7:9] = (120, 84, 60, 255)
    book[0, :] = book[15, :] = (88, 50, 110, 255)
    book[:, 0] = book[:, 15] = (88, 50, 110, 255)
    for y in range(3, 13, 2):
        for x in list(range(2, 7)) + list(range(9, 14)):
            if rng.random() < 0.55:
                book[y, x] = (150, 92, 230, 255)
    save(Image.fromarray(book, "RGBA"), TEXTURES / "block/arcane_lectern_book.png")

    y, x = np.mgrid[0:16, 0:16] + 0.5
    shade = 170 + 60 * np.cos((x - 8) / 16 * np.pi) + 10 * np.sin(y / 2)
    crystal = np.zeros((16, 16, 4), dtype=np.uint8)
    crystal[..., 0] = np.clip(shade * 0.78, 0, 255)
    crystal[..., 1] = np.clip(shade * 0.52, 0, 255)
    crystal[..., 2] = np.clip(shade * 1.0, 0, 255)
    crystal[..., 3] = 255
    for i in range(16):
        crystal[i, (i // 2 + 5) % 16, :3] = (240, 220, 255)
    save(Image.fromarray(crystal, "RGBA"), TEXTURES / "block/arcane_lectern_crystal.png")


def grid_image(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, (len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch] + (255,))
    return img


SCROLL = [
    "................",
    "................",
    "...kkkkkkkkk....",
    "..kPPPPPPPPPk...",
    "..kpkkkkkkkpk...",
    "...kPPPPPPPk....",
    "...kPrrPrrPk....",
    "...kPPPPPPPk....",
    "...kPrrrPrPk....",
    "...kPPPPPPPkk...",
    "...kPrPrrPPkSk..",
    "..kkkkkkkkkkSSk.",
    "..kPPPPPPPPPkSk.",
    "..kpkkkkkkkpkk..",
    "...kkkkkkkkk....",
    "................",
]
TOME = [
    "................",
    "...kkkkkkkkkk...",
    "..kBBBBBBBBBBk..",
    "..kBbbbbbbbbBkw.",
    "..kBbBBGBBbBBkw.",
    "..kBbBGGGBbBBkw.",
    "..kBbGGgGGbBBkw.",
    "..kBbBGGGBbBBkw.",
    "..kBbBBGBBbBBkw.",
    "..kBbbbbbbbbBkw.",
    "..kBBBBBBBBBBkw.",
    "..kBBBBBBBBBBkw.",
    "..kbbbbbbbbbbkw.",
    "...kkkkkkkkkkww.",
    "................",
    "................",
]


def items():
    save(grid_image(SCROLL, {"k": (70, 48, 30), "P": (236, 220, 180), "p": (200, 180, 140),
                             "r": (150, 92, 230), "S": (140, 30, 60)}),
         TEXTURES / "item/scroll_of_unbinding.png")
    save(grid_image(TOME, {"k": (26, 14, 40), "B": (70, 40, 120), "b": (96, 62, 150),
                           "G": GOLD, "g": (255, 248, 210), "w": (236, 224, 196)}),
         TEXTURES / "item/tome_of_insight.png")


# ---- NBT (structure templates) ----

class Tag:
    def __init__(self, kind, value):
        self.kind, self.value = kind, value


def int_tag(v):
    return Tag(3, v)


def string_tag(v):
    return Tag(8, v)


def write_payload(out, tag):
    k, v = tag.kind, tag.value
    if k == 3:
        out += struct.pack(">i", v)
    elif k == 8:
        data = v.encode("utf-8")
        out += struct.pack(">H", len(data)) + data
    elif k == 9:
        element_kind = v[0].kind if v else 0
        out += struct.pack(">bi", element_kind, len(v))
        for item in v:
            write_payload(out, item)
    elif k == 10:
        for name, item in v.items():
            out += struct.pack(">b", item.kind)
            data = name.encode("utf-8")
            out += struct.pack(">H", len(data)) + data
            write_payload(out, item)
        out += b"\x00"
    else:
        raise ValueError(k)


def compound(**items):
    return Tag(10, items)


def nbt_value(v):
    if isinstance(v, Tag):
        return v
    if isinstance(v, int):
        return int_tag(v)
    if isinstance(v, str):
        return string_tag(v)
    if isinstance(v, dict):
        return Tag(10, {k: nbt_value(x) for k, x in v.items()})
    if isinstance(v, list):
        return Tag(9, [nbt_value(x) for x in v])
    raise TypeError(v)


def write_structure(path, size, blocks):
    """blocks: {(x, y, z): (name, properties, block entity nbt or None)}."""
    palette, palette_index, entries = [], {}, []
    for pos in sorted(blocks, key=lambda p: (p[1], p[2], p[0])):
        name, props, nbt = blocks[pos]
        key = (name, tuple(sorted(props.items())))
        if key not in palette_index:
            palette_index[key] = len(palette)
            state = {"Name": name}
            if props:
                state["Properties"] = dict(props)
            palette.append(nbt_value(state))
        entry = {"pos": [int_tag(c) for c in pos], "state": int_tag(palette_index[key])}
        if nbt is not None:
            entry["nbt"] = nbt_value(nbt)
        entries.append(nbt_value(entry))
    root = compound(DataVersion=int_tag(DATA_VERSION), size=Tag(9, [int_tag(s) for s in size]),
                    palette=Tag(9, palette), blocks=Tag(9, entries), entities=Tag(9, []))
    out = bytearray()
    out += struct.pack(">b", 10) + struct.pack(">H", 0)
    write_payload(out, root)
    path.parent.mkdir(parents=True, exist_ok=True)
    with gzip.open(path, "wb") as f:
        f.write(bytes(out))
    print("wrote", path.relative_to(ROOT.parent.parent.parent))


# ---- the Arcanist's cottage ----
# 8 (x) by 7 (z) by 7 (y). The door is in the west wall; the street connects at (0, 0, 3).

VILLAGES = {
    "plains": dict(planks="oak_planks", post="oak_log", stairs="dark_oak_stairs", roof="dark_oak_planks",
                   floor="oak_planks", base="cobblestone", door="oak_door", doorstep="oak_stairs"),
    "desert": dict(planks="smooth_sandstone", post="cut_sandstone", stairs="sandstone_stairs", roof="cut_sandstone",
                   floor="smooth_sandstone", base="sandstone", door="birch_door", doorstep="sandstone_stairs"),
    "savanna": dict(planks="acacia_planks", post="acacia_log", stairs="acacia_stairs", roof="acacia_planks",
                    floor="acacia_planks", base="terracotta", door="acacia_door", doorstep="acacia_stairs"),
    "snowy": dict(planks="spruce_planks", post="stripped_spruce_log", stairs="spruce_stairs", roof="spruce_planks",
                  floor="spruce_planks", base="stone_bricks", door="spruce_door", doorstep="stone_brick_stairs"),
    "taiga": dict(planks="spruce_planks", post="spruce_log", stairs="spruce_stairs", roof="spruce_planks",
                  floor="spruce_planks", base="cobblestone", door="spruce_door", doorstep="spruce_stairs"),
}


def cottage(village, p):
    mc = lambda name: name if ":" in name else "minecraft:" + name
    blocks = {}

    def put(x, y, z, name, nbt=None, **props):
        blocks[(x, y, z)] = (mc(name), {k: str(v).lower() for k, v in props.items()}, nbt)

    for x in range(8):
        for y in range(7):
            for z in range(7):
                put(x, y, z, "air")

    # Floor (cobble rim, planks inside) and walls with log posts.
    for x in range(1, 8):
        for z in range(7):
            edge = x in (1, 7) or z in (0, 6)
            put(x, 0, z, p["base"] if edge else p["floor"])
            if edge:
                corner = x in (1, 7) and z in (0, 6)
                for y in range(1, 4):
                    put(x, y, z, p["post"] if corner else p["planks"], **({"axis": "y"} if corner and "log" in p["post"] else {}))
    # Windows of purple glass.
    for x in (3, 5):
        for z in (0, 6):
            put(x, 2, z, "purple_stained_glass_pane", east=True, west=True, north=False, south=False, waterlogged=False)
    # The door, and the doorstep where the street meets it.
    put(1, 1, 3, p["door"], facing="east", half="lower", hinge="left", open=False, powered=False)
    put(1, 2, 3, p["door"], facing="east", half="upper", hinge="left", open=False, powered=False)
    put(0, 0, 3, "jigsaw", orientation="west_up", nbt={
        "id": "minecraft:jigsaw", "name": "minecraft:building_entrance", "target": "minecraft:building_entrance",
        "pool": f"minecraft:village/{village}/streets", "joint": "aligned",
        "final_state": f"minecraft:{p['doorstep']}[facing=east,half=bottom,shape=straight,waterlogged=false]",
        "placement_priority": 0, "selection_priority": 0})
    # Someone lives here (the village's villager pool): they'll take the lectern.
    put(3, 0, 3, "jigsaw", orientation="up_north", nbt={
        "id": "minecraft:jigsaw", "name": "minecraft:bottom", "target": "minecraft:bottom",
        "pool": f"minecraft:village/{village}/villagers", "joint": "rollable",
        "final_state": f"minecraft:{p['floor']}", "placement_priority": 0, "selection_priority": 0})

    # Inside: a wall of books behind the lectern, a bed, a chest, a potted allium and a crafting
    # table. Nothing here is another profession's job site, so whoever lives here becomes the Arcanist.
    for z in range(1, 6):
        for y in (1, 2):
            put(6, y, z, "bookshelf")
    put(6, 3, 1, "amethyst_cluster", facing="up", waterlogged=False)
    put(6, 3, 5, "amethyst_cluster", facing="up", waterlogged=False)
    put(5, 1, 3, "elementalarcana:arcane_lectern", facing="west")
    put(4, 1, 3, "purple_carpet")
    put(2, 1, 1, "purple_bed", facing="west", occupied=False, part="head")
    put(3, 1, 1, "purple_bed", facing="west", occupied=False, part="foot")
    put(2, 1, 5, "chest", nbt={"id": "minecraft:chest", "LootTable": "elementalarcana:chests/arcanist_cottage"},
        facing="east", type="single", waterlogged=False)
    put(3, 1, 5, "potted_allium")
    put(4, 1, 5, "crafting_table")
    put(4, 3, 3, "lantern", hanging=True, waterlogged=False)

    # Ceiling, then a gabled roof running east to west.
    for x in range(1, 8):
        put(x, 4, 0, p["stairs"], facing="south", half="bottom", shape="straight", waterlogged=False)
        put(x, 4, 6, p["stairs"], facing="north", half="bottom", shape="straight", waterlogged=False)
        for z in range(1, 6):
            put(x, 4, z, p["roof"])
        put(x, 5, 1, p["stairs"], facing="south", half="bottom", shape="straight", waterlogged=False)
        put(x, 5, 5, p["stairs"], facing="north", half="bottom", shape="straight", waterlogged=False)
        put(x, 6, 2, p["stairs"], facing="south", half="bottom", shape="straight", waterlogged=False)
        put(x, 6, 4, p["stairs"], facing="north", half="bottom", shape="straight", waterlogged=False)
        put(x, 6, 3, p["roof"])
    for x in (1, 7):
        for z in (2, 3, 4):
            put(x, 5, z, p["planks"])
    write_structure(STRUCTURES / village / "arcanist_cottage.nbt", (8, 7, 7), blocks)


def main():
    arcanist_clothes()
    lectern_textures()
    items()
    for village, palette in VILLAGES.items():
        cottage(village, palette)


if __name__ == "__main__":
    main()
