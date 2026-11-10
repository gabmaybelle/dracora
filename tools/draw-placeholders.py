"""Draws Dracora's placeholder textures until Gabby's art replaces them.

    py tools/draw-placeholders.py

Writes:
- common/src/main/resources/assets/dracora/textures/entity/dragon/dragon.png: 128 x 128, grey,
  laid out to match DragonModel's boxes. The game multiplies it by each dragon's colour, so
  near-white reads as the full colour and the darker scale lines as its shading.
- common/src/main/resources/assets/dracora/textures/item/dragon_spawn_egg.png: a 16 x 16 egg.
- art/icon.png: the same egg at 64 x 64 (a stand-in: the real icon is Gabby's).

BOXES must match the texOffs and sizes in DragonModel.createBodyLayer.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/dracora"

# name: (u, v, width, height, depth), as in DragonModel. Membranes have height 0.
BOXES = {
	"body": (0, 0, 12, 10, 20),
	"neck": (64, 0, 6, 6, 8),
	"head": (0, 30, 8, 7, 7),
	"snout": (30, 30, 5, 4, 5),
	"horn": (50, 30, 2, 5, 2),
	"tail1": (64, 14, 6, 6, 10),
	"tail2": (96, 0, 4, 4, 10),
	"tail3": (96, 14, 2, 2, 10),
	"leg": (0, 44, 4, 10, 4),
	"wing_arm": (0, 60, 16, 2, 2),
	"wing_tip_arm": (40, 60, 14, 2, 2),
	"membrane": (0, 64, 16, 0, 14),
	"tip_membrane": (64, 64, 14, 0, 12),
}

BASE = 228      # most scales
SHADE = 186     # the line under each scale
RIDGE = 205     # tops of the back, neck and tail
BELLY = 248     # undersides
HORN = 250      # horns and claws stay pale
EYE = 30


def faces(u, v, w, h, d):
	"""Vanilla's box unwrap: (face, x, y, width, height)."""
	return [
		("top", u + d, v, w, d),
		("bottom", u + d + w, v, w, d),
		("east", u, v + d, d, h),
		("front", u + d, v + d, w, h),
		("west", u + d + w, v + d, d, h),
		("back", u + 2 * d + w, v + d, w, h),
	]


def scales(img, x0, y0, w, h, base, rng):
	"""Rows of small overlapping scales: each 4 wide, shaded along its lower edge."""
	px = img.load()
	for y in range(y0, y0 + h):
		for x in range(x0, x0 + w):
			row = (y - y0) // 3
			col = (x - x0 + (2 if row % 2 else 0)) % 4
			value = base
			if (y - y0) % 3 == 2 and col in (1, 2):
				value = SHADE
			elif (y - y0) % 3 == 2:
				value = base - 18
			value += rng.randint(-4, 4)
			px[x, y] = (value, value, value, 255)


def fill(img, x0, y0, w, h, value, rng, jitter=3):
	px = img.load()
	for y in range(y0, y0 + h):
		for x in range(x0, x0 + w):
			g = value + rng.randint(-jitter, jitter)
			px[x, y] = (g, g, g, 255)


def membrane(img, x0, y0, w, h, rng):
	"""Thin skin, lighter than scales, with three veins fanning back from the arm."""
	fill(img, x0, y0, w, h, 236, rng, 2)
	px = img.load()
	for i in range(1, 4):
		vx = x0 + i * w // 4
		for y in range(y0, y0 + h):
			x = min(x0 + w - 1, vx + (y - y0) // 4)
			px[x, y] = (200, 200, 200, 255)
	for x in range(x0, x0 + w):            # trailing edge
		px[x, y0 + h - 1] = (196, 196, 196, 255)


def dragon_texture():
	rng = random.Random(7)
	img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
	for name, (u, v, w, h, d) in BOXES.items():
		if h == 0:
			membrane(img, u + d, v, w, d, rng)
			membrane(img, u + d + w, v, w, d, rng)
			continue
		for face, x, y, fw, fh in faces(u, v, w, h, d):
			if fw <= 0 or fh <= 0:
				continue
			if name == "horn":
				fill(img, x, y, fw, fh, HORN, rng)
			elif face == "bottom":
				fill(img, x, y, fw, fh, BELLY, rng)
			elif face == "top":
				scales(img, x, y, fw, fh, RIDGE, rng)
			else:
				scales(img, x, y, fw, fh, BASE, rng)
	# Eyes: the top corners of the head's front face, above the snout.
	u, v, w, h, d = BOXES["head"]
	px = img.load()
	for ex in (u + d + 1, u + d + w - 2):
		px[ex, v + d + 1] = (EYE, EYE, EYE, 255)
		px[ex, v + d + 2] = (EYE, EYE, EYE, 255)
	# Claws: the bottom row of each leg's sides.
	u, v, w, h, d = BOXES["leg"]
	for x in range(u, u + 2 * d + 2 * w):
		px[x, v + d + h - 1] = (HORN, HORN, HORN, 255)
	return img


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


def save(img, path):
	path.parent.mkdir(parents=True, exist_ok=True)
	img.save(path)
	print("wrote", path.relative_to(ROOT))


if __name__ == "__main__":
	save(dragon_texture(), ASSETS / "textures/entity/dragon/dragon.png")
	save(egg(1), ASSETS / "textures/item/dragon_spawn_egg.png")
	save(egg(4), ROOT / "art/icon.png")
