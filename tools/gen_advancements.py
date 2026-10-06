#!/usr/bin/env python3
"""Writes the mod's advancement tab (docs/superpowers/specs/2026-10-03-advancements-design.md):
the advancements under data/elementalarcana/advancement/arcana/, the tags they use, and their
titles and descriptions in en_us.json (every advancement.elementalarcana.* key is rewritten, in
this order, at the end of the file).

Run it after changing the tab: python3 tools/gen_advancements.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/elementalarcana"
LANG = ROOT / "src/main/resources/assets/elementalarcana/lang/en_us.json"
NS = "elementalarcana:"


def item(name, element=None, potion=None):
    icon = {"id": name if ":" in name else NS + name}
    if element:
        icon["components"] = {NS + "element": element}
    if potion:
        icon["components"] = {"minecraft:potion_contents": {"potion": potion}}
    return icon


def magic(event, key=None, least=None):
    conditions = {"event": event}
    if key is not None:
        conditions["key"] = key
    if least is not None:
        conditions["min"] = least
    return {event: {"trigger": NS + "magic", "conditions": conditions}}


def location(structures):
    return {"found": {"trigger": "minecraft:location", "conditions": {"player": {"location": {"structures": structures}}}}}


def killed(entity_type):
    return {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {"type": entity_type}}}}


def has(*stacks):
    """inventory_changed, one criterion per stack (all needed)."""
    criteria = {}
    for name, element in stacks:
        predicate = {"items": NS + name}
        if element:
            predicate["components"] = {NS + "element": element}
        criteria[f"{name}_{element}" if element else name] = {
            "trigger": "minecraft:inventory_changed", "conditions": {"items": [predicate]}}
    return criteria


def effect(*effects):
    return {e.split(":")[1]: {"trigger": "minecraft:effects_changed", "conditions": {"effects": {e: {}}}} for e in effects}


def any_of(criteria):
    return criteria, [list(criteria)]


# Arcane Flora's herbs and the names of their tonics (tools/gen_flora.py).
HERBS = ["emberbloom", "moonlily", "skyplume", "deepcap", "frostcap", "prismleaf", "stormthistle", "sunpetal"]
TONICS = ["embers", "tides", "gales", "stone", "frost", "prisms", "storms", "dawn"]
TICK = {"tick": {"trigger": "minecraft:tick"}}
BLESSINGS = {name: crit for name, crit in
             ((f"{e}_blessing", {"trigger": "minecraft:effects_changed",
                                 "conditions": {"effects": {f"{NS}{e}_blessing": {}}}})
              for e in ("fire", "water", "wind", "earth"))}

# id, parent, icon, frame, title, description, criteria, requirements (None: all of them)
ADVANCEMENTS = [
    ("root", None, item("arcanist_journal"), "task", "Elemental Arcana",
     "Mana sleeps in everyone. One day, it wakes", TICK, None),
    ("awakening", "root", item("wind_essence"), "goal", "Awakening",
     "Your magic wakes: survive a brush with an element, or wait for the day it stirs", magic("elements", least=1), None),
    ("first_spark", "awakening", item("fire_essence"), "task", "First Spark",
     "Cast a spell", magic("cast"), None),
    ("reaction", "first_spark", item("water_essence"), "task", "Elemental Chemistry",
     "Set off a reaction: Melt, Vaporize, Freeze, Swirl or Crystallize", magic("reaction"), None),
    ("crossroads", "first_spark", item("adept_staff", "fire"), "task", "Crossroads",
     "Raise a spell to level 5, where its path forks", magic("spell_level", least=5), None),
    ("capstone", "crossroads", item("archmage_staff", "fire"), "goal", "Capstone",
     "Raise a spell to level 10", magic("spell_level", least=10), None),
    ("skyward", "first_spark", item("minecraft:feather"), "task", "Take to the Sky",
     "Leap skyward on the wind, and glide", effect(NS + "skyward"), None),
    ("two_natures", "awakening", item("fire_catalyst"), "task", "Two Natures",
     "Wake a second element (a Catalyst can help)", magic("elements", least=2), None),
    ("elemental_sage", "two_natures", item("earth_catalyst"), "challenge", "Elemental Sage",
     "Hold four elements at once", magic("elements", least=4), None),
    ("apprentice", "awakening", item("apprentice_wand", "water"), "task", "Apprentice",
     "Reach magic level 10", magic("level", least=10), None),
    ("adept", "apprentice", item("adept_staff", "water"), "task", "Adept",
     "Reach magic level 25", magic("level", least=25), None),
    ("master", "adept", item("master_staff", "water"), "goal", "Master",
     "Reach magic level 40", magic("level", least=40), None),
    ("archmage", "master", item("archmage_staff", "water"), "challenge", "Archmage",
     "Reach magic level 60", magic("level", least=60), None),
    ("rule_breaker", "awakening", item("tome_of_insight"), "task", "Rule Breaker",
     "Take a keystone in the skill tree, and change a rule of magic", magic("keystones", least=1), None),
    ("places_of_power", "awakening", item("shrine_core"), "task", "Places of Power",
     "Find an elemental shrine", location("#" + NS + "shrines"), None),
    ("ley_walker", "places_of_power", item("minecraft:ender_pearl"), "task", "Ley Walker",
     "Walk a ley line from one shrine to another", magic("ley"), None),
    ("a_place_of_your_own", "ley_walker", item("ley_anchor"), "task", "A Place of Your Own",
     "Touch a Ley Anchor, and bring the ley lines home", magic("ley_anchor"), None),
    ("blessed", "places_of_power", item("earth_essence"), "task", "Blessed",
     "Receive a shrine's blessing", *any_of(BLESSINGS)),
    ("wisp_hunter", "awakening", item("wisp_mote"), "task", "Wisp Hunter",
     "Defeat a wisp", killed("#" + NS + "wisps"), None),
    ("dancing_lights", "wisp_hunter", item("minecraft:red_mushroom"), "task", "Dancing Lights",
     "Step into a wisp ring at night", magic("wisp_ring"), None),
    ("kindred_spirit", "wisp_hunter", item("binding_charm"), "goal", "Kindred Spirit",
     "Bind a wisp as your familiar", magic("familiar"), None),
    ("dragon_slayer", "awakening", item("fire_drake_scale"), "goal", "Dragon Slayer",
     "Defeat an Elemental Drake", killed("#" + NS + "drakes"), None),
    ("drake_hoard", "dragon_slayer", item("minecraft:gold_block"), "task", "Hoard",
     "Find a drake's nest on the heights", location(NS + "drake_nest"), None),
    ("hatchling", "drake_hoard", item("fire_drake_egg"), "goal", "Hatchling",
     "Keep a drake's egg warm in its element's way until it hatches", magic("drake_hatched"), None),
    ("dragon_rider", "hatchling", item("drake_saddle"), "challenge", "Dragon Rider",
     "Saddle and ride a drake you raised", magic("drake_ride"), None),
    ("hedge_magic", "awakening", item("cantrip_scroll"), "task", "Hedge Magic",
     "Learn a cantrip from a Cantrip Scroll", magic("cantrip"), None),
    ("infused", "awakening", item("infusion_altar"), "task", "Infused",
     "Infuse a weapon or armour piece with an element at an Infusion Altar", magic("infusion"), None),
    ("island_in_the_sky", "awakening", item("minecraft:feather"), "goal", "Island in the Sky",
     "Set foot on a sky isle", location(NS + "sky_isle"), None),
    ("wish_upon_a_star", "awakening", item("star_fragment"), "goal", "Wish Upon a Star",
     "Reach a fallen star before dawn and take its fragments", has(("star_fragment", None)), None),
    ("golem_breaker", "awakening", item("minecraft:mossy_cobblestone"), "task", "Golem Breaker",
     "Defeat an Elemental Golem", killed("#" + NS + "golems"), None),
    ("into_the_wild", "awakening", item("heartwood"), "challenge", "Into the Wild",
     "Defeat a Thornwood Treant, a Frost Wraith and an Ember Salamander",
     {name: killed(NS + name)["killed"] for name in ("thornwood_treant", "frost_wraith", "ember_salamander")}, None),
    ("wild_things", "into_the_wild", item("harpy_plume"), "challenge", "Wild Things",
     "Defeat a Gale Harpy, a Crystal Crawler and a Bog Lurker",
     {name: killed(NS + name)["killed"] for name in ("gale_harpy", "crystal_crawler", "bog_lurker")}, None),
    ("wild_at_heart", "into_the_wild", item("salamander_charm"), "goal", "Wild at Heart",
     "Make a Heartwood Talisman, a Wraithsilk Veil and a Salamander Charm",
     has(("heartwood_talisman", None), ("wraithsilk_veil", None), ("salamander_charm", None)), None),
    ("bag_of_tricks", "awakening", item("charm_pouch"), "task", "Bag of Tricks",
     "Carry a Charm Pouch with three charms or relics in it", {
         "pouch": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{
             "items": NS + "charm_pouch", "predicates": {"minecraft:container": {"items": {"size": {"min": 3}}}}}]}}}, None),
    ("where_the_needle_points", "awakening", item("seekers_compass"), "task", "Where the Needle Points",
     "Find a place with a Seeker's Compass", magic("seek_found"), None),
    ("lorekeeper", "awakening", item("minecraft:written_book"), "task", "Lorekeeper",
     "Find a page of the old story in a ruin", {
         "page": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{
             "items": "minecraft:written_book", "predicates": {"minecraft:custom_data": "{elementalarcana_lore:1b}"}}]}}}, None),
    ("attuned_foe", "awakening", item("crystal_essence"), "task", "Attuned Foe",
     "Defeat a creature Attuned to an element", magic("defeated"), None),
    ("archmage_slayer", "attuned_foe", item("radiance_essence"), "challenge", "Archmage Slayer",
     "Defeat an Attuned Archmage", magic("defeated", key="archmage"), None),
    ("rift_closer", "awakening", item("minecraft:crying_obsidian"), "goal", "Rift Closer",
     "Close an elemental rift: hold out against all three of its waves", magic("rift_closed"), None),
    ("the_arcanist", "awakening", item("arcane_lectern"), "task", "The Arcanist",
     "Buy something from an Arcanist, a villager who trades in magic", {
         "bought": {"trigger": "minecraft:villager_trade",
                    "conditions": {"item": {"items": "#" + NS + "arcanist_goods"}}}}, None),
    ("rumours_on_the_road", "the_arcanist", item("minecraft:filled_map"), "task", "Rumours on the Road",
     "Trade with a Wandering Mage", {
         "traded": {"trigger": "minecraft:villager_trade", "conditions": {"villager": {"type": NS + "wandering_mage"}}}}, None),
    ("bounty_hunter", "the_arcanist", item("bounty_contract"), "task", "Bounty Hunter",
     "Hand in an Arcanist's bounty at an Arcane Lectern", magic("bounty"), None),
    ("liquid_mana", "the_arcanist", item("minecraft:potion", potion=NS + "mana_draught"), "task", "Liquid Mana",
     "Brew a Mana Draught", {"brewed": {"trigger": "minecraft:brewed_potion",
                                        "conditions": {"potion": NS + "mana_draught"}}}, None),
    ("herbalist", "awakening", item("emberbloom"), "task", "Herbalist",
     "Gather an herb of every element", has(*((herb, None) for herb in HERBS)), None),
    ("kinship", "herbalist", item("minecraft:potion", potion=NS + "tonic_of_embers"), "task", "Kinship",
     "Brew a tonic from an herb of the elements", *any_of({
         tonic: {"trigger": "minecraft:brewed_potion", "conditions": {"potion": NS + tonic}}
         for tonic in (f"tonic_of_{name}" for name in TONICS)})),
    ("the_hollowed", "awakening", item("hollow_shard"), "task", "The Hollowed",
     "Defeat one of the Hollowed, who eat mana", killed("#" + NS + "hollowed"), None),
    ("shattered_hunger", "the_hollowed", item("hunger_obelisk"), "goal", "Shattered Hunger",
     "Break the Hunger Obelisk at the heart of a Hollowed camp", magic("obelisk"), None),
    ("silence_the_herald", "the_hollowed", item("hungerward_charm"), "challenge", "Silence the Herald",
     "Defeat a Hollow Herald", killed(NS + "hollow_herald"), None),
    ("glowing_company", "herbalist", item("bottled_glowmoth", "radiance"), "task", "Glowing Company",
     "Catch a glowmoth in a glass bottle", magic("glowmoth_bottled"), None),
    ("on_the_skyrays_wake", "skyward", item("minecraft:phantom_membrane"), "goal", "On the Skyray's Wake",
     "Glide on a skyray's wake, high over the sea", magic("skyray_wake"), None),
    ("crypt", "awakening", item("coffin"), "task", "Into the Crypt",
     "Go down into an arcane crypt, where the first mages lie", location(NS + "crypt"), None),
    ("runebreaker", "crypt", item("runestone"), "task", "Runebreaker",
     "Light a rune gate's runes and break its seal", magic("rune_gate"), None),
    ("laid_to_rest", "runebreaker", item("grave_flame"), "goal", "Laid to Rest",
     "Defeat a crypt's Revenant", killed(NS + "revenant"), None),
    ("relic", "crypt", item("ember_heart"), "goal", "Relic of the First Mages",
     "Bind a relic from a crypt to yourself", magic("relic"), None),
    ("tower", "awakening", item("tower_heart"), "task", "The Tower Wakes",
     "Enter a mage tower", location(NS + "mage_tower"), None),
    ("magister", "tower", item("guardian_core", "fire"), "goal", "Magister's Fall",
     "Defeat a tower's Magister and take its Guardian Core", killed(NS + "tower_magister"), None),
    ("master_staff", "magister", item("master_staff", "earth"), "task", "Staff of a Master",
     "Craft a Master Staff", has(("master_staff", None)), None),
    ("sanctum", "magister", item("sanctum_seal"), "task", "The Sanctums",
     "Find one of the four Sovereigns' sanctums", location("#" + NS + "sanctums"), None),
    ("sovereign", "sanctum", item("sovereign_heart", "fire"), "goal", "Sovereign's Fall",
     "Defeat a Sovereign", killed("#" + NS + "sovereigns"), None),
    ("archmage_staff", "sovereign", item("archmage_staff", "earth"), "task", "Staff of an Archmage",
     "Craft an Archmage Staff", has(("archmage_staff", None)), None),
    ("four_hearts", "sovereign", item("sovereign_heart", "water"), "challenge", "Four Hearts",
     "Hold the hearts of all four Sovereigns",
     has(("sovereign_heart", "fire"), ("sovereign_heart", "water"), ("sovereign_heart", "wind"), ("sovereign_heart", "earth")), None),
    ("prime_key", "four_hearts", item("prime_key"), "goal", "The Prime Key",
     "Forge the Prime Key from the four hearts", has(("prime_key", None)), None),
    ("into_the_hollow", "prime_key", item("minecraft:ender_eye"), "goal", "Into the Hollow",
     "Turn the Prime Key and go to the Hollow", {
         "entered": {"trigger": "minecraft:changed_dimension", "conditions": {"to": NS + "the_hollow"}}}, None),
    ("bound_again", "into_the_hollow", item("prime_heart"), "challenge", "Bound Again",
     "Defeat the Hollow and bind it once more", killed(NS + "the_hollow"), None),
]

TAGS = {
    "worldgen/structure/shrines": [NS + f"{e}_shrine" for e in ("fire", "water", "ice", "wind", "earth", "crystal", "lightning", "radiance")],
    "worldgen/structure/sanctums": [NS + f"{e}_sanctum" for e in ("fire", "water", "wind", "earth")],
    "item/arcanist_goods": ["#" + NS + "essences", NS + "arcanist_journal", NS + "apprentice_wand", NS + "adept_staff",
                            NS + "fire_catalyst", NS + "water_catalyst", NS + "wind_catalyst", NS + "earth_catalyst",
                            NS + "scroll_of_unbinding", NS + "tome_of_insight"]
                           + [NS + f"{tier}_{piece}" for tier in ("apprentice", "adept")
                              for piece in ("hood", "robe", "trousers", "boots")],
}


def main():
    folder = DATA / "advancement/arcana"
    folder.mkdir(parents=True, exist_ok=True)
    for old in folder.glob("*.json"):
        old.unlink()
    lang_entries = {}
    for name, parent, icon, frame, title, description, criteria, requirements in ADVANCEMENTS:
        display = {
            "icon": icon,
            "title": {"translate": f"advancement.elementalarcana.{name}.title"},
            "description": {"translate": f"advancement.elementalarcana.{name}.description"},
            "frame": frame,
            "show_toast": parent is not None,
            "announce_to_chat": parent is not None,
        }
        if parent is None:
            display["background"] = "minecraft:textures/block/amethyst_block.png"
        advancement = {}
        if parent:
            advancement["parent"] = f"{NS}arcana/{parent}"
        advancement["display"] = display
        advancement["criteria"] = criteria
        if requirements:
            advancement["requirements"] = requirements
        (folder / f"{name}.json").write_text(json.dumps(advancement, indent=2) + "\n")
        lang_entries[f"advancement.elementalarcana.{name}.title"] = title
        lang_entries[f"advancement.elementalarcana.{name}.description"] = description
    for path, values in TAGS.items():
        target = DATA / "tags" / f"{path}.json"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps({"values": values}, indent=2) + "\n")
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    lang = {key: value for key, value in lang.items() if not key.startswith("advancement.elementalarcana.")}
    lang.update(lang_entries)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"wrote {len(ADVANCEMENTS)} advancements, {len(TAGS)} tags, {len(lang_entries)} lang keys")


if __name__ == "__main__":
    main()
