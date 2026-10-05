#!/usr/bin/env python3
"""Generates the Seeker's Compass's art and data (docs/superpowers/specs/2026-10-05-seekers-compass-design.md):

- 32 frames of the compass (a brass rim set with an amethyst, a deep violet face, a needle glowing
  at its tip), frame k with the needle k/32 of a turn clockwise from straight up, and the item model
  choosing among them by the minecraft:angle property (0 is "dead ahead");
- the recipe;
- a structure tag per kind it can seek (elementalarcana:seekable/<kind>).

Run from the project root:  python3 tools/gen_seeker.py
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"
FRAMES = 32

# What each kind seeks (kept in the order of api/SeekerRules.KINDS).
KINDS = {
    "shrines": ["#" + NS + "shrines"],
    "ruins": [NS + "ruin"],
    "crypts": [NS + "crypt"],
    "mage_towers": [NS + "mage_tower"],
    "sanctums": ["#" + NS + "sanctums"],
    "drake_nests": [NS + "drake_nest"],
    "sky_isles": [NS + "sky_isle"],
}

RIM_DARK = (92, 62, 24)
RIM = (176, 128, 52)
RIM_LIGHT = (232, 192, 104)
FACE = (36, 26, 58)
FACE_LIGHT = (54, 40, 84)
STAR = (150, 130, 200)
GEM = (190, 120, 255)
GEM_LIGHT = (240, 210, 255)
TIP = (255, 140, 240)
TIP_HOT = (255, 236, 252)
TAIL = (120, 110, 150)
PIN = (232, 192, 104)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def face():
    """The compass without its needle."""
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - cx, y - cy)
            if r > 7.6:
                continue
            if r > 6.4:
                # The rim, lit from the top left.
                light = (x - cx) + (y - cy) < -2
                dark = (x - cx) + (y - cy) > 3
                color = RIM_LIGHT if light else RIM_DARK if dark else RIM
            else:
                color = FACE_LIGHT if (x - cx) + (y - cy) < -5 else FACE
            img[y, x] = (*color, 255)
    for x, y in ((4, 5), (11, 4), (10, 11), (4, 10), (7, 3)):
        img[y, x] = (*STAR, 255)
    # The amethyst set in the rim at the top.
    img[0, 7] = (*GEM_LIGHT, 255)
    img[0, 8] = (*GEM, 255)
    img[1, 7] = (*GEM, 255)
    img[1, 8] = (*GEM, 255)
    return img


def frame(k):
    img = face()
    angle = 2 * math.pi * k / FRAMES
    dx, dy = math.sin(angle), -math.cos(angle)
    cx = cy = 7.5
    # The tail, then the glowing half over it, from the middle out.
    for step in np.arange(0, 4.6, 0.25):
        x, y = round(cx - dx * step - 0.01), round(cy - dy * step - 0.01)
        img[y, x] = (*TAIL, 255)
    for step in np.arange(0, 5.6, 0.25):
        x, y = round(cx + dx * step - 0.01), round(cy + dy * step - 0.01)
        img[y, x] = (*(TIP_HOT if step > 4.2 else TIP), 255)
    img[round(cy - 0.01), round(cx - 0.01)] = (*PIN, 255)
    return Image.fromarray(img, "RGBA")


def main():
    items = ASSETS / "textures/item"
    models = ASSETS / "models/item"
    for k in range(FRAMES):
        frame(k).save(items / f"seekers_compass_{k:02d}.png")
        write_json(models / f"seekers_compass_{k:02d}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/seekers_compass_{k:02d}"}})
    # The last override that matches wins: frame k from half a frame before k/32, frame 0 again at the end.
    overrides = [{"predicate": {"angle": max(0.0, (k - 0.5) / FRAMES)}, "model": f"{NS}item/seekers_compass_{k:02d}"}
                 for k in range(FRAMES)]
    overrides.append({"predicate": {"angle": (FRAMES - 0.5) / FRAMES}, "model": f"{NS}item/seekers_compass_00"})
    write_json(models / "seekers_compass.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/seekers_compass_00"},
        "overrides": overrides})
    write_json(DATA / "recipe/seekers_compass.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [{"item": "minecraft:compass"}, {"item": "minecraft:amethyst_shard"}, {"item": "minecraft:amethyst_shard"},
                        {"tag": NS + "essences"}],
        "result": {"id": f"{NS}seekers_compass", "count": 1}})
    for kind, values in KINDS.items():
        write_json(DATA / f"tags/worldgen/structure/seekable/{kind}.json", {"values": values})
    print("seeker art and data written")


if __name__ == "__main__":
    main()
