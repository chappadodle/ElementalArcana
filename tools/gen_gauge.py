#!/usr/bin/env python3
"""Writes tools/autotest/gauge.txt: the balance gauge (docs/superpowers/specs/2026-10-08-balance-pass-design.md).

Each fight gives the test player a believable build for its level, summons a foe at a set level in
front of them, and has them stand and cast at it (the AutoTest `fight` step) while /arcana gauge
counts the damage dealt and taken. The build at level L:

- stat points (L - 1): 40% Potency, 30% Vitality, 20% Ward, 10% Focus;
- the element's spell at min(10, 1 + L // 5), which costs its levels in tree points;
- the other tree points on the small nodes nearest the element's start (then the core ring), as a
  player walking out from their start would take them;
- a mage's gear of the tier their level allows: an apprentice wand and robes, then adept (15),
  master (35) and archmage (50), the staff of their element.

Run from the project root:  python3 tools/gen_gauge.py
"""
import json
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TREE = ROOT / "src/main/resources/data/elementalarcana/skill_tree"
OUT = ROOT / "tools/autotest/gauge.txt"

# (foe's entity type, its base level (before its rank's bonus), the player's level, element, spell,
#  hold ticks, gap ticks, seconds, why)
FIGHTS = [
    ("elementalarcana:revenant", 15, 20, "fire", "elementalarcana:fireball", 20, 10, 150,
     "a crypt's Revenant in a level-15 zone (story boss: 25; its element is random), against a player of 20"),
    ("elementalarcana:revenant", 15, 20, "fire", "elementalarcana:fireball", 20, 10, 150, "another Revenant"),
    ("elementalarcana:tower_magister", 15, 20, "fire", "elementalarcana:fireball", 20, 10, 150,
     "a tower's Magister in a level-15 zone (25), against a player of 20"),
    ("elementalarcana:forgewarden", 35, 30, "water", "elementalarcana:hydro_jet", 30, 10, 150,
     "the Forgewarden of a Nether forge (zone 35), against a player of 30"),
    ("elementalarcana:fire_sovereign", 30, 35, "water", "elementalarcana:hydro_jet", 30, 10, 180,
     "Vulkhar, a Sovereign at its least (30, story boss: 40), against a player of 35"),
    ("elementalarcana:wind_sovereign", 30, 35, "fire", "elementalarcana:fireball", 20, 10, 180, "Caelum, likewise"),
    ("elementalarcana:the_hollow", 50, 50, "water", "elementalarcana:hydro_jet", 30, 10, 240,
     "the Hollow (60), against a player of 50"),
]


def load_tree():
    nodes, links = {}, {}
    for path in TREE.glob("*.json"):
        for node in json.loads(path.read_text())["nodes"]:
            nodes[node["id"]] = node
            for other in node.get("links", []):
                links.setdefault(node["id"], set()).add(other)
                links.setdefault(other, set()).add(node["id"])
    return nodes, links


def stat_nodes(start, count, nodes, links):
    """The first {count} small and notable nodes walking out from {start}, nearest first."""
    taken, seen, queue = [], {start}, deque([start])
    while queue and len(taken) < count:
        here = queue.popleft()
        for other in sorted(links.get(here, ())):
            if other in seen or other not in nodes:
                continue
            seen.add(other)
            if nodes[other]["type"] in ("small", "notable"):
                taken.append(other)
                queue.append(other)
                if len(taken) == count:
                    break
    return taken


def build(level, element, spell, nodes, links):
    points = level - 1
    spend = {"potency": round(points * 0.4), "vitality": round(points * 0.3), "ward": round(points * 0.2)}
    spend["focus"] = points - sum(spend.values())
    spell_level = min(10, 1 + level // 5)
    tree = stat_nodes(f"{element}_start", max(0, (level - 1) - (spell_level - 1)), nodes, links)
    lines = ["cmd arcana tree reset", "cmd arcana stats reset", f"cmd arcana awaken {element}",
             f"cmd arcana level set {level}"]
    lines += [f"cmd arcana stats spend {n} {stat}" for stat, n in spend.items() if n > 0]
    branches = (" cluster" if spell.endswith("fireball") else "") if spell_level >= 5 else ""
    lines.append(f"cmd arcana spell {spell} {spell_level}{branches if spell.endswith('fireball') else ''}")
    lines += [f"cmd arcana tree take {node}" for node in tree]
    tier = "apprentice" if level < 15 else "adept" if level < 35 else "master" if level < 50 else "archmage"
    focus = "apprentice_wand" if tier == "apprentice" else f"{tier}_staff"
    lines.append(f'cmd item replace entity @s weapon.mainhand with elementalarcana:{focus}[elementalarcana:element="{element}"]')
    for slot, piece in (("head", "hood"), ("chest", "robe"), ("legs", "trousers"), ("feet", "boots")):
        lines.append(f"cmd item replace entity @s armor.{slot} with elementalarcana:{tier}_{piece}")
    return lines


def main():
    nodes, links = load_tree()
    out = ["# The balance gauge (written by tools/gen_gauge.py: edit that, not this). Each fight: a build for",
           "# the player's level, a foe at a set level 12 blocks ahead, the player standing and casting at it",
           "# while /arcana gauge counts the damage both ways (the player is kept standing; the report says",
           "# when they would have fallen).",
           "hud on", "cmd time set noon", "cmd weather clear", "cmd gamerule doDaylightCycle false",
           "cmd gamerule doMobSpawning false", "cmd gamerule doMobLoot false", "cmd gamemode survival",
           "cmd arcana affinity reset", "cmd tp @s 0.5 100 0.5 0 0", "wait 40"]
    for (foe, base, level, element, spell, hold, gap, seconds, why) in FIGHTS:
        out += ["", f"# {why}", "cmd kill @e[type=!player]", "cmd kill @e[type=item]",
                # A walled arena (barriers, so nothing is knocked off it into the world below).
                "cmd fill -21 99 -13 21 99 37 minecraft:stone", "cmd fill -21 100 -13 21 112 37 minecraft:barrier",
                "cmd fill -20 100 -12 20 112 36 minecraft:air",
                "cmd tp @s 0.5 100 0.5 0 0", "cmd effect clear @s", "cmd arcana affinity reset"]
        out += build(level, element, spell, nodes, links)
        out += ["cmd arcana mana fill", "cmd arcana cooldowns reset", "cmd effect give @s minecraft:instant_health 1 10 true",
                f"cmd summon {foe} 0.5 100 12.5",
                f"cmd arcana creaturelevel @e[type={foe}] set {base}",
                f"cmd arcana creaturelevel @e[type={foe}]",
                "wait 20", f"select {spell}", "cmd arcana gauge start", f"fight {foe} {seconds} {hold} {gap}",
                "wait 5", "cmd arcana gauge stop", "wait 10"]
    out += ["", "cmd kill @e[type=!player]", "cmd arcana tree reset", "cmd arcana stats reset",
            "cmd item replace entity @s weapon.mainhand with minecraft:air", "cmd item replace entity @s armor.head with minecraft:air", "cmd item replace entity @s armor.chest with minecraft:air",
            "cmd item replace entity @s armor.legs with minecraft:air", "cmd item replace entity @s armor.feet with minecraft:air",
            "wait 10", "quit"]
    OUT.write_text("\n".join(out) + "\n")
    print(f"wrote {OUT.relative_to(ROOT)} ({len(FIGHTS)} fights)")


if __name__ == "__main__":
    main()
