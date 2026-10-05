# Trophies of the Wild II

## Why

The second three Creatures of the Wild leave Harpy Plumes, Prism Cores and Bog Pearls. As with the
first three, each makes a charm that teaches its creature's trick, carried anywhere in the
inventory. Together the six make getting around the world itself a kind of magic: up cliffs,
across gaps, under lakes.

## The charms

### Plume of the Gale (Wind)

Two Harpy Plumes, a Wind Essence and a feather. Carried:
- **Gale leap**: a jump while sprinting is carried by a gust: you go twice as far and a little
  higher (a puff of wind behind you). Falls of up to 8 blocks can't hurt you.

### Crawler's Prism (Crystal)

Two Prism Cores, a Crystal Essence and an amethyst shard. Carried:
- **Wall climbing**: walking into a wall, you climb it, as a crawler does (sneak to hold still on
  it). Not while flying or swimming.

### Bog Pearl Charm (Water)

Two Bog Pearls, a Water Essence and a slimeball. Carried:
- **Lurker's lungs**: you breathe under water, and swim half again as fast.

## Code

| Piece | What it does |
|---|---|
| `api/WildTrophyRules` | The leap's boost, the climb speed, the swim boost (tested) |
| `content/wild/ModWild` | The three charms (WildCharmItem) |
| `content/wild/WildCharms` | Breath and swim speed, the fall ward (server) |
| `client/WildCharmsClient` | The leap and the climb (the local player's own movement) |
| `tools/gen_wild.py` | Icons and recipes |

## Testing

- `WildTrophyRulesTest` (more cases).
- `tools/autotest/wild_trophies_2.txt`: a sprint-jump with the plume (distance logged against one
  without), climbing a wall with the prism (height logged), and swimming under water with the pearl
  (air supply and speed logged).
