# The Circle, part 2: Commissions and Marks

## Why

Part 1 gave the world an Enclave: a refuge to find, mages who greet you and fight beside you. But
nothing brings a player back once they've seen it. Part 2 gives the Archmagister work for the
player, deeds against the Hollow's servants and the world's great foes that grow with the player's
story, and gives the Circle a coin of its own, the **Mark of the Circle**, which buys what no one
else sells: above all the **Sigil of the Circle**, which calls a mage of the Circle to fight at the
player's side.

## Commissions

- **Use the Archmagister** (part 1 greets you):
  - Carrying a finished commission (anywhere in the inventory): the Archmagister takes it and pays
    its marks and some experience, with a word of thanks and the trade sound.
  - Carrying an unfinished one: the greeting, then a reminder ("Your commission stands: ...").
  - Carrying none, magic awake: the Archmagister hands one over ("I have work for you: ...").
  - Magic asleep: the greeting only; no work for a sleeping mage.
  - **Sneaking**: the Circle's stores (below).
- A **Circle Commission** is a sealed letter (white, a gold seal). Its name says the task; its
  tooltip shows the progress and the pay ("Break a Hunger Obelisk: 0/1 — 4 Marks of the Circle");
  a finished one glints. The Archmagister gives one at a time (any commission in the inventory
  counts, finished or not).
- The task is rolled from the player's stage (the one the Circle's talk follows, `CircleTalk`):

| Stage | Task | Count | Marks |
|---|---|---|---|
| Novice (awake, below level 15) | Slay the Hollowed (any of them) | 5 | 2 |
| Novice | Close an elemental rift | 1 | 2 |
| Novice | Slay Attuned creatures (any element) | 6 | 2 |
| Adept (15–34) | Break a Hunger Obelisk | 1 | 4 |
| Adept | Slay a Magus | 1 | 4 |
| Adept | Slay a Hollow Herald | 1 | 4 |
| Master (35–59) | Slay a tower's Magister | 1 | 7 |
| Master | Slay a crypt's Revenant | 1 | 7 |
| Master | Slay an Archmage | 1 | 7 |
| Archmage (60 and up) | Defeat a Sovereign | 1 | 12 |
| Archmage | Break Hunger Obelisks | 2 | 10 |
| Archmage | Slay Archmages | 2 | 10 |

- The experience paid is eight points (vanilla experience) per mark.
- **Counting**, as bounties count: a kill counts when the player (or their spell) lands the
  killing blow; an obelisk when the player breaks it; a rift for everyone near it when it closes.
  Every commission the player carries that fits counts.

## Marks of the Circle

- A coin of white gold with the Circle's sigil, stacking to 64. Only commissions pay them.
- **The Circle's stores**: sneak and use the Archmagister to open the trading screen (the vanilla
  one), priced in marks. Each trade can be made a few times and restocks each morning; there are
  no trade levels.

| Trade | Marks | Uses a day |
|---|---|---|
| Sigil of the Circle | 5 | 2 |
| Tome of Insight | 6 | 1 |
| Scroll of Unbinding | 3 | 1 |
| Hungerward Charm | 4 | 1 |
| Mana Draught ×3 | 1 | 4 |
| Elixir of Clarity ×2 | 2 | 2 |
| Wisp Mote ×3 | 1 | 4 |
| A rumour of a crypt, a sanctum, a sky isle (maps, one of each) | 2 | 1 |

## The Sigil of the Circle

- A one-use gold medallion. Use it, and a Circle Mage of a random element answers: a flash of
  light beside the player, and the mage fights at their side for **two minutes**.
  - It follows the player (keeping within six blocks; left more than 24 behind, it's beside them
    again).
  - It takes on whatever hurts the player or whatever the player strikes, and any monster within
    12 blocks of them; its magic spares them, as all the Circle's does.
  - Its time up (or the player gone, dead, or off to another dimension), it bows out in a flash
    of light.
- One at a time: a second sigil sends the first mage home and calls a new one (so the second
  sigil isn't wasted, it's only the element that changes).
- Its health and level are an Enclave mage's; it drops nothing and pays no experience if it falls.

## Also

- An advancement, **In the Circle's Service** (under The Circle): hand in a commission.
- A journal page: commissions and marks, the sigil.
- `/arcana commission <task> [done]` gives a commission (finished, with `done`) for testing.

## Code

| Piece | What it does |
|---|---|
| `api/CommissionRules` | Tasks by stage, their counts and marks (tested) |
| `content/circle/Commission` | A commission's terms (the `elementalarcana:commission` component) |
| `content/circle/CommissionItem` | The letter: name, tooltip, glint |
| `content/circle/Commissions` | Counting deeds; handing out and in at the Archmagister |
| `content/circle/CircleStores` | The Archmagister's trades |
| `content/circle/CircleMageEntity` | The Archmagister as a merchant (vanilla `Merchant`); a sigil's mage as a companion |
| `content/circle/CircleSigilItem` | The sigil |
| `tools/gen_circle.py` | The letter's, the mark's and the sigil's textures and models |

## Testing

- `CommissionRulesTest`: the tasks of each stage, counts and marks in range.
- `tools/autotest/circle_service.txt`: the Archmagister hands out a commission (counted), reminds
  of it when used again (no second one), takes a finished one (its marks counted), the stores
  opened (a shot) and a sigil bought (an AutoTest `trade` step picks a trade, as clicking it
  would), a sigil used (the mage beside the player, its time logged, a shot; it kills a husk; its
  time cut short, it bows out).
- `tools/autotest/mp_smoke.txt`: the stores opened on a client joined to a dedicated server.
