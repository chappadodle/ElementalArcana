# Wisp Familiars

## Why

A mage in a fantasy story rarely walks alone. Wisps are already the mod's most characterful
creatures, drops of pure element drawn to mana; a mage who has bested one should be able to bind
one. A familiar is a companion that follows you, fights beside you with its element's magic and
grows with you, and it makes the world's wisps something to seek out, not just Essence on the wing.

## Binding one

- A **Binding Charm** (crafted: an amethyst shard, a Wisp Mote and two Essence of any element,
  shapeless) binds a wisp. Use it on a wild wisp that's been worn down to a quarter of its health
  or less: the charm is used up, the wisp stops fighting and becomes your **familiar** (a chime, a
  ring of its element's light, and its name over its head: "Fire Wisp (Steve's familiar)").
- Only a mage can bind one (your magic must have woken), and only one familiar at a time: binding
  a new one releases the old (it drifts away, wild again but peaceful to you).
- A shrine's guardian wisps can't be bound (they belong to the shrine).

## What a familiar does

- **Follows** you, floating at your shoulder, and teleports to you if it falls 24 blocks behind
  (like a tamed wolf). It doesn't despawn, and it's saved with the world.
- **Fights** what you fight: whatever you hurt, and whatever hurts you, as long as it's something
  your magic may hurt (never players, never your other pets, never villagers or golems). It casts its
  element's creature spells (the same ones Attuned creatures cast) at its rank: an Adept's at first.
- **Grows** with you: it takes the rank Magus when your magic reaches level 20 and Archmage at 40,
  casting the stronger spells those ranks know, and its health grows with its rank.
- **Rests:** sneak and right-click it with an empty hand to make it stay (it hovers in place) or
  follow again. Right-click it with a Wisp Mote to heal it fully.
- If it dies it leaves a Wisp Mote and its element's Essence; bind another.

Your magic now spares your own pets, too: area spells (splashes, bursts, whirlpools) no longer
hurt your tamed wolves, cats, horses or your familiar, and your spells' projectiles pass through
them.

## Code

| Piece | What it does |
|---|---|
| `api/FamiliarRules` | Binding (health share), rank by the owner's level, follow and teleport distances (tested) |
| `content/creature/FamiliarEntity` | The familiar: a wisp of an element with an owner; follow, stay, fight beside its owner |
| `content/creature/BindingCharmItem` | The charm, and binding a wild wisp |
| `api/SpellTargets`, `api/SpellProjectile` | A player's magic, and their familiar's, spares their pets; their projectiles pass through them |
| data/assets | The charm's recipe, texture and model; lang |

## Testing

- `FamiliarRulesTest`: binding thresholds, rank by level, distances.
- `tools/autotest/familiars.txt`: a wisp worn down and bound, following, fighting a zombie beside
  its owner, sitting, healing with a mote; a fireball's splash sparing it.
