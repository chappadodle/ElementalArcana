#!/usr/bin/env python3
"""Generates the Hollowed's art and data (docs/superpowers/specs/2026-10-06-the-hollowed-design.md):

- their skins (64x64, the vanilla illager model's layout): the Acolyte hooded in black and violet,
  the Devourer bare-headed in a ragged tunic, the Herald in a mantle trimmed with gold; ash-grey skin
  veined with violet, and the eyes, glowing (a layer of their own);
- the Hollow Shard, the Hungerward Charm and the Hunger Orb (what a Hunger Bolt is drawn as);
- the Hunger Obelisk: its art, model, loot (two shards) and the pickaxe it needs;
- their loot, the camp's chest, the letters, the Hungerward's recipe;
- the camp's structure, spread and biomes; the hunger they deal (a damage type) and their tag.

Run from the project root:  python3 tools/gen_hollowed.py
"""
import json
from pathlib import Path

from PIL import Image

from gen_towers import Canvas, faces

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

SKIN = (122, 112, 128)
SKIN_DARK = (92, 82, 100)
VEIN = (120, 70, 168)
EYE = (214, 150, 255)
BOOT = (22, 18, 26)

# robe, robe shadow, trim, sash
KINDS = {
    "acolyte": ((36, 30, 42), (22, 18, 28), (118, 62, 178), (160, 96, 230)),
    "devourer": ((52, 44, 58), (32, 26, 38), (84, 42, 120), (110, 56, 150)),
    "herald": ((22, 18, 28), (12, 10, 16), (214, 176, 82), (168, 100, 240)),
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


# ---- the skins ----

def head(c, veined):
    hd = faces(0, 0, 8, 10, 8)
    for rect in hd.values():
        c.fill(rect, SKIN, SKIN_DARK, 0.14)
    x, y, w, h = hd["front"]
    c.px[y + 3, x + 1:x + 7] = (*SKIN_DARK, 255)  # a heavy brow
    for ex in (x + 1, x + 2, x + 5, x + 6):
        c.px[y + 4, ex] = (40, 20, 56, 255)  # the eyes' sockets; the eyes themselves glow (eyes.png)
    c.px[y + 7, x + 2:x + 6] = (*SKIN_DARK, 255)  # a thin mouth
    if veined:
        # Violet veins climbing the cheeks.
        for vx, vy in ((x + 1, y + 6), (x + 1, y + 7), (x + 2, y + 8), (x + 6, y + 5), (x + 6, y + 6), (x + 5, y + 7)):
            c.px[vy, vx] = (*VEIN, 255)
    nose = faces(24, 0, 2, 4, 2)
    for rect in nose.values():
        c.fill(rect, SKIN, SKIN_DARK, 0.3)


def hood(c, robe, dark, trim):
    hd = faces(32, 0, 8, 12, 8)
    for rect in hd.values():
        c.fill(rect, robe, dark, 0.2)
    x, y, w, h = hd["front"]
    c.clear((x + 1, y + 2, w - 2, h - 2))  # the face shows through
    c.px[y + 1, x:x + w] = (*trim, 255)
    for name in ("right", "left", "back"):
        c.row(hd[name], hd[name][3] - 1, trim)


def robe(c, robe_color, dark, trim, sash, kind):
    body = faces(16, 20, 8, 12, 6)
    for rect in body.values():
        c.fill(rect, robe_color, dark, 0.18)
    jacket = faces(0, 38, 8, 20, 6)
    for name, rect in jacket.items():
        c.fill(rect, robe_color, dark, 0.2 if kind == "devourer" else 0.16)
        if name in ("right", "front", "left", "back"):
            c.row(rect, 7, sash)
            if kind == "devourer":
                # A ragged hem: every other pixel torn away.
                x, y, w, h = rect
                for px in range(0, w, 2):
                    c.px[y + h - 1, x + px] = 0
            else:
                c.row(rect, rect[3] - 1, trim)
    if kind != "devourer":
        c.col(jacket["front"], 3, trim, start=8)
        c.col(jacket["front"], 4, trim, start=8)
    if kind == "herald":
        # A gold-trimmed mantle over the shoulders, and the Hollow's eye on the chest.
        for name in ("right", "front", "left", "back"):
            c.row(jacket[name], 0, trim)
            c.row(jacket[name], 1, dark)
            c.row(jacket[name], jacket[name][3] - 2, trim)
        c.fill(jacket["top"], trim, dark, 0.3)
        x, y, w, h = jacket["front"]
        for ex, ey in ((x + 3, y + 3), (x + 4, y + 3), (x + 2, y + 4), (x + 5, y + 4), (x + 3, y + 5), (x + 4, y + 5)):
            c.px[ey, ex] = (*sash, 255)
        c.px[y + 4, x + 3] = (24, 8, 36, 255)
        c.px[y + 4, x + 4] = (24, 8, 36, 255)


def legs(c, dark):
    leg = faces(0, 22, 4, 12, 4)
    for name, rect in leg.items():
        c.fill(rect, dark, BOOT, 0.2)
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 2, BOOT)
            c.row(rect, rect[3] - 1, BOOT)


def arms(c, robe_color, dark, trim, bare):
    """Crossed arms (sleeves, the hands in the middle box) and free arms (casting or clawing). A
    Devourer's are bare, ash-grey and veined, its hands long-clawed."""
    sleeve, sleeve_dark = (SKIN, SKIN_DARK) if bare else (robe_color, dark)
    side = faces(44, 22, 4, 8, 4)
    for name, rect in side.items():
        c.fill(rect, sleeve, sleeve_dark, 0.18)
        if name in ("right", "front", "left", "back") and not bare:
            c.row(rect, rect[3] - 1, trim)
    middle = faces(40, 38, 8, 4, 4)
    for rect in middle.values():
        c.fill(rect, sleeve, sleeve_dark, 0.18)
    for name in ("front", "top", "bottom"):
        x, y, w, h = middle[name]
        c.fill((x + 2, y, w - 4, h), SKIN, SKIN_DARK, 0.2)
    arm = faces(40, 46, 4, 12, 4)
    for name, rect in arm.items():
        c.fill(rect, sleeve, sleeve_dark, 0.18)
        if name in ("right", "front", "left", "back"):
            x, y, w, h = rect
            if bare:
                c.col(rect, 1, VEIN, start=2)
                c.row(rect, h - 1, (30, 20, 36))  # claws
            else:
                c.row(rect, h - 4, trim)
                c.fill((x, y + h - 3, w, 3), SKIN, SKIN_DARK, 0.2)
    c.fill(arm["bottom"], (30, 20, 36) if bare else SKIN, SKIN_DARK, 0.2)


def skin(kind, seed):
    robe_color, dark, trim, sash = KINDS[kind]
    c = Canvas(seed)
    head(c, veined=kind != "acolyte")
    if kind != "devourer":
        hood(c, robe_color, dark, trim)
    robe(c, robe_color, dark, trim, sash, kind)
    legs(c, dark)
    arms(c, robe_color, dark, trim, bare=kind == "devourer")
    return Image.fromarray(c.px, "RGBA")


def eyes():
    """Their eyes alone, glowing (drawn over the skin, full bright)."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    x, y = 8, 8  # the head's front face
    for ex in (x + 1, x + 2, x + 5, x + 6):
        img.putpixel((ex, y + 4), (*EYE, 255))
    return img


# ---- items and the obelisk (16x16, a character grid each) ----

SPRITES = {
    # A sliver of the Hollow's dark: a black shard with a violet heart.
    "item/hollow_shard": ({"k": (16, 10, 24), "d": (48, 24, 72), "v": (110, 58, 170), "l": (176, 120, 236), "w": (236, 214, 255)}, [
        "................",
        "...........kk...",
        "..........kwlk..",
        ".........kwvdk..",
        "........klvddk..",
        ".......klvdk....",
        "......klvddk....",
        ".....klvddk.....",
        "....klvddk......",
        "...kdvddk.......",
        "..kddddk........",
        "..kdddk.........",
        "...kdk..........",
        "....k...........",
        "................",
        "................",
    ]),
    # A Hungerward Charm: a dark shard set in a gold ring, a white ward-mark over it, on a cord.
    "item/hungerward_charm": ({"k": (40, 26, 16), "c": (138, 106, 58), "g": (232, 184, 64), "y": (255, 224, 138), "d": (168, 122, 32),
                               "s": (24, 14, 34), "v": (120, 64, 180), "w": (246, 240, 255)}, [
        "................",
        "......kkkk......",
        ".....kcccck.....",
        "....kc....ck....",
        "....kc....ck....",
        ".....kc..ck.....",
        "......kggk......",
        ".....kgyygk.....",
        "....kgdssdgk....",
        "...kgsvwvssgk...",
        "...kgswwwvsgk...",
        "...kgsvwvssgk...",
        "....kgssssgk....",
        ".....kgddgk.....",
        "......kkkk......",
        "................",
    ]),
    # A Hunger Orb: a black core in a ring of violet light.
    "item/hunger_orb": ({"k": (8, 4, 12), "d": (52, 22, 80), "v": (128, 64, 200), "l": (196, 140, 255), "w": (240, 220, 255)}, [
        "................",
        "................",
        "......llll......",
        "....llvvvvll....",
        "...lvvddddvvl...",
        "...lvdkkkkdvl...",
        "..lvdkkkkkkdvl..",
        "..lvdkkkkkkdvl..",
        "..lvdkkkkkkdvl..",
        "..lvdkkkkkkdvl..",
        "...lvdkkkkdvl...",
        "...lvvddddvvw...",
        "....llvvvvll....",
        "......llll......",
        "................",
        "................",
    ]),
    # The obelisk's sides: polished dark stone, a column of runes glowing violet.
    "block/hunger_obelisk_side": ({"k": (24, 20, 30), "s": (44, 38, 52), "S": (58, 50, 68), "v": (150, 84, 220), "l": (206, 154, 255)}, [
        "kkkkkkkkkkkkkkkk",
        "ksSsssssssssSssk",
        "kssssssllssssssk",
        "ksssssvssvsssssk",
        "kssssssvvssssssk",
        "ksSsssssssssssSk",
        "ksssssvllvsssssk",
        "kssssssvvssssssk",
        "ksssssvssvsssssk",
        "kSsssssllsssssSk",
        "kssssssssssssssk",
        "kssssslvvlsssssk",
        "ksssssvssvsssssk",
        "kssssssllssssssk",
        "ksSsssssssssSssk",
        "kkkkkkkkkkkkkkkk",
    ]),
    # Its top: the same stone, an eye of violet light looking up.
    "block/hunger_obelisk_top": ({"k": (24, 20, 30), "s": (44, 38, 52), "S": (58, 50, 68), "v": (150, 84, 220), "l": (206, 154, 255),
                                  "d": (10, 4, 16)}, [
        "kkkkkkkkkkkkkkkk",
        "ksSssssssssssSsk",
        "kssssssssssssssk",
        "ksssssvvvvsssssk",
        "ksssvvllllvvsssk",
        "kssvllvvvvllvssk",
        "kssvlvddddvlvssk",
        "ksvlvddddddvlvsk",
        "ksvlvddddddvlvsk",
        "kssvlvddddvlvssk",
        "kssvllvvvvllvssk",
        "ksssvvllllvvsssk",
        "ksssssvvvvsssssk",
        "kssssssssssssssk",
        "ksSssssssssssSsk",
        "kkkkkkkkkkkkkkkk",
    ]),
}


def render(palette, grid):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), (*palette[ch], 255))
    return img


# ---- the letters ----

LETTERS = {
    "herald_orders": ("Orders", "the Herald", 2),
    "acolyte_diary": ("A Diary, Unfinished", "an Acolyte", 2),
    "thin_places": ("On the Thin Places", "the Herald", 2),
}


def letter(name):
    title, author, pages = LETTERS[name]
    return {"type": "minecraft:item", "name": "minecraft:written_book", "weight": 1, "functions": [
        {"function": "minecraft:set_book_cover", "title": title, "author": author},
        {"function": "minecraft:set_written_book_pages", "mode": "replace_all",
         "pages": [{"translate": f"lore.elementalarcana.{name}.{i}"} for i in range(1, pages + 1)]},
        {"function": "minecraft:set_custom_data", "tag": f"{{elementalarcana_lore:1b,elementalarcana_page:\"{name}\"}}"}]}


def counted(item, low, high, weight=1):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if (low, high) != (1, 1):
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def shard_drop(chance):
    """A Hollow Shard, now and then, for a player's kill (Looting adds to the chance)."""
    return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "hollow_shard"}], "conditions": [
        {"condition": "minecraft:killed_by_player"},
        {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
         "unenchanted_chance": chance, "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.1, "per_level_above_first": 0.1}}]}


def loot():
    tables = DATA / "loot_table"
    write_json(tables / "entities/hollowed_acolyte.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [counted("minecraft:bone", 0, 2)]}, shard_drop(0.33)]})
    write_json(tables / "entities/hollowed_devourer.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [counted("minecraft:rotten_flesh", 0, 2)]}, shard_drop(0.5)]})
    write_json(tables / "entities/hollow_herald.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [counted(NS + "hollow_shard", 3, 5)]},
        {"rolls": 1, "entries": [letter(name) for name in LETTERS]},
        {"rolls": 1, "entries": [counted("minecraft:ender_pearl", 1, 2)]}]})
    write_json(tables / "blocks/hunger_obelisk.json", {"type": "minecraft:block", "random_sequence": NS + "blocks/hunger_obelisk",
                                                      "pools": [{"rolls": 1, "entries": [counted(NS + "hollow_shard", 2, 2)],
                                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    essences = [counted(f"{NS}{element}_essence", 1, 2, 2) for element in ("fire", "water", "wind", "earth", "ice", "crystal", "lightning",
                                                                             "radiance")]
    write_json(tables / "chests/hollowed_camp.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [counted(NS + "hollow_shard", 2, 4)]},
        {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 1}] + [letter(name) for name in LETTERS]},
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            counted("minecraft:gold_ingot", 2, 5, 8), counted("minecraft:ender_pearl", 1, 2, 5), counted("minecraft:emerald", 1, 4, 6),
            counted("minecraft:amethyst_shard", 1, 3, 5), counted("minecraft:black_candle", 1, 3, 4),
            counted("minecraft:crying_obsidian", 1, 2, 3)] + essences},
        {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 9}, counted(NS + "hungerward_charm", 1, 1, 1)]}]})


def merge_tag(path, values):
    tag = json.loads(path.read_text()) if path.exists() else {"replace": False, "values": []}
    for value in values:
        if value not in tag["values"]:
            tag["values"].append(value)
    write_json(path, tag)


def main():
    textures = ASSETS / "textures"
    for i, kind in enumerate(KINDS):
        save(skin(kind, 2900 + i), textures / f"entity/hollowed/{kind}.png")
    save(eyes(), textures / "entity/hollowed/eyes.png")
    for path, sprite in SPRITES.items():
        save(render(*sprite), textures / f"{path}.png")
    models = ASSETS / "models"
    for item in ("hollow_shard", "hungerward_charm", "hunger_orb"):
        write_json(models / f"item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{item}"}})
    for egg in ("hollowed_acolyte_spawn_egg", "hollowed_devourer_spawn_egg", "hollow_herald_spawn_egg"):
        write_json(models / f"item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})
    write_json(models / "block/hunger_obelisk.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": f"{NS}block/hunger_obelisk_top", "side": f"{NS}block/hunger_obelisk_side"}})
    write_json(models / "item/hunger_obelisk.json", {"parent": f"{NS}block/hunger_obelisk"})
    write_json(ASSETS / "blockstates/hunger_obelisk.json", {"variants": {"": {"model": f"{NS}block/hunger_obelisk"}}})
    loot()
    write_json(DATA / "recipe/hungerward_charm.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["GSG", "SAS", "GSG"],
        "key": {"G": {"item": "minecraft:gold_ingot"}, "S": {"item": NS + "hollow_shard"}, "A": {"item": "minecraft:amethyst_shard"}},
        "result": {"id": NS + "hungerward_charm", "count": 1}})
    write_json(DATA / "damage_type/hollowed.json", {"exhaustion": 0.0, "message_id": "elementalarcana.hollowed", "scaling": "never"})
    minecraft_tags = DATA.parent / "minecraft/tags"
    merge_tag(minecraft_tags / "damage_type/bypasses_armor.json", [NS + "hollowed"])
    merge_tag(minecraft_tags / "block/mineable/pickaxe.json", [NS + "hunger_obelisk"])
    merge_tag(minecraft_tags / "block/needs_diamond_tool.json", [NS + "hunger_obelisk"])
    write_json(DATA / "tags/entity_type/hollowed.json", {"values": [NS + "hollowed_acolyte", NS + "hollowed_devourer", NS + "hollow_herald"]})
    write_json(DATA / "worldgen/structure/hollowed_camp.json", {
        "type": NS + "hollowed_camp", "biomes": f"#{NS}has_structure/hollowed_camps", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "beard_thin"})
    write_json(DATA / "worldgen/structure_set/hollowed_camps.json", {
        "structures": [{"structure": NS + "hollowed_camp", "weight": 1}],
        # Kept clear of villages and the shrines (SpreadAwayPlacement): a camp is pitched away from them.
        "placement": {"type": NS + "spread_away", "spacing": 36, "separation": 12, "salt": 815243,
                      "keep_away": {"sets": ["minecraft:villages", NS + "shrines"], "chunk_count": 4}}})
    write_json(DATA / "tags/worldgen/biome/has_structure/hollowed_camps.json", {"values": [
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:birch_forest", "minecraft:taiga",
        "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:savanna", "minecraft:meadow",
        "minecraft:snowy_plains", "minecraft:snowy_taiga"]})
    print("hollowed art and data written")


if __name__ == "__main__":
    main()
