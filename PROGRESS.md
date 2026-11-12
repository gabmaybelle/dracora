# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends, saying what changed, what the game tests counted, what the proof run was (the feature off,
which checks fail, code put back), what was learned, and what was deliberately not done.

## Where things stand

- **What it does so far:** a dragon you can summon (`/summon dracora:dragon` or its spawn egg). It
  walks, looks around, and sits or follows when tamed; its owner's empty hand sits it down and
  stands it up. Its colour comes from the biome it appears in (a first draft of each palette; End
  colours are separate and never appear in the Overworld). An egg block that carries a colour:
  set down beside a lit campfire or lava it gathers warmth, cracks twice and hatches after a
  Minecraft day of warmth into a baby dragon of its colour, owned by whoever set the egg down
  (there or away). A plain egg takes its colour from the biome it is set down in; a broken egg
  drops with its colour. Fabric and NeoForge on 26.3. The dragon's texture, the egg's model and
  textures, the spawn egg and the icon are placeholders until Gabby's art.
- **Broken:** nothing known. The dragon's model is a first pass: folded wings stand up beside the
  body as tall sheets, which reads as resting but blocky. The egg's checks in a real game (its
  tint reaching the player) are written but have not been run yet.
- **Next:** session 3: growth stages (the hatchling is a vanilla baby for now), sit and follow,
  juvenile hops, adult flight, and a dedicated-server flight test. Flight borrows ideas from
  vanilla's Ender Dragon (written our own way, not copied): a three-part wing beat with the tips
  lagging, a neck and tail that trail through turns, the jaw. Then a model and texture session,
  once flight has settled the dragon's shape, before the nests.

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
| 2026-11-11 | Session 2: the egg, hatching and bonding | `EggBlock` with a block entity (`EggBlockEntity`) that remembers the egg's colour, who set it down and the warmth it has gathered; `Incubation` holds the rules. The egg never ticks: it looks for warmth on one scheduled tick every 100 game ticks. A block in the `dracora:warms_eggs` tag (campfires and lava) within two blocks warms it, and one with a "lit" state only while lit. It cracks at a third and two thirds of 24000 ticks of warmth and hatches at the end; a cold egg keeps what it has. The hatchling is a baby dragon in the egg's colour, tamed for the placer if they are in the world and theirs by id if they are away; an egg nobody set down (a nest's egg, later) never hatches. As an item the egg carries its colour in vanilla's dyed-colour component, which tints the item with no code (the "Dyed" tooltip line is hidden); the block is tinted from its block entity through each loader's block colour registration, and the colour is all that is sent to players. A plain egg (the creative menu's) takes a colour from the biome it is set down in. Loot table: the egg drops itself with its colour, also from an explosion. Placeholder egg model (`tools/make-egg-model.py`) and shell textures. Decided with Gabby: the hatchling bonds with whoever set the egg down; the dragon's model and texture get their own session after flight. Both jars build; unit tests pass (the crack stages). NeoForge game tests, all twelve passing, six of them new: the egg is registered; lit campfires, soul campfires and lava warm an egg and an unlit campfire, a torch and magma don't; a plain egg set down by a player takes a plains colour in the plains room and remembers them, a coloured egg keeps its colour and drops with it and without its placer or warmth; beside a campfire an egg is whole after 79 checks, cracked after 80 and 160, still an egg after 239 and hatched after 240 into one baby dragon in the egg's colour owned by the placer, and the same for a placer who is away; an egg gathers nothing when cold, beside an unlit campfire, with a campfire three blocks off, or with no placer, and 2000 warmth from 20 checks beside lava; colour, placer and warmth come back from a save and players are sent only the colour. Proof runs: with the colour pick, the save and the warmth rule switched off, exactly the five checks that depend on them failed; with bonding switched off, the hatching check failed on ownership; the first run of the drop check failed on its own, because the loot table was in the pre-26 format and dropped an uncoloured egg (fixed). Code put back and the run green again. Fabric and NeoForge dedicated servers load the jars with 0 problem lines, place an egg with a colour and drop it from its loot table. The Fabric client game test gained egg checks (the egg and its colour reach the player's game, Dracora's tint draws it in that colour, and colour and placer survive reopening the world); they compile but were not run this session, since they need a game window. Not done: no recipe or other survival source for eggs (they come from nests, session 4), and no growth stages yet. |
| 2026-11-10 | Session 1, rebuilt on the current template | The repo laid down again from the current multiloader layout (`common` / `fabric` / `neoforge`), with the NeoForge checks on vanilla's headless test server instead of a game window. `Dragon` (a tamable animal: walks, looks at players, sits and follows its owner, and toggles sitting for its owner's empty hand; flight and growth later) with its colour as a synced, saved RGB number picked from its birthplace's palette (`Birthplace`: snowy, jungle, warm, forest, water, plains, End). `DragonModel` in plain model code (body, neck and head that follow the gaze, three-part tail, four legs, wings with membranes that fold on the ground and beat in the air, a lowered sitting pose) and `DragonRenderer`, which tints one grey texture by the colour and scales a young dragon by its age. A spawn egg in the spawn egg tab. Placeholder texture, egg and icon from `tools/draw-placeholders.py`. Both jars build; the unit tests pass (End colours never appear in an Overworld palette, every roll picks from its own palette, every colour can be picked). NeoForge game tests, all six passing: the dragon and its egg are registered; a dragon in the test room is alive after 40 ticks with 40 health, wild, wearing a colour from the room's own palette; each kind of biome (snowy slopes, jungle, badlands, savanna, taiga, ocean, plains, the End, End highlands) maps to its palette; with the room's biome turned into the End a dragon spawned there wears an End colour; a tamed dragon belongs to its tamer, sits for their empty hand and stands for the next, and ignores a stranger's hand; colour, owner and the order to sit all come back from a save. Proof run: with the colour pick, the sit toggle and the colour save switched off at once, exactly those four checks failed (the two rule checks still passed); code put back and the run green again. Fabric and NeoForge dedicated servers load the jars with 0 problem lines and summon a dragon. The Fabric client game test passes all 25 checks in a real game: two posed dragons (a tamed, sitting plains dragon and a wild End dragon) keep their colours, owner and pose on the server and in the player's game, are drawn by Dracora's renderer, and come back the same after the world is saved, closed and reopened; the screenshot shows both with folded wings in their own colours. |
