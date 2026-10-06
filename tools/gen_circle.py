#!/usr/bin/env python3
"""Generates the Circle's art and data (docs/superpowers/specs/2026-10-06-the-circle-enclave-design.md):

- the Circle's robes (the villager model's layout, drawn over the villager as a profession's
  clothes are): the Arcanist's cut in white, trimmed with each element's colour, and the
  Archmagister's white and gold;
- the Enclave's structure (on the surface, the land shaped to it), its spread (rare, and kept clear
  of villages, shrines, mage towers and Hollowed camps), its biomes, the compass's tag for it, and
  its library's chest (the compass's tag for it is gen_seeker's);
- the Circle Heart (chiseled quartz to look at) and the spawn eggs' models.

Run from the project root:  python3 tools/gen_circle.py
"""
import json
from pathlib import Path

import numpy as np

import gen_people as people

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

ELEMENTS = {"fire": (255, 122, 31), "water": (63, 156, 255), "ice": (158, 230, 255), "wind": (143, 227, 192), "earth": (181, 137, 90),
            "crystal": (208, 140, 255), "lightning": (255, 225, 77), "radiance": (255, 241, 184)}
WHITE = {people.ROBE: (232, 232, 238), people.ROBE_DARK: (196, 196, 206), people.ROBE_LIGHT: (250, 250, 252)}


def darker(color, share):
    return tuple(int(v * share) for v in color)


def clothes(trim, trim_dark, star, seed):
    """The Arcanist's cut, in white, its gold made {@code trim}."""
    c = people.Canvas(seed=seed)
    people.hat(c)
    people.robe(c)
    people.legs(c)
    people.villager_arms(c)
    colors = {**WHITE, (30, 18, 56): darker(trim_dark, 0.6), people.GOLD: trim, people.GOLD_DARK: trim_dark, people.STAR: star}
    px = c.px
    original = px.copy()
    for old, new in colors.items():
        mask = (original[..., 0] == old[0]) & (original[..., 1] == old[1]) & (original[..., 2] == old[2]) & (original[..., 3] > 0)
        px[mask, :3] = new
    return c.image()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def counted(item, low, high, weight=1):
    entry = {"type": "minecraft:item", "name": item, "weight": weight}
    if (low, high) != (1, 1):
        entry["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return entry


def main():
    textures = ASSETS / "textures/entity/circle"
    textures.mkdir(parents=True, exist_ok=True)
    for i, (element, color) in enumerate(ELEMENTS.items()):
        clothes(color, darker(color, 0.7), (255, 255, 255), 3300 + i).save(textures / f"mage_{element}.png")
    clothes(people.GOLD, people.GOLD_DARK, (255, 246, 200), 3400).save(textures / "archmagister.png")
    models = ASSETS / "models"
    write_json(models / "block/circle_heart.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "minecraft:block/chiseled_quartz_block_top", "side": "minecraft:block/chiseled_quartz_block"}})
    write_json(ASSETS / "blockstates/circle_heart.json", {"variants": {"": {"model": f"{NS}block/circle_heart"}}})
    for egg in ("circle_mage_spawn_egg", "archmagister_spawn_egg"):
        write_json(models / f"item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})
    write_json(DATA / "worldgen/structure/enclave.json", {
        "type": NS + "enclave", "biomes": f"#{NS}has_structure/enclaves", "step": "top_layer_modification",
        "spawn_overrides": {}, "terrain_adaptation": "beard_thin"})
    write_json(DATA / "worldgen/structure_set/enclaves.json", {
        "structures": [{"structure": NS + "enclave", "weight": 1}],
        "placement": {"type": NS + "spread_away", "spacing": 80, "separation": 30, "salt": 905117,
                      "keep_away": {"sets": ["minecraft:villages", NS + "shrines", NS + "mage_towers", NS + "hollowed_camps"],
                                    "chunk_count": 6}}})
    write_json(DATA / "tags/worldgen/biome/has_structure/enclaves.json", {"values": [
        "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:flower_forest"]})
    essences = [counted(f"{NS}{element}_essence", 1, 2, 2) for element in ELEMENTS]
    write_json(DATA / "loot_table/chests/enclave_library.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
            counted("minecraft:book", 1, 3, 10), counted("minecraft:paper", 2, 6, 8), counted("minecraft:ink_sac", 1, 3, 6),
            counted("minecraft:feather", 1, 3, 6), counted("minecraft:amethyst_shard", 1, 3, 5), counted("minecraft:experience_bottle", 1, 3, 5),
            {"type": "minecraft:item", "name": "minecraft:book", "weight": 4,
             "functions": [{"function": "minecraft:enchant_randomly"}]}] + essences},
        {"rolls": 1, "entries": [{"type": "minecraft:empty", "weight": 3}, counted(NS + "tome_of_insight", 1, 1, 1)]}]})
    print("circle art and data written")


if __name__ == "__main__":
    main()
