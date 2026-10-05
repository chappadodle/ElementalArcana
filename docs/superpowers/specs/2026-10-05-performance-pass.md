# Performance Pass

## Why

Before the mod is fit to release, its cost on a server has to be known: a magic mod that ticks
spells, spawners, auras and world events for every player can easily make a server stutter.

## How it was measured

`tools/autotest/perf.txt`, played by a client joined to the dedicated server (`tools/mp_test.sh`):
on a platform in fresh land, a mage of Fire, Lightning and Radiance (level 40) with two dozen
zombies closing in, two rifts open, a mana tide rising, and Pyronado, Stormcall and Dawnbreak cast
among them. `/neoforge tps` logged quiet and through the fight; the server's Flight Recorder
(`/jfr start`, `/jfr stop`) ran for the fight's 20 seconds. The recording's execution samples of
the server thread were read for the mod's own frames.

## What it found (2026-10-05)

| When | Overworld tick |
|---|---|
| Arriving in fresh land (chunks generating) | 10.8 ms |
| Quiet | 6.6 ms |
| The fight | 6.4 to 7.1 ms |

- The server held 20 ticks a second throughout: a tick never took more than about a seventh of
  its 50 ms.
- The fight cost no more than standing still: the time goes to vanilla work (mob AI and
  pathfinding, chunks), not the mod's.
- Of the server thread's samples through the fight, 4 in 147 (under 3%) had any of the mod's code on
  the stack, each in a different place (Smite's ticks, a rift creature climbing out, a death's
  reward, a wisp's wander goal): nothing to optimise.

## Repeating it

```bash
tools/mp_test.sh tools/autotest/perf.txt
```

Tick times are in `build/autotest.log` (the chat), and the recording in `run-server/debug/`
(`jfr print --events jdk.ExecutionSample` lists the samples).
