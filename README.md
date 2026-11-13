# Dracora

Rare dragons, raised from an egg, coloured by where they were born.

Dragons in Minecraft are usually one boss at the end of the game. In Dracora they live in the
Overworld and the End, and they're rare: finding a nest is an event. Earn a wild parent's trust,
take its egg home, hatch it by a warm fire, and raise the hatchling into a dragon you can fly. Its
colours tell everyone where it came from: pale blues from snowy peaks, greens and golds from
jungles, violets and blacks from the End.

## What's in it

- Rare nests on high peaks and in old-growth forests, and on the outer End islands: a stone bowl
  with one egg, a shy parent beside it, and a guide book on a lectern.
- Earn the parent's trust with calm visits, one a day; after the third, the egg is yours to take.
- Hatch the egg beside a campfire or lava. The dragon bonds with whoever set the egg down, and only
  they can ride it. A dragon is only ever yours when you hatch it; no wild one can be tamed.
- Three stages: a hatchling that follows you, a juvenile you can ride for short hops, and an adult
  that flies with you at about an elytra's glide.
- Colours from birthplace: pale blues from snowy peaks, greens and golds from jungles, rusts from
  the badlands, sea blues, and the End's own violets and blacks.
- Breeding: feed two of your grown dragons raw fish and they lay an egg in a blend of their
  colours, sometimes a rare pearl or obsidian.
- Every dragon glows faintly, so yours shows at night.
- Look after your dragon: each one is one of a kind, and one that dies is gone for good.

Settings in `config/dracora.properties`: how far apart nests are, the top flying speed, and
whether juveniles can be ridden.

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
