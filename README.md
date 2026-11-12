# Dracora

Rare dragons, raised from an egg, coloured by where they were born.

Dragons in Minecraft are usually one boss at the end of the game. In Dracora they live in the
Overworld and the End, and they're rare: finding a nest is an event. Earn a wild parent's trust,
take its egg home, hatch it by a warm fire, and raise the hatchling into a dragon you can fly. Its
colours tell everyone where it came from: pale blues from snowy peaks, greens and golds from
jungles, violets and blacks from the End.

**Work in progress.** Nothing is released yet. So far there is a dragon you can summon, with a
colour from the biome it appears in, that sits and follows once tamed, and an egg that hatches
beside a campfire or lava into a hatchling of its colour, bonded to whoever set it down; nests,
growing up, flying and breeding are on the way.

## Planned for the first release

- Rare nests on high peaks and in old-growth forests, and on remote outer End islands.
- Shy wild dragons: approach calmly and visit again to earn the parent's trust and its egg.
- Hatch the egg near warmth; the dragon bonds with you, and only you can ride it.
- Three stages: a hatchling that follows you, a juvenile you can ride for short hops, and an adult
  that flies about as fast as an elytra glides.
- Colours from birthplace, mixed when two dragons have an egg, with a small chance of a rare colour.
- Look after your dragon: one that dies is gone for good.

Minecraft 26.3, Fabric (with Fabric API) and NeoForge. Install on the server and on every
player's game. Made by Gabby.

## Building

```
./gradlew build
```

The jars land in `fabric/build/libs/` and `neoforge/build/libs/`. The in-game tests:

```
./gradlew :fabric:runClientGameTest
./gradlew :neoforge:runGameTestServer
```

The Fabric one opens a real game; the NeoForge one runs on a test server with no window.

## Licence

MIT.
