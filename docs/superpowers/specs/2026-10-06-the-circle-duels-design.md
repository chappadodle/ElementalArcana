# The Circle, part 3: Duels

## Why

Every Enclave has a duelling ring, and so far it stands empty. Part 3 puts it to use: a player can
challenge the Circle's mages to a **duel**, a fair, bounded fight in the ring that no one dies of.
It's practice against a real caster of an element, a measure of how far the player has come, and
one more way to earn Marks of the Circle, ending with the Archmagister, who duels only those who
have beaten the Circle's mages.

## A duel

- **The challenge**: use a Circle Mage while holding a **Mark of the Circle**: the mark is the
  wager. The mage takes it and goes to the ring (walking, or stepping there by magic if it hasn't
  arrived in ten seconds); the player has 30 seconds to step in, or the mage gives the mark back.
  Each mage (not a sigil's) duels once a day, win or lose, against anyone, so an Enclave has four
  or five bouts a day.
- **The Archmagister** takes the same challenge from a player who has won three duels (before
  that, they decline, and the mark stays the player's), stepping down into the ring by magic.
- **The bout**: once both are in the ring, a bell, a three-second count in the middle of the
  screen, a bell, and they fight: the mage casts its element's spells at the player, and the
  player's spells hit it (for the length of the bout, and between the two of them only, a duel
  sets aside the rule that the Circle's magic and a player's spare each other). Others' spells
  still spare both.
- **Yielding**: no one dies in a duel. A blow that would bring a duellist below a fifth of their
  health brings them to that fifth instead, and they yield: the bout ends.
- **Out of the ring**: a player who steps out of the ring (or goes farther than 12 blocks from its
  middle) yields. The mage keeps to the ring.
- **Time**: two minutes; then it's a draw.
- **The end**: a bell; the mage says a word (a winner's, a loser's or a draw's), heals to full
  and goes back to its post (the Archmagister back to the study, by magic).
  - **The player wins**: the wager back, Marks of the Circle (2 from a mage, 5 from the
    Archmagister) and experience (eight points a mark won); their duel wins are counted.
  - **A draw**: the wager back.
  - **The player yields**: the mage keeps the wager; nothing else is lost.
- A player in a duel can't start another; a logout, a death (from something else), or a trip to
  another dimension ends it as a yield.

## Also

- Advancements: **First Blood, Politely** (win a duel; under The Circle) and **The Archmagister
  Yields** (win a duel against the Archmagister; a challenge).
- A journal page on duels.
- `/arcana duels <n>` sets how many duels a player has won (for testing).

## Code

| Piece | What it does |
|---|---|
| `api/DuelRules` | The yield share, the time, the reward by opponent, the wins the Archmagister asks for (tested) |
| `api/SpellTargets` | A duel's two are fair game for each other (a hook content code sets) |
| `content/circle/Duels` | The bouts: challenge, the wait for the ring, the count, yielding, the end; events |
| `content/circle/CircleMageEntity` | The challenge (a mark held out); in a bout, the player is its only target and the ring its bounds |
| `content/circle/ModCircle` | The duel wins a player has (an attachment, kept through death) |

## Testing

- `DuelRulesTest`.
- `tools/autotest/circle_duel.txt`: a mage challenged with a mark (it goes to the ring), the player
  steps in, the count (a shot), the bout: the player's Fireball hurts the mage, the mage's spells
  hurt the player (health logged); the mage brought to a fifth yields (marks counted); a second
  challenge the same day is refused; another mage challenged and the player steps out: a yield
  (the wager kept, counted); the Archmagister declines a player with one win, accepts one with
  three, steps into the ring by magic and, the player stepping out, back to the study (where
  logged). A mage that stopped walking is faced only after a few ticks (the client sees where it
  stopped late); player data persists between runs, so the test heals the player and sets the
  wins first.
