# JEI integration

Date: 2026-10-03
Status: built (a companion mod, as the user allowed: "if you think there are any mods that would
work well with the one you are making, you can use them").

Most of the mod's progression runs through crafting (wands, staves, robes, Catalysts, the Prime Key)
and through things found in the world (Essence, Cores, Hearts, Wisp Motes). JEI is the recipe viewer
most players already have, so the mod speaks to it, optionally: the mod needs nothing from JEI and
runs the same without it.

## What it does (`compat/jei`)

- **Element variants are separate items** (`ElementSubtypes`, keyed on the element component):
  every element's Apprentice Wand, Adept, Master and Archmage Staff, Guardian Core and Sovereign
  Heart has its own entry, and looking up a Fire Archmage Staff shows the Fire recipe only. Without
  this JEI would fold all eight into one entry and show every element's recipe for each.
- **Info pages** for what comes from the world rather than a crafting table, from the README's own
  facts: Essence (where it drops and what it's for), Catalysts, the Wisp Mote, Guardian Cores,
  Sovereign Hearts, the Prime Key, the Heart of the Prime, the Arcane Lectern, the Scroll of
  Unbinding, the Tome of Insight and the Arcanist's Journal (`jei.elementalarcana.info.*`).
- Recipes need nothing special: they're all vanilla crafting (with NeoForge component ingredients
  for the element variants) and brewing, which JEI reads itself.

## Build

JEI 19.57.0.450 for NeoForge 1.21.1, from Modrinth's Maven (the source Jade already comes from):
`compileOnly` to build the plugin and `localRuntime` so the dev game has it. It's pinned by its
Modrinth version id (`Tn0dgwL0`) because the Forge and Fabric builds share the version number. The
mods.toml lists it as an optional dependency (19.0 and up). JEI finds `ArcanaJeiPlugin` by its
annotation; nothing else in the mod refers to it.

## Testing

`tools/autotest/jei.txt`, with two dev-only steps (`jei_filter`, `jei_show`) that reach JEI's
runtime through `JeiHooks` (and do nothing without JEI): the mod's items in JEI's list (all 32 wands
and staves), "staff" (24, three tiers of eight elements), the Sovereign Heart's and Essence's info
pages, and the Fire Archmage Staff's recipe (one, its own). The dedicated server starts with JEI
loaded without errors (`tools/server_tests/smoke.txt`).
