# Skill tree: notables and keystones

Date: 2026-10-02
Status: built (milestone 9 of `2026-10-02-fantasy-adventure-roadmap.md`).

The skill tree had small stat nodes, spells and spell paths. Builds now differ more: every region
ends one arm in a **notable** and a **keystone**, and the core ring has one more of each.

## Notables

A larger node, gold-ringed in its main stat's colour, that gives several stats at once
(`type: notable`, `stats: {key: amount}`). Taking one costs a single point, like any node, so the
arms that lead to them are worth walking.

| Notable | Where | Gives |
|---|---|---|
| Kindled Soul | Fire, after the ember arm | +6 Fire Affinity, +4 Potency |
| Deep Reserves | Water, on a new arm | +6 Reservoir, +4 Vitality |
| Tailwind | Wind, on a new arm | +6 Wind Affinity, +4 Focus |
| Stone Blood | Earth, on a new arm | +6 Vitality, +4 Ward |
| Winter's Edge | Ice, past Frost Nova | +6 Water Affinity, +4 Potency |
| Arcane Vigor | the core, out past the ring between Fire and Water | +4 Reservoir, +4 Vitality, +2 Potency |

## Keystones

The biggest nodes: purple, double-framed, with a gem in the middle (`type: keystone`, `keystone:
<id>`). Each changes a rule of magic, for better and for worse, so it shapes a whole build. Each sits
just past its region's notable.

| Keystone | Where | Better | Worse |
|---|---|---|---|
| Glass Cannon | Fire | spells hit 30% harder | 30% less max health |
| Wellspring | Water | mana regenerates 60% faster | 25% less max mana |
| Gale Step | Wind | 15% faster, never any fall damage | every element hurts you 15% more |
| Mountain's Heart | Earth | 40% more max health, no knockback | 15% slower |
| Winter's Grasp | Ice | spells hit frozen and frosted creatures 30% harder (ice spells frost as they hit) | fire hurts you 40% more |
| Blood Magic | the core, past Arcane Vigor | spells and conjured upkeep cost health instead of mana (a heart per 20 mana), never Mana Sickness | your mana goes unused, so every cast is a wound |

The numbers live in `api/Keystones`. They're applied where each rule lives:
- spell power, max mana and regen in `MagicData`;
- max health, speed and knockback in `PlayerStats`;
- Blood Magic in `CastingService` and `Conjuring`;
- damage taken and dealt in `ElementalMatchups`;
- Gale Step's landings in `KeystoneEvents`.

## Compatibility

Existing node ids and positions are unchanged (the generator only adds), so trees saved before this
load as they were. Refunds and the Scroll of Unbinding work on notables and keystones like any node.

## Testing

- `NotablesAndKeystonesTest`: grants sum notable stats and collect keystones; each keystone's
  numbers.
- `tools/autotest/keystones.txt`: Glass Cannon (max health 20 to 14), Blood Magic (a 15-mana cast
  takes 1.5 health, mana untouched), and the tree screen with the new nodes.
