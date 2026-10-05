# The Voice in the Stone

## Why

The world is full of things to do now, but nothing ties them together: the advancements list them,
the journal explains them, the ruins' lore books hint at a story. A fantasy adventure has someone
who sets you on the road, tells you why it matters, and is glad when you get there. This gives the
mod a main quest: a mentor's voice that leads a newly woken mage from their first shrine to the
Hollow, telling the old story as they go.

## The story

Caelith was the last Keeper, the mage whose pages turn up in ruins ("I have the four hearts. The
key is cold in my hand."). They carried the Prime Key into the Hollow to bind it again, and never
finished: they have held it back from the inside ever since, worn thin. The seals are failing
(that's why magic wakes in people, and rifts open). When your magic woke, it rang through the
Prime like a bell, and Caelith heard it: they speak to you through a **Sending Stone**.

## The Sending Stone

- Given at your awakening, with the journal (and once, on joining, to anyone already awakened).
  Lost: a smooth stone, an amethyst shard and any Essence make another.
- **Use it** to listen: a screen with Caelith's words for the chapter you're on, the task, what
  it's worth, and, once the task is done, Caelith's answer and a button to take the reward.
- When you finish a chapter's task, the stone grows warm ("The sending stone grows warm in your
  pack.", a chime).

## The chapters

Each chapter's task is an advancement the mod already has (done before you reach it: done at once).
Rewards: XP worth a level or more at your level, and something for the road ahead.

| # | Chapter | Task | Reward |
|---|---|---|---|
| 0 | A Voice in the Stone | listen | Seeker's Compass |
| 1 | Places of Power | find a shrine | 1 level, 4 Essence of your element |
| 2 | Drops of the Prime | defeat a wisp | 1 level, Binding Charm |
| 3 | Those Who Remember | buy from an Arcanist | 1 level, 6 emeralds |
| 4 | The Wardens Below | enter a crypt | 1 level |
| 5 | Laid to Rest | defeat a Revenant | 2 levels, Charm Pouch |
| 6 | The Tower Wakes | enter a mage tower | 1 level |
| 7 | Magister's Fall | defeat a Magister | 2 levels, Wishing Star |
| 8 | The Seals | find a sanctum | 1 level |
| 9 | Not Your Enemy | defeat a Sovereign | 2 levels, 2 golden apples |
| 10 | Four Hearts | hold all four hearts | 2 levels |
| 11 | The Key | forge the Prime Key | 2 levels, a totem of undying |
| 12 | Into the Hollow | enter the Hollow | 1 level |
| 13 | Bound Again | defeat the Hollow | 3 levels, an enchanted golden apple |

After the last, the stone is quiet ("It remembers my voice.").

## Code

| Piece | What it does |
|---|---|
| `api/MentorChapters` | The chapters: ids, tasks (advancement ids), rewards (tested) |
| `content/mentor/ModMentor` | The Sending Stone, the player's place in the tale (an attachment) |
| `content/mentor/Mentor` | Listening, claiming, the warm stone, giving the stone |
| `network/MentorPayload`, `MentorClaimPayload` | The chapter to the screen; a claim back |
| `client/MentorScreen` | Caelith's words, the task, the reward, the buttons |
| `/arcana mentor <chapter>` | Sets your chapter (for testing) |
| `tools/gen_mentor.py` | The stone's icon and recipe; the chapters' words in the language file |

## Testing

- `MentorChaptersTest`: ids unique, every chapter but the first has a task, rewards well formed,
  XP by level.
- `tools/autotest/mentor.txt`: the stone given at awakening (logged); listening (a shot); taking
  the compass (logged); a chapter's task granted (the warm stone logged), its answer (a shot),
  claimed (the reward logged); the last chapter and the quiet stone (a shot).
