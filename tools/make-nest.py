"""Writes the nest's structure template: a shallow bowl nine blocks across, with a floor of
smooth basalt, a low rim of cobblestone and mossy cobblestone, a bed of hay in the middle with a
guarded egg on it, and the parent dragon standing beside the egg. The egg carries no colour: its
parent gives it one when it first looks around. The file is plain NBT (gzip), written here
without a library.

    py tools/make-nest.py
"""
import gzip
import math
import struct
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "common/src/main/resources/data/dracora/structure/nest.nbt"
SIZE = (9, 2, 9)
CENTRE = 4
DATA_VERSION = 5023  # the version vanilla's 26.3 templates carry
PALETTE = ["minecraft:smooth_basalt", "minecraft:cobblestone", "minecraft:mossy_cobblestone", "minecraft:hay_block", "dracora:dragon_egg"]
BASALT, COBBLE, MOSSY, HAY, EGG = range(5)


def name(text):
	raw = text.encode()
	return struct.pack(">H", len(raw)) + raw


def tag_byte(key, value):
	return b"\x01" + name(key) + struct.pack(">b", value)


def tag_int(key, value):
	return b"\x03" + name(key) + struct.pack(">i", value)


def tag_string(key, value):
	raw = value.encode()
	return b"\x08" + name(key) + struct.pack(">H", len(raw)) + raw


def tag_list(key, kind, payloads):
	return b"\x09" + name(key) + bytes([kind]) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def tag_compound(key, *tags):
	return b"\x0a" + name(key) + b"".join(tags) + b"\x00"


def compound(*tags):
	return b"".join(tags) + b"\x00"


def ints(values):
	return [struct.pack(">i", v) for v in values]


def doubles(values):
	return [struct.pack(">d", v) for v in values]


def block(x, y, z, state, *extra):
	return compound(tag_list("pos", 3, ints((x, y, z))), tag_int("state", state), *extra)


blocks = []
for x in range(SIZE[0]):
	for z in range(SIZE[2]):
		reach = math.hypot(x - CENTRE, z - CENTRE)
		if reach > 4.6:
			continue
		rim = reach > 3.6
		mossy = (x * 7 + z * 13) % 3 == 0
		if rim:
			blocks.append(block(x, 0, z, MOSSY if mossy else COBBLE))
			blocks.append(block(x, 1, z, COBBLE if mossy else MOSSY))
		else:
			blocks.append(block(x, 0, z, HAY if abs(x - CENTRE) + abs(z - CENTRE) <= 1 else BASALT))
blocks.append(block(CENTRE, 1, CENTRE, EGG, tag_compound("nbt", tag_string("id", "dracora:dragon_egg"), tag_byte("Guarded", 1))))

parent = compound(
	tag_list("pos", 6, doubles((CENTRE + 0.5 + 2.2, 1.0, CENTRE + 0.5))),
	tag_list("blockPos", 3, ints((CENTRE + 2, 1, CENTRE))),
	tag_compound("nbt", tag_string("id", "dracora:dragon")),
)

root = (
	b"\x0a" + name("")
	+ tag_list("size", 3, ints(SIZE))
	+ tag_list("entities", 10, [parent])
	+ tag_list("blocks", 10, blocks)
	+ tag_list("palette", 10, [compound(tag_string("id", block_name)) for block_name in PALETTE])
	+ tag_int("DataVersion", DATA_VERSION)
	+ b"\x00"
)

OUT.parent.mkdir(parents=True, exist_ok=True)
with gzip.open(OUT, "wb") as f:
	f.write(root)
print("wrote", OUT, "with", len(blocks), "blocks")
