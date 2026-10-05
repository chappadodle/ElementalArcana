#!/usr/bin/env python3
"""Writes the starter skill tree (docs/superpowers/specs/2026-10-01-skill-tree-design.md) to
src/main/resources/data/elementalarcana/skill_tree/*.json.

Layout (1 unit = 1 screen pixel at zoom 1, y grows downward):
- a core ring of 30 small stat nodes at radius 140;
- Wind to the north, Fire to the east, Water to the south, Earth to the west: a spoke
  node, the element's start, and arms of small nodes and spells leaving it;
- each leveled spell is one "path" entry, expanded by the game into its upgrade chain and forks;
- Ice as a cluster off Water's outer edge, behind a gate that needs Water Affinity 10;
- the other derived elements the same way (docs/superpowers/specs/2026-10-03-derived-elements-design.md):
  Lightning off Wind's Gale Dash, Radiance off Fire's Flame Burst, Crystal off Earth's quake arm,
  each behind a gate that needs its family's Affinity 10.

Run it after changing the layout: python3 tools/gen_skill_tree.py
"""
import json
import math
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/data/elementalarcana/skill_tree"
NS = "elementalarcana:"
STEP = 40
STATS = ["reservoir", "potency", "focus", "ward", "vitality", "insight"]

files = {name: [] for name in ("core", "fire", "water", "wind", "earth", "ice", "crystal", "lightning", "radiance")}
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
    """A line of nodes leaving origin at angle: ("stat", key) small nodes, ("spell", id) spell nodes,
    ("notable", (id, {stat: amount})) notables and ("keystone", (id, keystone)) keystones."""
    previous = origin_id
    last = None
    for k, (kind, value) in enumerate(items, start=1):
        xy = polar(angle, STEP * k, origin)
        node_id = f"{prefix}_{k}"
        if kind == "stat":
            small(file, node_id, xy, value, [previous], element)
        elif kind == "spell":
            node_id = value
            add(file, {"id": node_id, "type": "spell", "x": round(xy[0]), "y": round(xy[1]), "spell": NS + value,
                       "element": element, "links": [previous]})
        else:
            node_id, payload = value
            node = {"id": node_id, "type": kind, "x": round(xy[0]), "y": round(xy[1]), "links": [previous]}
            node["stats" if kind == "notable" else "keystone"] = payload
            if element:
                node["element"] = element
            add(file, node)
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


# Each region ends one arm in a notable (several stats at once) and a keystone (a rule of magic
# changed, for better and worse; see docs/superpowers/specs/2026-10-02-notables-and-keystones-design.md).
wind_ends = region("wind", "wind", -90, "wind_blade", [
    ("dash", 45, [("spell", "gale_dash")]),
    ("sky", -45, [("stat", "affinity/wind"), ("stat", "focus"), ("stat", "affinity/wind"), ("stat", "potency"),
                  ("spell", "updraft")]),
    ("gust", 100, [("stat", "affinity/wind"), ("stat", "focus"),
                   ("notable", ("wind_tailwind", {"affinity/wind": 6, "focus": 4})),
                   ("keystone", ("wind_gale_step", "gale_step"))]),
    ("eye", -80, [("stat", "affinity/wind"), ("stat", "focus"), ("spell", "stormeye")]),
])
fire_ends = region("fire", "fire", 0, "fireball", [
    ("burst", 45, [("stat", "affinity/fire"), ("stat", "potency"), ("stat", "affinity/fire"), ("stat", "reservoir"),
                   ("spell", "flame_burst")]),
    ("sprite", 90, [("stat", "affinity/fire"), ("spell", "ember_sprite")]),
    ("ember", -45, [("stat", "affinity/fire"), ("stat", "focus"), ("stat", "potency"),
                    ("notable", ("fire_kindled_soul", {"affinity/fire": 6, "potency": 4})),
                    ("keystone", ("fire_glass_cannon", "glass_cannon"))]),
])
water_ends = region("water", "water", 90, "hydro_jet", [
    ("tide", 30, [("spell", "tidal_wave")]),
    ("rain", -50, [("stat", "affinity/water"), ("stat", "reservoir"), ("spell", "bubble_prison"),
                   ("stat", "affinity/water"), ("spell", "healing_rain")]),
    ("deep", 60, [("stat", "affinity/water"), ("stat", "ward"), ("stat", "affinity/water"), ("stat", "vitality"),
                  ("stat", "affinity/water"), ("stat", "reservoir")]),
    ("spring", -95, [("stat", "reservoir"), ("stat", "affinity/water"),
                     ("notable", ("water_deep_reserves", {"reservoir": 6, "vitality": 4})),
                     ("keystone", ("water_wellspring", "wellspring"))]),
])

earth_ends = region("earth", "earth", 180, "boulder", [
    ("shield", 45, [("stat", "affinity/earth"), ("stat", "ward"), ("spell", "stone_skin")]),
    ("quake", -45, [("stat", "affinity/earth"), ("stat", "reservoir"), ("spell", "tremor"),
                    ("stat", "affinity/earth"), ("stat", "vitality")]),
    ("bedrock", 100, [("stat", "affinity/earth"), ("stat", "vitality"),
                      ("notable", ("earth_stone_blood", {"vitality": 6, "ward": 4})),
                      ("keystone", ("earth_mountain_heart", "mountain_heart"))]),
])

# ---- the core's keystone: Blood Magic, out past the ring between Fire and Water ----
vigor = add("core", {"id": "core_arcane_vigor", "type": "notable", "x": round(polar(45, 190)[0]), "y": round(polar(45, 190)[1]),
                     "stats": {"reservoir": 4, "vitality": 4, "potency": 2}, "links": [ring_node_at(45)]})
add("core", {"id": "core_blood_magic", "type": "keystone", "x": round(polar(45, 235)[0]), "y": round(polar(45, 235)[1]),
             "keystone": "blood_magic", "links": ["core_arcane_vigor"]})
shield_id, shield_xy = earth_ends["shield"]
path("earth", "earth", "earth_stone_skin", "stone_skin", shield_xy, 225, start_from="stone_skin")
# Skyward Leap (its spell is still updraft) levels on along Wind's sky arm.
sky_id, sky_xy = wind_ends["sky"]
path("wind", "wind", "wind_skyward_leap", "updraft", sky_xy, -135, start_from="updraft")
# Stormeye levels on along Wind's eye arm.
eye_id, eye_xy = wind_ends["eye"]
path("wind", "wind", "wind_stormeye", "stormeye", eye_xy, -170, start_from="stormeye")
# Gale Dash levels on eastward from its node (Lightning's cluster leaves it to the north-east).
dash_id, dash_xy = wind_ends["dash"]
path("wind", "wind", "wind_gale_dash", "gale_dash", dash_xy, 0, start_from="gale_dash")

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
nova_id, nova_xy = arm("ice", "ice", "ice_start", ice_xy, 95, [("stat", "focus"), ("stat", "affinity/water"), ("stat", "ward"),
                                                                ("stat", "potency"), ("spell", "frost_nova")], "ice_nova")
arm("ice", "ice", nova_id, nova_xy, 95, [("notable", ("ice_winters_edge", {"affinity/water": 6, "potency": 4})),
                                         ("keystone", ("ice_winters_grasp", "winters_grasp"))], "ice_grasp")



# ---- the other derived elements: clusters like Ice's, each off its family's region ----
def cluster(element, family, origin, angle, starter, arms):
    """Two of the family's nodes leading on from origin, the gate (the family's Affinity 10), then
    the element's start and its arms."""
    previous, xy = origin
    for k, stat in enumerate((f"affinity/{family}", "potency"), start=1):
        xy = polar(angle, STEP, xy)
        previous = small(element, f"{element}_way_{k}", xy, stat, [previous], family)["id"]
    xy = polar(angle, STEP, xy)
    small(element, f"{element}_gate", xy, f"affinity/{family}", [previous], element, {"stat": f"affinity/{family}", "min": 10})
    start_xy = polar(angle, STEP, xy)
    add(element, {"id": f"{element}_start", "type": "start", "x": round(start_xy[0]), "y": round(start_xy[1]),
                  "element": element, "spell": NS + starter, "links": [f"{element}_gate"]})
    for name, offset, items in arms:
        arm(element, element, f"{element}_start", start_xy, angle + offset, items, f"{element}_{name}")
    return start_xy


lightning_xy = cluster("lightning", "wind", wind_ends["dash"], -45, "chain_lightning", [
    ("storm", -45, [("stat", "potency"), ("stat", "affinity/wind"), ("stat", "focus"),
                    ("notable", ("lightning_static_charge", {"potency": 6, "affinity/wind": 4})),
                    ("keystone", ("lightning_conductor", "conductor"))]),
    ("spark", 45, [("stat", "focus"), ("stat", "affinity/wind"), ("spell", "thunderclap")]),
    # Stormcall (docs/superpowers/specs/2026-10-05-stormcall-design.md).
    ("cloud", 90, [("stat", "potency"), ("stat", "affinity/wind"), ("spell", "stormcall")]),
])
# Chain Lightning levels on straight out from Lightning's start.
path("lightning", "lightning", "lightning_chain_lightning", "chain_lightning", lightning_xy, -45, start_from="lightning_start")
radiance_xy = cluster("radiance", "fire", fire_ends["burst"], 45, "smite", [
    ("dawn", -45, [("stat", "vitality"), ("stat", "affinity/fire"), ("stat", "reservoir"),
                   ("notable", ("radiance_inner_light", {"vitality": 6, "affinity/fire": 4})),
                   ("keystone", ("radiance_sunborn", "sunborn"))]),
    ("halo", 45, [("stat", "ward"), ("stat", "affinity/fire"), ("spell", "sanctuary")]),
    # Dawnbreak (docs/superpowers/specs/2026-10-05-dawnbreak-design.md).
    ("sun", -90, [("stat", "vitality"), ("stat", "affinity/fire"), ("spell", "dawnbreak")]),
])
crystal_xy = cluster("crystal", "earth", earth_ends["quake"], 135, "prism_bolt", [
    ("facet", 45, [("stat", "ward"), ("stat", "affinity/earth"), ("stat", "focus"),
                   ("notable", ("crystal_prismatic_ward", {"ward": 6, "affinity/earth": 4})),
                   ("keystone", ("crystal_refraction", "refraction"))]),
    ("geode", 90, [("stat", "potency"), ("stat", "affinity/earth"), ("spell", "prism_ward")]),
    # Geode Sentinel (docs/superpowers/specs/2026-10-05-geode-sentinel-design.md).
    ("sentinel", -90, [("stat", "ward"), ("stat", "affinity/earth"), ("spell", "geode_sentinel")]),
])

# Smite levels on straight out from Radiance's start.
path("radiance", "radiance", "radiance_smite", "smite", radiance_xy, 45, start_from="radiance_start")
# Prism Bolt levels on straight out from Crystal's start.
path("crystal", "crystal", "crystal_prism_bolt", "prism_bolt", crystal_xy, 135, start_from="crystal_start")

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
