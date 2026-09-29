# Bubble Prison: design

Date: 2026-09-29
Status: approved in conversation (from the water plan; targets and popping decided on 2026-09-29)

## The spell

Water, Magic Level 3, single level. Costs 30 mana, with a 12 s cooldown. Tap R and the creature
under your crosshair (up to 20 blocks) is trapped in a floating water bubble for **4 s**.

- It rises 1.2 blocks over the first 0.4 s, then hangs there, helpless, and it is **Wet** (so combos
  work: bubble then Icicle = Freeze, bubble then Fireball = Vaporize).
- **Any hit from a player pops it:** that hit deals **+4 bonus damage**, and the target drops.
  Otherwise it pops by itself after 4 s.
- **The look:** a see-through wobbling 3D water sphere around the target, sized to it, with bubble
  and drip particles. Sounds are vanilla bubble-column sounds, with a splash on pop.
- **No target under the crosshair:** the cast fails with a message, and no mana or cooldown is
  spent.

## Who can be trapped

Any creature, including Attuned ones (they can't cast while trapped), **and other players**. The
exceptions are:
- bosses (the `c:bosses` tag: Wither, Ender Dragon, Warden, Elder Guardian…);
- players in creative or spectator mode;
- anyone already in a bubble.

## How trapping works

- **Mobs:** their AI is switched off (`NoAI`) while trapped, so they can't move, attack or cast
  spells. The server holds them at the bubble's point each tick. A saved marker turns the AI back
  on if the server stops mid-bubble.
- **Players:** Minecraft moves players on their own client, so the trapped player's client holds
  them at the bubble's point. The server teleports them back if they drift more than 1.5 blocks,
  and cancels their attacks, item use and block interaction. Spells are refused with "You're
  trapped in a bubble".
- **Data:** the bubble is a synced (not saved) attachment `Bubble { anchor, startTick, endsAt }`.
  Every client can draw it, and the trapped player's client can hold them in it.
