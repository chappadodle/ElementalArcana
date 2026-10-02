#!/usr/bin/env python3
"""Writes the starter skill tree (docs/superpowers/specs/2026-10-01-skill-tree-design.md) to
src/main/resources/data/elementalarcana/skill_tree/*.json.

Layout (1 unit = 1 screen pixel at zoom 1, y grows downward):
- a core ring of 30 small stat nodes at radius 140;
- Wind to the north, Fire to the east, Water to the south, Earth to the west: a spoke
  node, the element's start, and arms of small nodes and spells leaving it;
- each leveled spell is one "path" entry, expanded by the game into its upgrade chain and forks;
- Ice as a cluster off Water's outer edge, behind a gate that needs Water Affinity 10.

Run it after changing the layout: python3 tools/gen_skill_tree.py
"""
import json
import math
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/elementalarcana/skill_tree"
NS = "elementalarcana:"
STEP = 40
STATS = ["reservoir", "potency", "focus", "ward", "vitality", "insight"]

files = {name: [] for name in ("core", "fire", "water", "wind", "earth", "ice")}
positions = {}


def add(file, node):
    files[file].append(node)
    positions[node["id"]] = (node["x"], node["y"])
    return node


def polar(angle_deg, radius, origin=(0.0, 0.0)):
    a = math.radians(angle_deg)
    return origin[0] + math.cos(a) * radius, origin[1] + math.sin(a) * radius


def small(file, node_id, xy, stat, links, element=None, requires=None):
    node = {"id": node_id, "type": "small", "x": round(xy[0]), "y": round(xy[1]), "stat": stat, "amount": 2, "links": links}
    if element:
        node["element"] = element
    if requires:
        node["requires"] = requires
    return add(file, node)


# ---- core ring ----
RING = 30
for i in range(RING):
    angle = -90 + i * 360 / RING
    small("core", f"core_{i}", polar(angle, 140), STATS[i % len(STATS)], [f"core_{(i + 1) % RING}"])


def ring_node_at(angle_deg):
    return f"core_{round((angle_deg + 90) / (360 / RING)) % RING}"


def arm(file, element, origin_id, origin, angle, items, prefix):
    """A line of nodes leaving origin at angle: ("stat", key) small nodes or ("spell", id) spell nodes."""
    previous = origin_id
    last = None
    for k, (kind, value) in enumerate(items, start=1):
        xy = polar(angle, STEP * k, origin)
        node_id = f"{prefix}_{k}"
        if kind == "stat":
            small(file, node_id, xy, value, [previous], element)
        else:
            node_id = value
            add(file, {"id": node_id, "type": "spell", "x": round(xy[0]), "y": round(xy[1]), "spell": NS + value,
                       "element": element, "links": [previous]})
        previous = node_id
        last = (node_id, xy)
    return last


def path(file, element, path_id, spell, origin, angle, start_from=None, links=None):
    """A spell's upgrade chain, heading out at angle (the game expands it)."""
    dx, dy = polar(angle, STEP)
    node = {"id": path_id, "type": "path", "spell": NS + spell, "x": round(origin[0]), "y": round(origin[1]),
            "dx": round(dx, 3), "dy": round(dy, 3), "element": element}
    if start_from:
        node["from"] = start_from
    if links:
        node["links"] = links
    files[file].append(node)
    # Reserve the chain's spots for the spacing check: Lv 2..10 along the direction.
    for level in range(2, 11):
        positions[f"{path_id}/{level}"] = (origin[0] + dx * (level - 1), origin[1] + dy * (level - 1))


def region(file, element, angle, starter, arms):
    spoke = small(file, f"{element}_spoke", polar(angle, 200), f"affinity/{element}", [ring_node_at(angle)], element)
    start_xy = polar(angle, 260)
    add(file, {"id": f"{element}_start", "type": "start", "x": round(start_xy[0]), "y": round(start_xy[1]),
               "element": element, "spell": NS + starter, "links": [spoke["id"]]})
    path(file, element, f"{element}_{starter}", starter, start_xy, angle, start_from=f"{element}_start")
    ends = {}
    for name, offset, items in arms:
        ends[name] = arm(file, element, f"{element}_start", start_xy, angle + offset, items, f"{element}_{name}")
    return ends


region("wind", "wind", -90, "wind_blade", [
    ("dash", 45, [("spell", "gale_dash")]),
    ("sky", -45, [("stat", "affinity/wind"), ("stat", "focus"), ("stat", "affinity/wind"), ("stat", "potency"),
                  ("spell", "updraft")]),
])
region("fire", "fire", 0, "fireball", [
    ("burst", 45, [("stat", "affinity/fire"), ("stat", "potency"), ("stat", "affinity/fire"), ("stat", "reservoir"),
                   ("spell", "flame_burst")]),
    ("ember", -45, [("stat", "affinity/fire"), ("stat", "focus"), ("stat", "potency")]),
])
water_ends = region("water", "water", 90, "hydro_jet", [
    ("tide", 30, [("spell", "tidal_wave")]),
    ("rain", -50, [("stat", "affinity/water"), ("stat", "reservoir"), ("spell", "bubble_prison"),
                   ("stat", "affinity/water"), ("spell", "healing_rain")]),
    ("deep", 60, [("stat", "affinity/water"), ("stat", "ward"), ("stat", "affinity/water"), ("stat", "vitality"),
                  ("stat", "affinity/water"), ("stat", "reservoir")]),
])

earth_ends = region("earth", "earth", 180, "boulder", [
    ("shield", 45, [("stat", "affinity/earth"), ("stat", "ward"), ("spell", "stone_skin")]),
    ("quake", -45, [("stat", "affinity/earth"), ("stat", "reservoir"), ("spell", "tremor"),
                    ("stat", "affinity/earth"), ("stat", "vitality")]),
])
shield_id, shield_xy = earth_ends["shield"]
path("earth", "earth", "earth_stone_skin", "stone_skin", shield_xy, 225, start_from="stone_skin")

# ---- Ice: a cluster off Water's outer edge ----
deep_end_id, deep_end = water_ends["deep"]
gate_xy = polar(165, STEP, deep_end)
small("ice", "ice_gate", gate_xy, "affinity/water", [deep_end_id], "ice", {"stat": "affinity/water", "min": 10})
ice_xy = polar(165, STEP, gate_xy)
add("ice", {"id": "ice_start", "type": "start", "x": round(ice_xy[0]), "y": round(ice_xy[1]), "element": "ice",
            "spell": NS + "icicle", "links": ["ice_gate"]})
path("ice", "ice", "ice_icicle", "icicle", ice_xy, 195, start_from="ice_start")
shield_end_id, shield_end = arm("ice", "ice", "ice_start", ice_xy, 135, [("stat", "ward"), ("stat", "affinity/water"),
                                                                        ("spell", "frost_shield")], "ice_shield")
path("ice", "ice", "ice_frost_shield", "frost_shield", shield_end, 160, start_from="frost_shield")
arm("ice", "ice", "ice_start", ice_xy, 95, [("stat", "focus"), ("stat", "affinity/water"), ("stat", "ward"),
                                             ("stat", "potency"), ("spell", "frost_nova")], "ice_nova")

# ---- checks and output ----
ids = list(positions)
too_close = []
for i, a in enumerate(ids):
    for b in ids[i + 1:]:
        (ax, ay), (bx, by) = positions[a], positions[b]
        if math.hypot(ax - bx, ay - by) < 18:
            too_close.append((a, b))
assert not too_close, f"nodes too close: {too_close[:10]}"

OUT.mkdir(parents=True, exist_ok=True)
for name, nodes in files.items():
    (OUT / f"{name}.json").write_text(json.dumps({"nodes": nodes}, indent=2) + "\n")
    print(f"wrote {name}.json ({len(nodes)} entries)")
