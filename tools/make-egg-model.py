"""Writes the egg block model: an egg shape stacked from one-pixel layers, so its sides curve
rather than stepping like a pyramid. It is widest just below the middle and narrows to a rounded
top, as an egg does.

    py tools/make-egg-model.py

Writes common/src/main/resources/assets/dracora/models/block/dragon_egg.json. Every face reads
the 16 x 16 egg texture where it sits (a side face shows the columns and rows it covers) and has
tint index 0, so the game multiplies it by the egg's colour.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "common/src/main/resources/assets/dracora/models/block/dragon_egg.json"

# (bottom, top, width): layers of the egg, centred, from the ground up.
# An egg's outline, sampled every half pixel up its 16-pixel height: widest (10) a little below
# the middle, narrowing faster toward the base and gently to a rounded top. Widths are kept to
# half pixels, so the sides curve in small steps rather than terraces.
import math

HEIGHT, STEP, WIDEST = 16.0, 0.5, 10.0
LAYERS = []
for i in range(int(HEIGHT / STEP)):
	y = (i + 0.5) * STEP
	t = y / HEIGHT
	# The egg curve: a circle stretched taller above its widest point (at 40% of the height).
	k = 0.25 + 0.75 * t / 0.4 if t < 0.4 else (1 - t) / 0.6  # the base starts wide enough to sit on
	w = WIDEST * math.sqrt(max(0.0, 1 - (1 - k) ** 2))
	w = max(3.0, round(w * 2) / 2)
	y0, y1 = i * STEP, (i + 1) * STEP
	if LAYERS and LAYERS[-1][2] == w:
		LAYERS[-1] = (LAYERS[-1][0], y1, w)  # merge equal neighbours into one box
	else:
		LAYERS.append((y0, y1, w))


def element(y0, y1, width):
	a = (16 - width) / 2
	b = a + width
	side = {"uv": [a, 16 - y1, b, 16 - y0], "texture": "#all", "tintindex": 0}
	cap = {"uv": [a, a, b, b], "texture": "#all", "tintindex": 0}
	return {
		"from": [a, y0, a],
		"to": [b, y1, b],
		"faces": {"north": side, "east": side, "south": side, "west": side, "up": cap, "down": cap},
	}


model = {
	"parent": "minecraft:block/block",
	"textures": {"particle": "dracora:block/dragon_egg", "all": "dracora:block/dragon_egg"},
	"elements": [element(*layer) for layer in LAYERS],
}
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(model, indent="\t") + "\n", encoding="utf-8", newline="\n")
print("wrote", OUT.relative_to(ROOT))
