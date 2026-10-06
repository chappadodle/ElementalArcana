#!/usr/bin/env python3
"""Generates the Ley Anchor (docs/superpowers/specs/2026-10-06-ley-anchors-design.md): a standing
stone of grey bricks with a vein of violet runes and an amethyst set on top. Its textures, model,
blockstate, the item's model, its loot (itself, keeping its name) and its recipe; it needs a pickaxe.

Run from the project root:  python3 tools/gen_anchors.py
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data"
NS = "elementalarcana:"

PALETTE = {
    "k": (58, 56, 62), "s": (122, 120, 126), "S": (142, 140, 146), "d": (96, 94, 100),
    "v": (150, 90, 200), "l": (214, 166, 255), "a": (110, 70, 160), "A": (182, 128, 232), "w": (240, 222, 255),
}

# The stone's sides: bricks in courses, a vein of runes down the middle.
SIDE = [
    "kkkkkkkkkkkkkkkk",
    "ksSssdksssSsdssk",
    "kssssdksssssdssk",
    "kkkkkkkvkkkkkkkk",
    "ksssdkslsssdkssk",
    "kSssdksvssSdksSk",
    "kkkkkkkvkkkkkkkk",
    "kssSsdklsSssdssk",
    "ksssssdvsssssdsk",
    "kkkkkkkvkkkkkkkk",
    "ksSsdksvsssdksSk",
    "kssssdklssssdssk",
    "kkkkkkkvkkkkkkkk",
    "ksssSsdssssSsdsk",
    "kssssssdssssssdk",
    "kkkkkkkkkkkkkkkk",
]
# Its top: the brick, around where the amethyst sits.
TOP = [
    "kkkkkkkkkkkkkkkk",
    "ksSssssssssssSsk",
    "kssssssssssssssk",
    "kssddddddddddssk",
    "kssdkkkkkkkkdssk",
    "kssdkvvvvvvkdssk",
    "kssdkvlllvvkdssk",
    "kssdkvlwlvvkdssk",
    "kssdkvlllvvkdssk",
    "kssdkvvvvvvkdssk",
    "kssdkkkkkkkkdssk",
    "kssddddddddddssk",
    "kssssssssssssssk",
    "ksSssssssssssSsk",
    "kssssssssssssssk",
    "kkkkkkkkkkkkkkkk",
]
# The amethyst on top: facets of violet.
CRYSTAL = [
    "aaAAaaAAaaAAaaAA",
    "aAAwaaAAaaAwaaAA",
    "AAwAaaAAaAwAaaAA",
    "AwAAaAAAaAAAaAAa",
    "aAAaaAAAaaAaaAAa",
    "aaAaaAAwaaAaaAaa",
    "aaAAaAwAaaAAaAAa",
    "AaAAaaAAaAAAaAAA",
    "AAaAaaAAaAAaaAwA",
    "AAAaaAAAaaAaAwAA",
    "aAAAaAAwaaAAwAAa",
    "aaAAAAwAaaAAAAaa",
    "aaaAAwAAaAAAaaaa",
    "AaaaAAAaaAAaaaAA",
    "AAaaaAaaaAaaaAAA",
    "AAAaaaaaaaaaAAAA",
]


def grid(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            img.putpixel((x, y), (*PALETTE[ch], 255))
    return img


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main():
    textures = ASSETS / "textures/block"
    textures.mkdir(parents=True, exist_ok=True)
    grid(SIDE).save(textures / "ley_anchor_side.png")
    grid(TOP).save(textures / "ley_anchor_top.png")
    grid(CRYSTAL).save(textures / "ley_anchor_crystal.png")
    faces = lambda texture: {side: {"texture": texture} for side in ("north", "south", "east", "west", "up", "down")}
    write_json(ASSETS / "models/block/ley_anchor.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{NS}block/ley_anchor_side", "side": f"{NS}block/ley_anchor_side",
                     "top": f"{NS}block/ley_anchor_top", "crystal": f"{NS}block/ley_anchor_crystal"},
        "elements": [
            {"from": [3, 0, 3], "to": [13, 13, 13], "faces": {
                **{side: {"texture": "#side"} for side in ("north", "south", "east", "west")},
                "up": {"texture": "#top"}, "down": {"texture": "#top"}}},
            {"from": [5, 13, 5], "to": [11, 16, 11], "faces": faces("#crystal")},
        ]})
    write_json(ASSETS / "models/item/ley_anchor.json", {"parent": f"{NS}block/ley_anchor"})
    write_json(ASSETS / "blockstates/ley_anchor.json", {"variants": {"": {"model": f"{NS}block/ley_anchor"}}})
    write_json(DATA / "elementalarcana/loot_table/blocks/ley_anchor.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}blocks/ley_anchor", "pools": [{
            "rolls": 1, "bonus_rolls": 0, "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": f"{NS}ley_anchor", "functions": [
                {"function": "minecraft:copy_components", "source": "block_entity", "include": ["minecraft:custom_name"]}]}]}]})
    write_json(DATA / "elementalarcana/recipe/ley_anchor.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["BAB", "APA", "BAB"],
        "key": {"B": {"item": "minecraft:stone_bricks"}, "A": {"item": "minecraft:amethyst_shard"}, "P": {"item": "minecraft:ender_pearl"}},
        "result": {"id": f"{NS}ley_anchor", "count": 1}})
    pickaxe = DATA / "minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads(pickaxe.read_text()) if pickaxe.exists() else {"replace": False, "values": []}
    if f"{NS}ley_anchor" not in tag["values"]:
        tag["values"].append(f"{NS}ley_anchor")
    write_json(pickaxe, tag)
    print("ley anchor written")


if __name__ == "__main__":
    main()
