#!/usr/bin/env python3
"""Generates the Sky Isles' data (docs/superpowers/specs/2026-10-05-sky-isles-design.md): the structure
(over any Overworld biome; its height is its own), its spread (about one every 750 blocks) and its
chest's sky loot. The isle itself is code-built (content/world/SkyIslePiece).

Run from the project root:  python3 tools/gen_sky_isles.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def entry(name, weight, low=None, high=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if low is not None:
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    return e


def main():
    write_json(DATA / "worldgen/structure/sky_isle.json", {
        "type": f"{NS}sky_isle", "biomes": f"#{NS}has_structure/sky_isles", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/sky_isles.json", {
        "structures": [{"structure": f"{NS}sky_isle", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 48, "separation": 20, "salt": 912611}})
    write_json(DATA / "tags/worldgen/biome/has_structure/sky_isles.json", {"values": ["#minecraft:is_overworld"]})
    write_json(DATA / "loot_table/chests/sky_isle.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
            entry(f"{NS}wind_essence", 8, 2, 4),
            entry("minecraft:feather", 8, 2, 6),
            entry("minecraft:golden_carrot", 5, 1, 3),
            entry("minecraft:golden_apple", 3, 1, 1),
            entry("minecraft:emerald", 6, 2, 5),
            entry("minecraft:phantom_membrane", 3, 1, 2),
            entry("minecraft:diamond", 2, 1, 1),
            entry("minecraft:amethyst_shard", 4, 2, 5),
        ]},
        {"rolls": 1, "entries": [entry(f"{NS}feather_of_the_gale", 8), entry("minecraft:elytra", 1), {"type": "minecraft:empty", "weight": 91}]},
    ]})
    print("sky isle data written")


if __name__ == "__main__":
    main()
