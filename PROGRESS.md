# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game tests counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **What it does so far:** a dragon you can summon (`/summon dracora:dragon` or its spawn egg). It
  walks, looks around, and sits or follows when tamed; its owner's empty hand sits it down and
  stands it up. Its colour comes from the biome it appears in (a first draft of each palette; End
  colours are separate and never appear in the Overworld). Fabric and NeoForge on 26.3. The
  texture, spawn egg and icon are placeholders until Gabby's art.
- **Broken:** nothing known. The model is a first pass: folded wings stand up beside the body as
  tall sheets, which reads as resting but blocky.
- **Next:** session 2: the egg block, hatching near warmth, bonding to the player who hatches it.
  Flight (session 3) borrows ideas from vanilla's Ender Dragon (written our own way, not copied):
  a three-part wing beat with the tips lagging, a neck and tail that trail through turns, the jaw.

## 0.1.0 scope (locked, 2026-10-21)

Everything below ships together in 0.1.0 (Gabby's call). New ideas go to "Later", never here.

- Overworld nests: rare, on high peaks and in old-growth forests, one egg each.
- End nests on remote outer islands, with the End palette (violets, blacks, shimmer).
- A shy wild parent near each nest: sprinting, attacking or rushing close scares it off; calm
  visits earn its trust until it lets you take the egg. It never fights.
- Eggs hatch near warmth; the hatchling bonds to the player who hatched it.
- Colour from birthplace (biome palettes); End colours never overlap Overworld ones.
- Three stages: hatchling follows, juvenile can be ridden for short hops, adult flies at about
  elytra gliding speed. Sit and follow. Only the owner rides.
- Breeding: colours blend, with a small chance of a rare mutation (pearl, obsidian...).
- A raised dragon that dies is gone for good.

Later: clues near nests (shed scales, scorch marks, claw-marked trees), sighting times, saddlebags,
abilities, more colours.

## Sessions

| Date | Session | What happened |
|---|---|---|
| 2026-11-10 | Session 1, rebuilt on the current template | The repo laid down again from the current multiloader layout (`common` / `fabric` / `neoforge`), with the NeoForge checks on vanilla's headless test server instead of a game window. `Dragon` (a tamable animal: walks, looks at players, sits and follows its owner, and toggles sitting for its owner's empty hand; flight and growth later) with its colour as a synced, saved RGB number picked from its birthplace's palette (`Birthplace`: snowy, jungle, warm, forest, water, plains, End). `DragonModel` in plain model code (body, neck and head that follow the gaze, three-part tail, four legs, wings with membranes that fold on the ground and beat in the air, a lowered sitting pose) and `DragonRenderer`, which tints one grey texture by the colour and scales a young dragon by its age. A spawn egg in the spawn egg tab. Placeholder texture, egg and icon from `tools/draw-placeholders.py`. Both jars build; the unit tests pass (End colours never appear in an Overworld palette, every roll picks from its own palette, every colour can be picked). NeoForge game tests, all six passing: the dragon and its egg are registered; a dragon in the test room is alive after 40 ticks with 40 health, wild, wearing a colour from the room's own palette; each kind of biome (snowy slopes, jungle, badlands, savanna, taiga, ocean, plains, the End, End highlands) maps to its palette; with the room's biome turned into the End a dragon spawned there wears an End colour; a tamed dragon belongs to its tamer, sits for their empty hand and stands for the next, and ignores a stranger's hand; colour, owner and the order to sit all come back from a save. Proof run: with the colour pick, the sit toggle and the colour save switched off at once, exactly those four checks failed (the two rule checks still passed); code put back and the run green again. Fabric and NeoForge dedicated servers load the jars with 0 problem lines and summon a dragon. The Fabric client game test passes all 25 checks in a real game: two posed dragons (a tamed, sitting plains dragon and a wild End dragon) keep their colours, owner and pose on the server and in the player's game, are drawn by Dracora's renderer, and come back the same after the world is saved, closed and reopened; the screenshot shows both with folded wings in their own colours. |
