# The Hollow

Date: 2026-10-03
Status: built (milestone 11, second half, of `2026-10-02-fantasy-adventure-roadmap.md`; the first
half is `2026-10-03-sanctums-and-sovereigns-design.md`).

The story's end: the Hollow, the hunger that devours mana, was bound by the four elements. Its
seals have faded. With the four Sovereigns' hearts the player can go to it and bind it again.

## The way: the Prime Key

`prime_key`: the four Sovereign Hearts (Fire above, Water left, Wind right, Earth below, each matched
by its element component) around an eye of ender, amethyst shards in the corners. Hold it for two
seconds (like eating; the key hums and the air tears in front of you) and it is spent: you, and
anyone within 6 blocks of you, are taken to **the Hollow**. It only works in the Overworld.

## The Hollow (the place)

A dimension of its own (`elementalarcana:the_hollow`, data-driven): nothing but void under a dead
sky (the End's), always night, with drifting ash. No creatures spawn there; beds and anchors don't
work; creatures that come to be there (the Hollow's wisps) are as strong as the End's. Being a
datapack dimension, it makes Minecraft warn once about experimental settings when a world first
loads with it (NeoForge remembers the answer). In its middle hangs the **Ruin**, an island of blackstone and obsidian 49 blocks across, built
the first time anyone arrives (`HollowArena`, saved with the level):

- cracked floor with veins of crying obsidian;
- four broken pillars on the diagonals (so arrivals look past them at the maw), each holding a
  restored Sanctum Seal of one element (the four seals that bind it, lit);
- the Hollow's maw in the middle: a ring of obsidian, then one of crying obsidian, around a pit
  three deep, floored with crying obsidian.

The Ruin carries a version: when it changes, a world's older Ruin is cleared and built again.

Arrivals land on the island's south edge. Whoever is thrown off the island isn't left to the void
(nor their things): the Hollow spits them back onto it at the arrival point, with 4 hearts of its
hunger's damage. Whoever dies in the Hollow wakes at home as usual.

## The Hollow (the hunger)

`HollowEntity`: the hunger made a shape, a Sovereign's form gone black (the Sovereign model at
three times size: a void mask, a violet core). It rises from the maw when a player arrives and it
is not already there. Level 70 (its own boss bar, "The Hollow", purple, darkening the sky), about
1020 health (670 before Vitality: the game caps health at 1024), armour 12, no knockback, immune
to fire, drowning and falls.

- **It wears what it ate.** It fights in four forms, a quarter of its health each: Fire, Water, Wind,
  then Earth. In each it is Attuned to that element, so the element chart decides what hurts it
  (Water beats its Fire form, Earth its Wind form...), it casts that element's spells and both of
  that Sovereign's signatures, and its core and halo burn in that colour. Each change of form is a
  **Devouring**: it pulls everyone within 16 blocks toward it, then bursts (6 damage, thrown back),
  and announces its new form.
- **Mana Hunger:** while it lives, everyone within 24 blocks loses 5 mana a second (most of what
  a level-60 mage regains); whoever has none left loses half a heart instead. Mana Draughts and
  Clarity matter here.
- If no one is left to fight, it sinks back into the maw and heals.

When it falls the seals are whole again: a title for everyone in the Hollow ("The Hollow is bound"),
an epilogue line, and for each of them a **Heart of the Prime**. Ten seconds later
the Hollow lets them go: everyone there is taken home (their spawn point, or the world spawn). The
next key can find it again; the hunger never quite ends.

## The Heart of the Prime

`prime_heart`: use it (once) and your magic remembers that it was one: every element you don't
hold yet awakens, opposites included, and you get 5 tree points. Its tooltip says so.

## Testing

- Unit: the form for each share of health, the drain rule (`api/HollowRules`).
- Server: the Hollow summons in its own dimension (level 70), its forms change at 75/50/25% with
  the right Attunement, it drops a Heart of the Prime per player.
- In game (`tools/autotest/hollow.txt`): the key used in the Overworld, the arrival, the Ruin, the
  fight in all four forms, the victory and the way home, the Heart of the Prime used.
