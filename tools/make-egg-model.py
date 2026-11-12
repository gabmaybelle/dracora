"""Writes the placeholder egg block model: an egg shape stacked from six boxes.

    py tools/make-egg-model.py

Writes common/src/main/resources/assets/dracora/models/block/dragon_egg.json. Every face reads
the 16 x 16 egg texture where it sits (a side face shows the columns and rows it covers) and has
tint index 0, so the game multiplies it by the egg's colour. The real model is Gabby's.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "common/src/main/resources/assets/dracora/models/block/dragon_egg.json"

# (bottom, top, width): layers of the egg, centred, from the ground up.
LAYERS = [(0, 1, 8), (1, 3, 10), (3, 8, 12), (8, 11, 10), (11, 13, 8), (13, 14, 4)]


def element(y0, y1, width):
	a = (16 - width) // 2
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
