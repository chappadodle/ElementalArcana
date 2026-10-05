#!/usr/bin/env python3
"""Generates the Wisp Rings' data (docs/superpowers/specs/2026-10-06-wisp-rings-design.md): the
structure (on the surface, in forests, meadows and plains), its spread (about one every 600
blocks) and its biomes. The ring itself is code-built (content/world/WispRingPiece).

Run from the project root:  python3 tools/gen_wisp_rings.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

BIOMES = ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
          "minecraft:dark_forest", "minecraft:meadow", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:cherry_grove"]


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main():
    write_json(DATA / "worldgen/structure/wisp_ring.json", {
        "type": f"{NS}wisp_ring", "biomes": f"#{NS}has_structure/wisp_rings", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/wisp_rings.json", {
        "structures": [{"structure": f"{NS}wisp_ring", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 38, "separation": 14, "salt": 731507}})
    write_json(DATA / "tags/worldgen/biome/has_structure/wisp_rings.json", {"values": BIOMES})
    print("wisp ring data written")


if __name__ == "__main__":
    main()
