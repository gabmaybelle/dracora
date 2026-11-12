"""Draws Dracora's placeholder textures until Gabby's art replaces them.

    py tools/draw-placeholders.py

Writes:
- common/src/main/resources/assets/dracora/textures/item/dragon_spawn_egg.png: a 16 x 16 egg.
- art/icon.png: the same egg at 64 x 64 (a stand-in: the real icon is Gabby's).
- common/src/main/resources/assets/dracora/textures/block/dragon_egg*.png: the egg block's grey
  shell, whole, slightly cracked and very cracked. The game tints it with the egg's colour.

The dragon itself needs no picture from here: it wears the game's own Ender Dragon texture,
greyed and tinted in the game.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/dracora"

EGG = [
	"......####......",
	"....##aaaa##....",
	"...#aaaaaaaa#...",
	"..#aaabbaaaaa#..",
	"..#aabbbaaacc#..",
	".#aaaabbaaaccc#.",
	".#aaaaaaaaaaaa#.",
	".#acaaaaabbaaa#.",
	".#cccaaaabbbaa#.",
	".#acaaaaaabbaa#.",
	".#aaaabbaaaaaa#.",
	".#aaabbbaaacca#.",
	"..#aaabaaaccc#..",
	"..#aaaaaaaaaa#..",
	"...##aaaaaa##...",
	".....######.....",
]
EGG_COLOURS = {
	"#": (58, 40, 70, 255),      # outline, deep violet
	"a": (236, 226, 204, 255),   # shell, warm cream
	"b": (122, 92, 168, 255),    # End violet speckles
	"c": (94, 140, 70, 255),     # Overworld green speckles
}


def egg(scale):
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	px = img.load()
	for y, row in enumerate(EGG):
		for x, ch in enumerate(row):
			if ch in EGG_COLOURS:
				px[x, y] = EGG_COLOURS[ch]
	if scale != 1:
		img = img.resize((16 * scale, 16 * scale), Image.NEAREST)
	return img


# The egg block's cracks, as (x, y) steps across the 16 x 16 shell: the first set shows on a
# slightly cracked egg, both on a very cracked one.
CRACKS_1 = [(3, 6), (4, 7), (5, 7), (6, 8), (7, 7), (8, 8), (9, 9), (10, 9), (11, 8), (12, 9)]
CRACKS_2 = [(6, 9), (6, 10), (5, 11), (5, 12), (9, 10), (10, 11), (10, 12), (11, 13), (7, 6), (7, 5), (8, 4), (4, 4), (5, 5)]
CRACK = 70


def egg_shell(cracks):
	"""The egg block's texture: a grey speckled shell the game tints, darker toward the ground, with cracks."""
	rng = random.Random(11)
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	px = img.load()
	for y in range(16):
		for x in range(16):
			value = 238 - y * 2 + rng.randint(-4, 4)
			if rng.random() < 0.12:
				value -= 34                      # speckles
			px[x, y] = (value, value, value, 255)
	for x, y in cracks:
		px[x, y] = (CRACK, CRACK, CRACK, 255)
	return img


def save(img, path):
	path.parent.mkdir(parents=True, exist_ok=True)
	img.save(path)
	print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
	save(egg(1), ASSETS / "textures/item/dragon_spawn_egg.png")
	save(egg(4), ROOT / "art/icon.png")
	save(egg_shell([]), ASSETS / "textures/block/dragon_egg.png")
	save(egg_shell(CRACKS_1), ASSETS / "textures/block/dragon_egg_slightly_cracked.png")
	save(egg_shell(CRACKS_1 + CRACKS_2), ASSETS / "textures/block/dragon_egg_very_cracked.png")
