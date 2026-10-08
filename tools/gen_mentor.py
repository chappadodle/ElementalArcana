#!/usr/bin/env python3
"""Generates The Voice in the Stone's art, data and words
(docs/superpowers/specs/2026-10-05-voice-in-the-stone-design.md):

- the Sending Stone's icon (a smooth river stone with a glowing rune) and item model, its recipe;
- in en_us.json, Caelith's words for each chapter (mentor.elementalarcana.<chapter>.title, .text
  while its task is open, .done once it's done, .task), and the screen's own words. Every
  mentor.elementalarcana.* and screen.elementalarcana.mentor.* key is rewritten.

The chapters' ids, tasks and rewards are in api/MentorChapters; keep the ids here in step.

Run from the project root:  python3 tools/gen_mentor.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
LANG = ASSETS / "lang/en_us.json"
NS = "elementalarcana:"

# id: (title, text while the task is open, task, Caelith's answer once it's done)
CHAPTERS = {
    "voice": (
        "A Voice in the Stone",
        "Can you hear me? Good. When your magic woke, it rang through the Prime like a bell, and I heard it from "
        "where I am.\n\nMy name is Caelith. Long ago I carried a key into the Hollow, and I am there still. I'll "
        "explain, in time. First, take this: a compass that finds what I tell you to look for.",
        "Listen",
        None),
    "shrine": (
        "Places of Power",
        "Before the elements there was one magic, the Prime. In a few places it still runs close to the surface, "
        "and there the old mages built shrines.\n\nFind one. Sneak and use the compass until it seeks Shrines, then "
        "use it, and follow the needle.",
        "Find an elemental shrine",
        "There. Do you feel it hum? That is what the world was like once, everywhere.\n\nA shrine will bless you "
        "if you give it what it asks. Remember the way back."),
    "wisps": (
        "Drops of the Prime",
        "You'll have seen the wisps: little lights that drift and sting. They are drops of the Prime that never "
        "broke clean, and they guard what is theirs.\n\nDefeat one. Learn how your magic answers theirs.",
        "Defeat a wisp",
        "Not so hard, was it? A wisp worn thin enough will listen to you. Take this charm: bind one, and it will "
        "follow you as a lantern follows a hand."),
    "arcanist": (
        "Those Who Remember",
        "You are not the only one. In the villages there are Arcanists, who keep what little we wrote down and "
        "trade in magic.\n\nFind one and buy something. They will know you for what you are.",
        "Buy something from an Arcanist",
        "They are good people, the Arcanists. Their bounties will keep your purse full and your spells sharp.\n\n"
        "Now. It's time I told you about the dead."),
    "crypt": (
        "The Wardens Below",
        "We did not bury our dead. We set them to watch, deep in crypts sealed with the runes of their element. "
        "Many of my friends are down there.\n\nFind a crypt and go in. Light the runes with what they answer to.",
        "Go down into an arcane crypt",
        "Walk softly. The halls were warded against thieves, and in each crypt one warden does not sleep: the "
        "first, who swore to keep it until the Hollow was gone."),
    "revenant": (
        "Laid to Rest",
        "The first warden's oath outlived its flesh. It keeps a flame at the head of its tomb and knows no one "
        "now, not even me.\n\nLay it to rest. It has watched long enough.",
        "Defeat a crypt's Revenant",
        "Thank you. I knew that one, once.\n\nWhat it guarded is yours: the relics of the first mages. You'll want "
        "somewhere to keep them. Take this pouch."),
    "tower": (
        "The Tower Wakes",
        "When the seals were whole, our order kept towers, and Magisters to guard them. They stood empty a long "
        "time. Something has woken them.\n\nFind a mage tower, and climb it.",
        "Enter a mage tower",
        "Mind the stairs. The Magister at the top won't remember the order it served. It knows only its tower, "
        "and that you don't belong in it."),
    "magister": (
        "Magister's Fall",
        "Defeat the Magister and take its Guardian Core, a heart of pure element: the stuff a master's staff is "
        "made of.\n\nYou are ready for this. I wouldn't send you if you weren't.",
        "Defeat a tower's Magister",
        "Well done. Truly.\n\nNow you're ready to hear the rest: why I'm where I am, and why the seals are failing."),
    "hollowed": (
        "The Hollowed",
        "First, the ones who want the seals to fail. When the hunger stirred, some heard it and answered. It hollowed "
        "them out: they make no mana now, they eat it. They camp where the seals are thin, round obelisks that drink at "
        "the cracks, and at dusk they hunt people like you.\n\nDefeat one of the Hollowed. Keep your mana close.",
        "Defeat one of the Hollowed",
        "Now you know what we're up against: not only a hunger, but people who feed it. Break their obelisks where you "
        "find them, and keep their shards: a ward made of the Hollow's own dark keeps it from eating you. Here, I had "
        "one made.\n\nBut you won't stand against them alone."),
    "circle": (
        "The Ones Who Stayed",
        "Not all of my order slept or fell. Some kept the oaths in the open, generation after generation: the Circle. "
        "They keep Enclaves, walled gardens round a white Spire, far apart and hard to find.\n\nFind one. Sneak and use "
        "the compass until it seeks Enclaves. Tell their Archmagister I sent you; they'll know the name.",
        "Find an Enclave of the Circle",
        "They're still keeping the oaths. I didn't dare hope.\n\nThe Archmagister will have work for you, and their "
        "mages will stand with you against the Hollowed. Take these: the Circle's own coin, so you don't come "
        "empty-handed."),
    "forge": (
        "Where Fire Was Worked",
        "Our staves and robes were not made up here. Down in the Nether, over the lava sea, our smiths kept forges, "
        "and the fire there was older than ours.\n\nGo through to the Nether and find a Cinder Forge. The compass knows "
        "them there. Its keeper won't know you: be ready.",
        "Find a Cinder Forge",
        "The forge still burns, after all this time. The anvil on its dais can temper what you carry: an Ember Core "
        "and a little of your strength, and your staff or your robes will hold more.\n\nTake this core for your "
        "first. Then, when you're ready: the seals themselves."),
    "sanctum": (
        "The Seals",
        "When we broke the Prime, the Hollow choked on the pieces and slept. Four of us swore to keep it sleeping: "
        "Flame, Tide, Storm and Stone. They gave up their names and became the Sovereigns.\n\nFind one of their "
        "sanctums.",
        "Find a Sovereign's sanctum",
        "Their seals are thin. That's why magic wakes in people like you, and why the rifts tear open.\n\nGo in. "
        "The Sovereign will fight you: it's how it knows you're ready."),
    "sovereign": (
        "Not Your Enemy",
        "A Sovereign is not your enemy. It is a test, and a lock: only one who has bested all four can carry the "
        "key that opens the way to the Hollow.\n\nDefeat a Sovereign, and take its heart.",
        "Defeat a Sovereign",
        "One heart. It's warm, isn't it? It remembers the oath.\n\nThree more, and you'll have done what I did, so "
        "long ago."),
    "hearts": (
        "Four Hearts",
        "Flame, Tide, Storm and Stone. Each seal holds while its Sovereign stands, and each heart is a promise "
        "kept.\n\nGather all four.",
        "Hold the hearts of all four Sovereigns",
        "Four hearts. Now you know why I'm still here: I took the key into the Hollow to bind it again, and I "
        "didn't finish. I've held it ever since.\n\nI am so tired."),
    "key": (
        "The Key",
        "Forge the four hearts into the Prime Key. It's cold in the hand. Mine was.\n\nWhen you turn it, the way "
        "opens. I'll be on the other side.",
        "Forge the Prime Key",
        "That's it. That's the key.\n\nBefore you turn it, rest. Mend your gear. Learn what undoes each shape the "
        "Hollow wears: fire, tide, storm and stone."),
    "hollow": (
        "Into the Hollow",
        "Turn the key. I'll keep it from waking fully until you're through. That much I can still do.",
        "Go into the Hollow",
        "You're here. I can see you. Don't look for me: I'm part of the walls by now.\n\nIt wears every shape it "
        "ever ate. Strike each with what undoes it. Finish it."),
    "bound": (
        "Bound Again",
        "Finish it. Bind it again. I'll hold on until you do.",
        "Defeat the Hollow",
        "It's quiet. I'd forgotten quiet.\n\nThank you. The seals will hold now, for a long while. The stone will "
        "go quiet too, but keep it. It remembers my voice."),
    "beyond": (
        "Beyond the Last Sky",
        "One more thing, while the stone still carries me. Before the seals, before the Hollow, we read the stars. "
        "Out past the End's dragon, on the far isles, we built observatories, and charted the sky that's under all "
        "skies.\n\nFind one. Wake its orrery: turn its lenses to the chart in its floor. I'd like to know they're "
        "still there.",
        "Wake the orrery of an observatory on the End's far isles",
        "They're still there. The lenses still turn.\n\nThat's all I wanted. Keep the stars for me. Goodbye, and "
        "thank you, truly."),
}

SCREEN = {
    "speaker": "Caelith",
    "chapter": "Chapter %s of %s",
    "task": "Task: %s",
    "task_done": "Done: %s",
    "reward": "Reward:",
    "xp": "+%s XP",
    "claim": "Take it",
    "farewell": "Farewell",
    "quiet.title": "Quiet",
    "quiet.text": "The stone is warm in your hand, and silent.\n\nIt remembers a voice.",
}

STONE = [
    "................",
    "................",
    "......oooo......",
    "....ooLLLLoo....",
    "...olhhLLLLdo...",
    "..olhLLLrLLLdo..",
    "..olLLLrRrLLdo..",
    ".olLLrLLrLLrLdo.",
    ".oLLLLrrRrrLLdo.",
    ".oLLLLLLrLLLLdo.",
    ".oLLLLLrRrLLddo.",
    "..oLLLLLrLLLdo..",
    "..odLLLLLLLddo..",
    "...oddLLLLddo...",
    ".....oooooo.....",
    "................",
]
STONE_COLORS = {
    "o": (52, 52, 60), "d": (96, 96, 108), "L": (134, 134, 146), "l": (164, 164, 176), "h": (196, 196, 208),
    "r": (176, 120, 255), "R": (240, 214, 255),
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def rows(grid, colors, size=16):
    img = np.zeros((size, size, 4), dtype=np.uint8)
    assert len(grid) == size and all(len(r) == size for r in grid), [len(r) for r in grid]
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in colors:
                img[y, x] = (*colors[ch], 255)
    return Image.fromarray(img, "RGBA")


def main():
    rows(STONE, STONE_COLORS).save(ASSETS / "textures/item/sending_stone.png")
    write_json(ASSETS / "models/item/sending_stone.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/sending_stone"}})
    write_json(DATA / "recipe/sending_stone.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "minecraft:smooth_stone"}, {"item": "minecraft:amethyst_shard"}, {"tag": NS + "essences"}],
        "result": {"id": f"{NS}sending_stone", "count": 1}})
    entries = {}
    for chapter, (title, text, task, done) in CHAPTERS.items():
        entries[f"mentor.elementalarcana.{chapter}.title"] = title
        entries[f"mentor.elementalarcana.{chapter}.text"] = text
        entries[f"mentor.elementalarcana.{chapter}.task"] = task
        entries[f"mentor.elementalarcana.{chapter}.done"] = done if done is not None else text
    for key, value in SCREEN.items():
        entries[f"screen.elementalarcana.mentor.{key}"] = value
    lang = json.loads(LANG.read_text(encoding="utf-8"))
    lang = {key: value for key, value in lang.items()
            if not key.startswith("mentor.elementalarcana.") and not key.startswith("screen.elementalarcana.mentor.")}
    lang.update(entries)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"mentor art and data written, {len(entries)} lang keys")


if __name__ == "__main__":
    main()
