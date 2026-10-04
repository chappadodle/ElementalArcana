#!/usr/bin/env python3
"""Draws the relics' icons (docs/superpowers/specs/2026-10-04-relics-design.md), 16x16 pixel art,
and writes their item models.

Run from the project root:  python3 tools/gen_relics.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"

# Each icon: rows of 16 characters, '.' clear, other letters from the icon's palette.
ICONS = {
    "ember_heart": ({
        "o": (255, 140, 40), "y": (255, 214, 90), "G": (214, 168, 58), "g": (150, 108, 34),
        "r": (120, 20, 24), "R": (214, 48, 36), "W": (255, 220, 190),
    }, [
        "................",
        "......o...o.....",
        ".....oyo.oyo....",
        "....oyyo.oyyo...",
        "...GGGGG.GGGGG..",
        "..GrrRRGGGrRRrG.",
        "..GrRWRRRRRRRrG.",
        "..GrRRRRRRRRRrG.",
        "..GrRRRRRRRRRrG.",
        "...GrRRRRRRRrG..",
        "....GrRRRRRrG...",
        ".....GrRRRrG....",
        "......GrRrG.....",
        ".......GgG......",
        "........g.......",
        "................",
    ]),
    "tidecallers_pearl": ({
        "s": (60, 120, 200), "S": (110, 170, 235), "d": (30, 70, 140),
        "p": (225, 235, 250), "P": (255, 255, 255), "q": (170, 190, 220),
    }, [
        "................",
        "................",
        "....dSSSSSSd....",
        "...dSssssssSd...",
        "..dSsSsSsSsSSd..",
        "..dssSsSsSssSd..",
        "...dsssssssd....",
        "......pPp.......",
        ".....pPPPp......",
        ".....pPpqp......",
        "...dSSqqqSSd....",
        "..dSsSsSsSsSd...",
        "..dssSsSsSssd...",
        "...ddsssssdd....",
        ".....ddddd......",
        "................",
    ]),
    "rimeheart_locket": ({
        "c": (190, 200, 215), "C": (130, 140, 160), "b": (160, 225, 255), "B": (90, 170, 230),
        "W": (240, 252, 255), "d": (50, 100, 160),
    }, [
        "......c..c......",
        ".....c....c.....",
        "....c......c....",
        "....c......c....",
        ".....c....c.....",
        "......CccC......",
        "....dddCdddd....",
        "...dBbbdBbbBd...",
        "...dbWbbbbbbd...",
        "...dbbbbbbbbd...",
        "....dbbbbbbd....",
        ".....dbbbbd.....",
        "......dbbd......",
        ".......dd.......",
        "................",
        "................",
    ]),
    "feather_of_the_gale": ({
        "w": (235, 250, 245), "m": (150, 230, 200), "M": (70, 150, 120), "q": (60, 110, 95),
    }, [
        "................",
        "............www.",
        "..........wwwmw.",
        ".........wwwmmw.",
        "........wwwmMmw.",
        ".......wwwmMmww.",
        "......wwwmMmww..",
        ".....wwwmMmww...",
        "....wwwmMmww....",
        "...wwwmMmww.....",
        "...wwmMmww......",
        "....mMmw........",
        "...qM...........",
        "..q.............",
        ".q..............",
        "................",
    ]),
    "stoneheart_idol": ({
        "s": (130, 130, 136), "S": (170, 170, 176), "d": (80, 80, 88), "g": (90, 190, 90), "G": (160, 240, 140),
        "m": (70, 120, 50),
    }, [
        "................",
        "......dddd......",
        ".....dSSSSd.....",
        ".....dSgGSd.....",
        ".....dSssSd.....",
        "......dssd......",
        "....ddSSSSdd....",
        "...dSSsGgsSSd...",
        "...dSsssssssd...",
        "...dSsmssmssd...",
        "....dsssssd.....",
        "....dSsmsSd.....",
        "....dssssssd....",
        "...ddddddddd....",
        "................",
        "................",
    ]),
    "prism_of_the_deep": ({
        "p": (190, 120, 255), "P": (230, 190, 255), "v": (120, 60, 190), "d": (70, 30, 120), "W": (255, 245, 255),
    }, [
        "................",
        ".......dd.......",
        "......dPpd......",
        ".....dPWppd.....",
        ".....dPppvd.....",
        "....dPPppvvd....",
        "....dPpppvvd....",
        "...dPPppppvvd...",
        "...dPppppvvvd...",
        "...dPppppvvvd...",
        "....dPppvvvd....",
        "....dPppvvvd....",
        ".....dppvvd.....",
        "......dpvd......",
        ".......dd.......",
        "................",
    ]),
    "storm_sigil": ({
        "r": (90, 80, 70), "R": (140, 125, 100), "y": (255, 225, 77), "Y": (255, 250, 190), "b": (40, 44, 60),
    }, [
        "................",
        ".....rrrrrr.....",
        "...rrRRRRRRrr...",
        "..rRbbbbbYbbRr..",
        "..rRbbbbYYbbRr..",
        ".rRbbbbyYbbbbRr.",
        ".rRbbbyYbbbbbRr.",
        ".rRbbyyyyyybbRr.",
        ".rRbbbbbyybbbRr.",
        ".rRbbbbyybbbbRr.",
        ".rRbbbyybbbbbRr.",
        "..rRbbybbbbbRr..",
        "..rRbybbbbbbRr..",
        "...rrRRRRRRrr...",
        ".....rrrrrr.....",
        "................",
    ]),
    "sunstone": ({
        "g": (214, 168, 58), "G": (255, 210, 90), "s": (255, 241, 184), "S": (255, 255, 235), "o": (200, 120, 30),
    }, [
        "................",
        ".......G........",
        "...G...g...G....",
        "....g..g..g.....",
        ".....gggggg.....",
        "....ggssssgg....",
        "GgggsSSssssggggG",
        "....gsSssssg....",
        "....gssssssg....",
        "....ggssssgg....",
        ".....gggggg.....",
        "....g..g..g.....",
        "...G...g...G....",
        ".......G........",
        "................",
        "................",
    ]),
    "revenants_phylactery": ({
        "c": (180, 185, 200), "C": (120, 125, 140), "g": (40, 34, 52), "v": (150, 90, 255), "V": (210, 170, 255),
        "w": (245, 235, 255),
    }, [
        "................",
        "......CccC......",
        "......cCCc......",
        ".......gg.......",
        "......g..g......",
        ".....gvvvvg.....",
        "....gvVVvvvg....",
        "....gvVwvvvg....",
        "....gvvvvvvg....",
        "....gvvvVvvg....",
        "....gvvVVvvg....",
        "....gvvvvvvg....",
        ".....gvvvvg.....",
        "......gggg......",
        "................",
        "................",
    ]),
}


def draw(palette, rows):
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img[y, x] = (*palette[ch], 255)
    return Image.fromarray(img, "RGBA")


def main():
    textures = ASSETS / "textures/item"
    models = ASSETS / "models/item"
    textures.mkdir(parents=True, exist_ok=True)
    models.mkdir(parents=True, exist_ok=True)
    for name, (palette, rows) in ICONS.items():
        draw(palette, rows).save(textures / f"{name}.png")
        (models / f"{name}.json").write_text(json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"elementalarcana:item/{name}"}}, indent=2) + "\n")
    print(f"wrote {len(ICONS)} relic icons")


if __name__ == "__main__":
    main()
