#!/usr/bin/env python3
"""Generates the Ember Reaches, part 1 (docs/superpowers/specs/2026-10-06-cinder-forges-design.md):

- the Forgewarden's skin (an Elemental Golem's layout, gen_creatures), in blackstone and gilded
  stone with magma seams; its glow, and its molten glow (the seams cracked wide);
- the Ember Core's and the Forgefire Charm's icons and models, and the spawn egg's model;
- the Forge Heart's model (polished blackstone to look at);
- the Cinder Forge's structure (in the nether wastes), its spread (rare, kept clear of fortresses and
  bastions), its storerooms' chest and the Forgewarden's loot;
- the Forgefire Charm's recipe, and the Ember Core as furnace fuel (as long as a bucket of lava).

Run from the project root:  python3 tools/gen_forge.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

import gen_creatures as creatures

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NEOFORGE = ROOT / "src/main/resources/data/neoforge"
NS = "elementalarcana:"

# Blackstone, gilded here and there, magma in the seams: (light, mid, dark, accent, accent share, glow, core).
WARDEN = ((62, 54, 64), (42, 36, 44), (26, 22, 28), (196, 150, 52), 0.06, (255, 112, 28), (255, 226, 140))

ITEM_PALETTE = {
    ".": None,
    "O": (24, 18, 20), "d": (52, 42, 48), "m": (150, 52, 18), "M": (238, 108, 30), "Y": (255, 196, 72), "W": (255, 246, 214),
    "c": (110, 110, 120), "g": (150, 104, 30), "G": (226, 186, 74), "R": (214, 70, 24),
}
EMBER_CORE = [
    "................",
    "................",
    ".....OOOOOO.....",
    "....OddddddO....",
    "...OdmMMMmmdO...",
    "..OdmMYYYMmmdO..",
    "..OdMYWWYYMmdO..",
    "..OdMYWYYYMmdO..",
    "..OdMYYYYMmmdO..",
    "..OdmMYYMmmddO..",
    "...OdmMMmmddO...",
    "....OddddddO....",
    ".....OOOOOO.....",
    "................",
    "................",
    "................",
]
FORGEFIRE_CHARM = [
    "......cccc......",
    ".....c....c.....",
    "....c......c....",
    "....c......c....",
    ".....c....c.....",
    "......gGGg......",
    ".....gGRRGg.....",
    "....gGRYYRGg....",
    "....gGRYWRGg....",
    "....gGRRYRGg....",
    ".....gGRRGg.....",
    "......gGGg......",
    "................",
    "................",
    "................",
    "................",
]


def pixel_art(rows):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            color = ITEM_PALETTE[key]
            if color is not None:
                image.putpixel((x, y), color + (255,))
    return image


def forgewarden(seed):
    """Its skin and glow (the golem's, in the warden's stone, more of it cracked), and the molten glow: the seams cracked wide."""
    rng = np.random.default_rng(seed)
    img = np.zeros((64, 128, 4), dtype=np.uint8)
    glow = np.zeros((64, 128, 4), dtype=np.uint8)
    glow[..., 3] = 255
    for part, (u, v, w, h, d) in creatures.GOLEM_BOXES.items():
        for rect in creatures.faces(u, v, w, h, d):
            creatures.paint_golem_face(img, glow, rect, rng, WARDEN, 0.9 if part in ("body", "arm") else 0.6)
    core, hot = WARDEN[6], WARDEN[5]
    for rect in creatures.faces(*creatures.GOLEM_CORE):
        x, y, w, h = rect
        img[y:y + h, x:x + w] = (*hot, 255)
        glow[y:y + h, x:x + w] = (*hot, 255)
    fx, fy = creatures.GOLEM_CORE[0] + creatures.GOLEM_CORE[4], creatures.GOLEM_CORE[1] + creatures.GOLEM_CORE[4]
    img[fy + 1:fy + 3, fx + 1:fx + 3] = (*core, 255)
    glow[fy + 1:fy + 3, fx + 1:fx + 3] = (*core, 255)
    hx, hy = creatures.GOLEM_BOXES["head"][0] + creatures.GOLEM_BOXES["head"][4], creatures.GOLEM_BOXES["head"][1] + creatures.GOLEM_BOXES["head"][4]
    for ex in (hx + 1, hx + 5):
        img[hy + 3, ex:ex + 2] = (*core, 255)
        glow[hy + 3, ex:ex + 2] = (*core, 255)
    # Molten: every face crazed with more seams, the old ones widened.
    molten = glow.copy()
    lit = (glow[..., 0] > 0)
    for y, x in zip(*np.nonzero(lit)):
        for dy, dx in ((0, 1), (1, 0)):
            ny, nx = y + dy, x + dx
            if ny < 64 and nx < 128 and img[ny, nx, 3] > 0 and rng.random() < 0.6:
                molten[ny, nx] = (*hot, 255)
    for part, (u, v, w, h, d) in creatures.GOLEM_BOXES.items():
        for (x, y, w2, h2) in creatures.faces(u, v, w, h, d):
            if w2 < 3 or h2 < 3:
                continue
            for _ in range(2):
                cx, cy = int(rng.integers(0, w2)), int(rng.integers(0, h2))
                for _ in range(int(rng.integers(3, 3 + max(w2, h2) // 2))):
                    molten[y + cy, x + cx] = (*hot, 255)
                    cx = max(0, min(w2 - 1, cx + int(rng.integers(-1, 2))))
                    cy = max(0, min(h2 - 1, cy + int(rng.integers(0, 2))))
    return Image.fromarray(img, "RGBA"), Image.fromarray(glow, "RGBA"), Image.fromarray(molten, "RGBA")


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def counted(item, low, high, weight=1):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if (low, high) != (1, 1):
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def main():
    textures = ASSETS / "textures/entity/forgewarden"
    textures.mkdir(parents=True, exist_ok=True)
    skin, glow, molten = forgewarden(4100)
    skin.save(textures / "forgewarden.png")
    glow.save(textures / "forgewarden_glow.png")
    molten.save(textures / "forgewarden_molten.png")
    models = ASSETS / "models"
    for name, rows in (("ember_core", EMBER_CORE), ("forgefire_charm", FORGEFIRE_CHARM)):
        pixel_art(rows).save(ASSETS / f"textures/item/{name}.png")
        write_json(models / f"item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    write_json(models / "item/forgewarden_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    write_json(models / "block/forge_heart.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "minecraft:block/polished_blackstone"}})
    write_json(ASSETS / "blockstates/forge_heart.json", {"variants": {"": {"model": f"{NS}block/forge_heart"}}})

    write_json(DATA / "worldgen/structure/cinder_forge.json", {
        "type": NS + "cinder_forge", "biomes": f"#{NS}has_structure/cinder_forges", "step": "top_layer_modification",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/cinder_forges.json", {
        "structures": [{"structure": NS + "cinder_forge", "weight": 1}],
        "placement": {"type": NS + "spread_away", "spacing": 56, "separation": 20, "salt": 731905,
                      "keep_away": {"sets": ["minecraft:nether_complexes"], "chunk_count": 4}}})
    write_json(DATA / "tags/worldgen/biome/has_structure/cinder_forges.json", {"values": ["minecraft:nether_wastes"]})
    write_json(DATA / "loot_table/chests/cinder_forge.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            counted("minecraft:gold_ingot", 2, 6, 10), counted("minecraft:iron_ingot", 2, 6, 8), counted("minecraft:gold_nugget", 4, 12, 8),
            counted("minecraft:blaze_powder", 1, 4, 6), counted("minecraft:magma_cream", 1, 3, 5), counted(NS + "fire_essence", 1, 3, 6),
            counted("minecraft:obsidian", 1, 3, 4), counted("minecraft:netherite_scrap", 1, 1, 1),
            {"type": "minecraft:item", "name": "minecraft:book", "weight": 3, "functions": [{"function": "minecraft:enchant_randomly"}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 4}, counted(NS + "tome_of_insight", 1, 1, 1)]}]})
    # The Ember Cores are its own to drop (ForgewardenEntity); the rest is the loot table's.
    write_json(DATA / "loot_table/entities/forgewarden.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [counted(NS + "fire_essence", 4, 8)]},
        {"rolls": 1, "entries": [counted("minecraft:gold_ingot", 4, 10)]},
        {"rolls": 1, "entries": [counted("minecraft:magma_cream", 2, 5)]},
        {"rolls": 1, "entries": [counted("minecraft:blaze_rod", 1, 3)]}]})
    write_json(DATA / "recipe/forgefire_charm.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" C ", "GEG", " B "],
        "key": {"C": {"item": "minecraft:chain"}, "G": {"item": "minecraft:gold_ingot"}, "E": {"item": NS + "ember_core"},
                "B": {"item": "minecraft:blaze_powder"}},
        "result": {"id": NS + "forgefire_charm", "count": 1}})
    write_json(NEOFORGE / "data_maps/item/furnace_fuels.json", {"values": {NS + "ember_core": {"burn_time": 20000}}})
    print("forge art and data written")


if __name__ == "__main__":
    main()
