# Ember Sprite and Pyronado

Date: 2026-10-03
Status: built (milestone 12, the paused fire moves of the fire plan: Fireball was built; these are
its second and third moves).

**Ember Sprite** (Fire, level 3, 25 mana, 15 s). Tap R: a little fire spirit appears where you look,
up to 20 blocks away (just off the wall or ground it lands on). It's a fire wisp in miniature,
bobbing and shedding sparks. For 8 seconds it spits a small fire bolt (a Lv 1 fireball at half
power, drawn smaller) at the nearest hostile creature it can see within 10 blocks, once a second;
then it pops. The bolts are the caster's: they ignite, Melt the frozen and earn XP like any fire
spell. One sprite per caster; a new one sends the old one off. (`content/spell/EmberSprite`, an
entity of its own, drawn by `client/EmberSpriteRenderer`.)

**Pyronado** (Fire, level 8, 45 mana, 20 s). Tap R: three wheels of flame orbit you for 8 seconds,
3 blocks out and a third of a turn apart, each spinning upright along its path. Whatever you may
hurt that a wheel sweeps through takes 3 damage times your power, burns for 3 seconds and is
thrown back (each creature at most twice a second). You keep fighting while they spin
(`Pyronados`).

The hits are the server's; the wheels are drawn by each client. When they're called the server
sends one particle (`PyronadoOptions`), and `PyronadoEmitter` traces the rims and spokes from the
same geometry (`Pyronados.wheelCenter`, `rimPoint`) in plain pixel flames that live only three
ticks, with a small glow at each hub, anchored to the caster. Vanilla flames last up to two
seconds, so wheels drawn with them from the server smear into one ring around the caster; short
flames keep the three wheels apart, each with a short tail.

Pyronado replaces Flame Burst, the one-off ring of fire it grows out of. It keeps that spell's id
(`flame_burst`) and its place in the tree, so anyone who learned Flame Burst has Pyronado now. Ember
Sprite has a short arm of its own off the Fire start (Fire Affinity, then the spell).

Testing: `tools/autotest/fire_moves.txt` places a sprite before a row of husks (its bolts land), and
casts Pyronado among husks (they're burned and thrown).
