# Gear I: foci and robes

Date: 2026-10-02
Status: built (milestone 3 of `2026-10-02-fantasy-adventure-roadmap.md`).

## One stat system

Gear adds stat points to the same stats as stat points and skill tree nodes (`StatGear`, worked out
by `GearStats` once a second on the server and synced with the player's magic data). The Stats
window shows a stat's points and, after them, what the tree and gear add. Skill tree requirements
count only points and tree nodes, so taking off gear never strands a node.

## Foci (held in either hand; only the better of two counts)

Attuned to one element through the `elementalarcana:element` component, set by the recipe. The
gem takes the element's colour.

| Focus | Level | Adds | Recipe |
|---|---|---|---|
| Apprentice Wand | any | +2 Potency, +3 Affinity of its element | 2 sticks and 1 Essence of the element |
| Adept Staff | 15 | +4 Potency, +2 Focus, +6 Affinity of its element | an Apprentice Wand (any element), 2 Essence of the element, an amethyst shard and a gold ingot |

## Robes (worn; element-neutral)

| Set | Level | Armor (boots, trousers, robe, hood) | Adds (hood / robe / trousers / boots) | Recipe |
|---|---|---|---|---|
| Apprentice | any | 1, 2, 3, 1 | Insight +1 Reservoir +1 / Reservoir +3 Ward +1 / Focus +2 / Vitality +1 Reservoir +1 | wool and any Essence, in the shape of leather armor |
| Adept | 15 | 1, 4, 5, 2 (toughness 0.5) | Insight +2 Reservoir +2 / Reservoir +5 Ward +3 / Focus +4 Ward +1 / Vitality +2 Reservoir +2 | an apprentice piece ringed by gold, Essence and an amethyst shard |

Below the required level, a piece does nothing (its tooltip says what it needs).

## The master tier (level 35)

Made with the Guardian Cores that tower Magisters drop (see `2026-10-02-mage-towers-design.md`).

| Item | Recipe | Adds |
|---|---|---|
| Master Staff | an Adept Staff, a Guardian Core, 2 diamonds and 2 Essence, all of one element (the core and staff matched by their element component) | +7 Potency, +4 Focus, +10 Affinity of its element |
| Master's hood / robe / trousers / boots | the adept piece, a Guardian Core of any element and 2 gold ingots | Insight +3 Reservoir +3 / Reservoir +8 Ward +5 / Focus +6 Ward +2 / Vitality +3 Reservoir +3 |

Master robes are midnight blue with gold, with armor 2, 5, 6, 2 (boots, trousers, robe, hood) and
toughness 1.

## Later

Archmage gear from the Sovereigns (milestone 11).
