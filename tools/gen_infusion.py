#!/usr/bin/env python3
"""Generates Arcane Infusion's art and data (docs/superpowers/specs/2026-10-04-arcane-infusion-design.md):

- the Infusion Altar's textures (its side, laid out so each part of the model takes its own band:
  the basin's gold rim, the carved column, the base; its top, the basin; its bottom), its model
  (a base, a column, a basin, amethyst at the corners), blockstate, item model and loot table;
- its recipe, and the tags: what can be infused (elementalarcana:infusable) and mining it with a
  pickaxe.

Run from the project root:  python3 tools/gen_infusion.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

STONE = (44, 40, 52)
STONE_DARK = (30, 27, 36)
STONE_LIGHT = (62, 57, 72)
GOLD = (222, 176, 62)
GOLD_DARK = (160, 116, 34)
AMETHYST = (168, 112, 222)
AMETHYST_LIGHT = (214, 172, 250)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def stone(img, rng, rows, cols=range(16)):
    for y in rows:
        for x in cols:
            roll = rng.random()
            color = STONE_DARK if roll < 0.25 else STONE_LIGHT if roll > 0.88 else STONE
            img[y, x, :3] = np.clip(np.array(color, dtype=float) + (rng.random() - 0.5) * 8, 0, 255)
            img[y, x, 3] = 255


def side():
    rng = np.random.default_rng(6100)
    img = np.zeros((16, 16, 4))
    stone(img, rng, range(16))
    # Rows 3-5: the basin's side, a gold rim above and below dark stone.
    img[3, :, :3] = GOLD
    img[5, :, :3] = GOLD_DARK
    # Rows 6-12: the column, carved with an amethyst rune in its middle.
    rune = ["..a..", ".a.a.", "a.A.a", ".a.a.", "..a.."]
    for dy, row in enumerate(rune):
        for dx, ch in enumerate(row):
            if ch != ".":
                img[7 + dy, 6 + dx, :3] = AMETHYST_LIGHT if ch == "A" else AMETHYST
    for y in range(6, 13):
        img[y, 3, :3] = STONE_DARK
        img[y, 12, :3] = STONE_DARK
    # Rows 13-15: the base, a lighter edge on top.
    img[13, :, :3] = STONE_LIGHT
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def top():
    """The basin seen from above: a gold rim round a dark hollow with a faint amethyst ring."""
    rng = np.random.default_rng(6101)
    img = np.zeros((16, 16, 4))
    stone(img, rng, range(16))
    for y in range(16):
        for x in range(16):
            edge = min(x - 1, 14 - x, y - 1, 14 - y)
            if edge == 0:
                img[y, x, :3] = GOLD if (x + y) % 3 else GOLD_DARK
            elif edge >= 1:
                img[y, x, :3] = np.array(STONE_DARK, dtype=float) * (0.8 if edge >= 3 else 1.0)
            r = np.hypot(x - 7.5, y - 7.5)
            if 3.6 < r < 4.6:
                img[y, x, :3] = AMETHYST
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def bottom():
    rng = np.random.default_rng(6102)
    img = np.zeros((16, 16, 4))
    stone(img, rng, range(16))
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def box(lo, hi, textures, cull=()):
    faces = {}
    for side_name in ("north", "east", "south", "west", "up", "down"):
        texture = textures.get(side_name, textures.get("side"))
        face = {"texture": texture}
        if side_name in cull:
            face["cullface"] = side_name
        faces[side_name] = face
    return {"from": lo, "to": hi, "faces": faces}


def model():
    stone_faces = {"side": "#side", "up": "#top", "down": "#bottom"}
    elements = [
        box([0, 0, 0], [16, 3, 16], stone_faces, cull=("down", "north", "east", "south", "west")),
        box([3, 3, 3], [13, 10, 13], {"side": "#side", "up": "#bottom", "down": "#bottom"}),
        box([1, 10, 1], [15, 13, 15], stone_faces),
    ]
    for x, z in ((1, 1), (13, 1), (1, 13), (13, 13)):
        elements.append(box([x, 3, z], [x + 2, 7, z + 2], {"side": "#amethyst"}))
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": f"{NS}block/infusion_altar_side", "side": f"{NS}block/infusion_altar_side",
                         "top": f"{NS}block/infusion_altar_top", "bottom": f"{NS}block/infusion_altar_bottom",
                         "amethyst": "minecraft:block/amethyst_block"},
            "elements": elements}


ROBES = [f"{NS}{tier}_{piece}" for tier in ("apprentice", "adept", "master", "archmage")
         for piece in ("hood", "robe", "trousers", "boots")]


def main():
    blocks = ASSETS / "textures/block"
    side().save(blocks / "infusion_altar_side.png")
    top().save(blocks / "infusion_altar_top.png")
    bottom().save(blocks / "infusion_altar_bottom.png")
    write_json(ASSETS / "models/block/infusion_altar.json", model())
    write_json(ASSETS / "blockstates/infusion_altar.json", {"variants": {"": {"model": f"{NS}block/infusion_altar"}}})
    write_json(ASSETS / "models/item/infusion_altar.json", {"parent": f"{NS}block/infusion_altar"})
    write_json(DATA / "loot_table/blocks/infusion_altar.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{NS}infusion_altar"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(DATA / "recipe/infusion_altar.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": ["AEA", "GOG", "SSS"],
        "key": {"A": {"item": "minecraft:amethyst_shard"}, "E": {"tag": f"{NS}essences"}, "G": {"item": "minecraft:gold_ingot"},
                "O": {"item": "minecraft:obsidian"}, "S": {"item": "minecraft:smooth_stone"}},
        "result": {"id": f"{NS}infusion_altar", "count": 1}})
    write_json(DATA / "tags/item/infusable.json", {"values": [
        "#minecraft:enchantable/weapon", "#minecraft:enchantable/bow", "#minecraft:enchantable/crossbow",
        "#minecraft:enchantable/trident", "#minecraft:enchantable/mace", "#minecraft:enchantable/armor"] + ROBES})
    pickaxe = DATA.parent / "minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads(pickaxe.read_text()) if pickaxe.exists() else {"replace": False, "values": []}
    if f"{NS}infusion_altar" not in tag["values"]:
        tag["values"].append(f"{NS}infusion_altar")
    write_json(pickaxe, tag)
    print("infusion art and data written")


if __name__ == "__main__":
    main()
