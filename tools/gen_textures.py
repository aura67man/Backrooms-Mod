#!/usr/bin/env python3
"""Generates every texture in the mod (no hand-drawn or third-party art), built on texlab.py.

Each texture uses a small hue-shifted ramp, clustered detail instead of per-pixel noise, and is
run through texlab's validator. Preview sheets go to build/texlab/ so they can be looked at.

Usage: python3 tools/gen_textures.py
"""
import math
import os
import random
import sys
import warnings

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
warnings.simplefilter("ignore", DeprecationWarning)
from PIL import Image, ImageDraw  # noqa: E402
from texlab import Anim, Tex, preview, ramp, render_tiled, render_zoom, validate  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms", "textures")
PREVIEWS = os.path.join(ROOT, "build", "texlab")

# ---------------------------------------------------------------- shared palettes (dark -> light)
CARPET = ramp("#8f7d4c", 5, spread=0.07, hue_shift=5)
PAPER = ramp("#c9b25c", 5, spread=0.07, hue_shift=6)
WOOD = ramp("#6b4f2f", 5, spread=0.09, hue_shift=6)
CEILING = ramp("#d2cebd", 5, spread=0.06, hue_shift=6)
CONCRETE = ramp("#7c7c78", 5, spread=0.06, hue_shift=4)
ASPHALT = ramp("#434347", 5, spread=0.055, hue_shift=6)
PAINT = ramp("#c9a52e", 3, spread=0.12, hue_shift=8)
BLUE = ramp("#3f93bd", 5, spread=0.09, hue_shift=7)
TILE_GROUT, TILE_SHADE, TILE, TILE_HI, TILE_AQUA = "#b4c9d1", "#d9e6ea", "#ecf3f5", "#fcfefe", "#c2e5ea"
METAL = ramp("#2e2b29", 4, spread=0.07, hue_shift=6)
SHADE = ramp("#cfc08f", 5, spread=0.075, hue_shift=7)


def cluster(t, colour, x, y, cells, wrap=True):
    """Paint a hand-shaped cluster: `cells` are offsets from (x, y)."""
    for dx, dy in cells:
        px, py = x + dx, y + dy
        if wrap:
            px, py = px % t.w, py % t.h
        t.set(px, py, colour)


BLOB = ((0, 0), (1, 0), (0, 1), (1, 1))
SMEAR = ((0, 0), (1, 0), (2, 0), (1, 1), (2, 1))
PATCH = ((0, 0), (1, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1), (1, 2), (2, 2))
CHIP = ((0, 0), (1, 0), (1, 1))


# ---------------------------------------------------------------- Level 0
def damp_carpet():
    """Loop-pile carpet: short woven dashes, with darker patches where the damp has got in."""
    t = Tex(16)
    t.fill(CARPET[2])
    rnd = random.Random(1)
    for y in range(16):
        x = rnd.randrange(4)
        while x < 16:
            length = rnd.choice((2, 2, 3))
            if rnd.random() < 0.6:
                colour = CARPET[3] if (y + x // 3) % 2 == 0 else CARPET[1]
                for i in range(length):
                    t.set((x + i) % 16, y, colour)
            x += length + rnd.choice((1, 2, 3))
    cluster(t, CARPET[1], 3, 4, PATCH)
    cluster(t, CARPET[0], 4, 5, BLOB)
    cluster(t, CARPET[1], 10, 11, PATCH)
    cluster(t, CARPET[0], 11, 12, SMEAR)
    cluster(t, CARPET[0], 14, 1, CHIP)
    return t


def wallpaper():
    """Yellowed paper: embossed pinstripes and a half-drop diamond motif, plus a couple of stains."""
    t = Tex(16)
    t.fill(PAPER[2])
    for y in range(16):
        for x in (3, 11):  # stripe pairs sit away from the tile edge so the wrap is invisible
            t.set(x, y, PAPER[1])
            t.set(x + 1, y, PAPER[3])
    for cx, cy in ((7, 3), (15, 11)):  # half-drop repeat
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            t.set((cx + dx) % 16, (cy + dy) % 16, PAPER[3])
        t.set(cx % 16, cy % 16, PAPER[4])
        t.set((cx + 1) % 16, (cy + 1) % 16, PAPER[1])
        t.set(cx % 16, (cy + 2) % 16, PAPER[1])
    cluster(t, PAPER[1], 7, 9, SMEAR)
    cluster(t, PAPER[1], 13, 2, CHIP)
    cluster(t, PAPER[0], 8, 10, ((0, 0), (1, 0)))
    return t


def wallpaper_baseboard():
    """The same paper with a varnished skirting board along the bottom quarter."""
    t = wallpaper()
    t.rect(0, 11, 15, 11, PAPER[0])   # shadow line where the board meets the wall
    t.rect(0, 12, 15, 12, WOOD[4])    # top edge catches the light
    t.rect(0, 13, 15, 14, WOOD[2])
    t.rect(0, 15, 15, 15, WOOD[0])
    for x0, y, colour in ((1, 13, WOOD[3]), (6, 14, WOOD[1]), (9, 13, WOOD[1]), (12, 14, WOOD[3])):
        t.rect(x0, y, x0 + 2, y, colour)  # grain
    return t


def ceiling_tile():
    """One mineral-fibre drop-ceiling tile per block: bevelled edge, fissures in small clusters."""
    t = Tex(16)
    t.fill(CEILING[2])
    for x, y, cells in ((3, 3, SMEAR), (9, 2, CHIP), (12, 6, BLOB), (5, 8, CHIP), (2, 11, BLOB), (8, 12, SMEAR), (12, 11, CHIP)):
        cluster(t, CEILING[1], x, y, cells, wrap=False)
    for x, y in ((6, 5), (10, 9), (4, 13)):
        t.rect(x, y, x + 1, y, CEILING[3])
    t.bevel(CEILING[4], CEILING[0])
    return t


def light_panel(lit):
    """Troffer set into the ceiling grid: metal trim, ribbed diffuser, two tubes behind it."""
    t = ceiling_tile()
    if lit:
        trim, diffuser, rib, glow, core = "#99947c", "#f5e9b0", "#e3d391", "#fff9df", "#ffffff"
    else:
        trim, diffuser, rib, glow, core = "#6f6d62", "#8d8d84", "#7a7a72", "#a5a59b", "#b4b4aa"
    t.rect(2, 2, 13, 13, trim, outline=True)
    t.rect(3, 3, 12, 12, diffuser)
    for y in (5, 8, 11):
        t.rect(3, y, 12, y, rib)
    for x in (5, 9):  # each tube: a bright core with a soft edge either side
        t.rect(x - 1, 3, x - 1, 12, glow)
        t.rect(x, 3, x + 1, 12, core)
        t.rect(x + 2, 3, x + 2, 12, glow)
    return t


# ---------------------------------------------------------------- Poolrooms
def pool_tile(grout, shade, base, highlight, alt, alt_highlight, alt_cells):
    """Sixteen small glazed tiles. Grout lines sit mid-texture so the tile edge wraps cleanly."""
    t = Tex(16)
    t.fill(grout)
    for ty in range(4):
        for tx in range(4):
            body = alt if (tx, ty) in alt_cells else base
            for dy in range(3):
                for dx in range(3):
                    colour = body
                    if (dx, dy) in ((0, 0), (1, 0), (0, 1)):
                        colour = highlight if body == base else alt_highlight
                    elif (dx, dy) == (2, 2):
                        colour = shade
                    t.set((tx * 4 + 2 + dx) % 16, (ty * 4 + 2 + dy) % 16, colour)
    return t


def white_tile():
    return pool_tile(TILE_GROUT, TILE_SHADE, TILE, TILE_HI, TILE_AQUA, TILE, {(1, 0), (3, 2), (0, 3)})


def blue_tile():
    # a quieter glaze than the white tile: one step of highlight, a few tiles a shade deeper
    return pool_tile(BLUE[0], BLUE[1], BLUE[2], BLUE[3], BLUE[1], BLUE[2], {(2, 1), (0, 2), (3, 3)})


def pool_light():
    """A flush glass panel in a chrome ring, stepped from white-hot centre to aqua edge."""
    t = Tex(16)
    t.fill(TILE)
    t.bevel(TILE_HI, TILE_SHADE)
    t.rect(1, 1, 14, 14, "#9fb6bd", outline=True)
    bands = ((7.2, "#a5e8e0"), (5.4, "#c9f7f0"), (3.6, "#e6fffa"), (1.8, TILE_HI))
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            t.set(x, y, next((c for r, c in reversed(bands) if d <= r), bands[0][1]))
    return t


# ---------------------------------------------------------------- Car park
def garage_concrete():
    """Board-marked concrete: a formwork seam with a lit lip, tie holes, and soft tonal patches."""
    t = Tex(16)
    t.fill(CONCRETE[2])
    for x, y, cells, tone in ((1, 1, PATCH, 1), (9, 3, SMEAR, 3), (12, 10, PATCH, 1), (4, 11, SMEAR, 3), (6, 4, BLOB, 3), (14, 13, BLOB, 1)):
        cluster(t, CONCRETE[tone], x, y, cells)
    t.rect(0, 7, 15, 7, CONCRETE[0])
    t.rect(0, 8, 15, 8, CONCRETE[3])
    for x, y in ((3, 3), (11, 3), (3, 12), (11, 12)):
        t.set(x, y, CONCRETE[0])
        t.set(x + 1, y + 1, CONCRETE[4])
    return t


def garage_floor():
    """Sealed asphalt: tonal patches, bits of aggregate, and an oil stain."""
    t = Tex(16)
    t.fill(ASPHALT[2])
    for x, y, cells, tone in ((2, 2, PATCH, 1), (10, 1, SMEAR, 3), (6, 7, SMEAR, 3), (12, 12, PATCH, 1), (0, 10, BLOB, 3), (8, 13, CHIP, 3), (13, 6, CHIP, 1)):
        cluster(t, ASPHALT[tone], x, y, cells)
    cluster(t, ASPHALT[0], 4, 3, BLOB)
    cluster(t, ASPHALT[0], 12, 13, SMEAR)
    for x, y in ((8, 4), (1, 13), (14, 9)):
        t.rect(x, y, x + 1, y, ASPHALT[4])
    return t


def garage_line():
    """A painted bay line down the middle of the floor texture, worn through in places."""
    t = garage_floor()
    t.rect(6, 0, 6, 15, PAINT[0])
    t.rect(7, 0, 8, 15, PAINT[1])
    t.rect(9, 0, 9, 15, PAINT[0])
    for y0 in (1, 9):
        t.rect(7, y0, 7, y0 + 3, PAINT[2])  # thicker paint catching the light
    for x, y, cells in ((8, 5, CHIP), (6, 12, ((0, 0), (0, 1), (1, 1))), (8, 14, ((0, 0), (1, 0)))):
        cluster(t, ASPHALT[2], x, y, cells)
    t.set(9, 6, ASPHALT[1])
    return t


def hazard_pillar():
    """Diagonal warning stripes in flat paint, scuffed in short strokes and chipped back to concrete."""
    black = ("#1a1a1e", "#27272c", "#37373e")
    t = Tex(16)
    for y in range(16):
        for x in range(16):
            t.set(x, y, PAINT[1] if ((x + y) // 4) % 2 == 0 else black[1])
    # scuffs: two-pixel strokes that stay inside their own stripe
    for x, y, tone in ((1, 1, 2), (9, 1, 2), (4, 6, 0), (12, 6, 2), (0, 10, 0), (8, 10, 2), (5, 13, 2)):
        t.rect(x, y, x + 1, y, PAINT[tone])
    for x, y, tone in ((5, 1, 2), (13, 1, 0), (0, 6, 2), (8, 6, 0), (4, 10, 2), (12, 10, 2), (1, 13, 0), (9, 13, 2)):
        t.rect(x, y, x + 1, y, black[tone])
    cluster(t, CONCRETE[2], 3, 11, PATCH, wrap=False)
    cluster(t, CONCRETE[1], 4, 12, BLOB, wrap=False)
    cluster(t, CONCRETE[2], 11, 2, SMEAR, wrap=False)
    cluster(t, CONCRETE[1], 12, 3, ((0, 0), (1, 0)), wrap=False)
    return t


def sodium_lamp():
    """Bulkhead lamp in the concrete soffit: dark housing, wire guard, a long orange tube."""
    t = garage_concrete()
    t.rect(2, 2, 13, 13, "#2c2c2e", outline=True)
    t.rect(3, 3, 12, 12, "#b3601c")
    t.rect(3, 4, 12, 11, "#e88a2b")
    t.rect(3, 5, 12, 10, "#ffb347")
    t.rect(4, 6, 11, 9, "#ffd98a")
    for x in (5, 10):
        t.rect(x, 3, x, 12, "#6b3a12")  # guard bars
    return t


# ---------------------------------------------------------------- Shared
def threshold_frames(count=8):
    """The dark inside a door frame: bands of almost-black violet that sway while a faint ripple
    crosses them. Every term completes a whole number of cycles, so the loop is seamless."""
    tones = ("#050508", "#0a0914", "#120f22", "#1b1533", "#2a2050")
    frames = []
    for i in range(count):
        t = Tex(16)
        phase = i / count * 2 * math.pi
        for y in range(16):
            for x in range(16):
                v = math.sin(y * 2 * math.pi / 16 + 1.1 * math.sin(x * 2 * math.pi / 16) + 0.5 * math.sin(phase))
                v += 0.3 * math.sin((x + y) * 2 * math.pi / 8 - phase)
                level = 4 if v > 1.12 else 3 if v > 0.75 else 2 if v > 0.3 else 1 if v > -0.35 else 0
                t.set(x, y, tones[level])
        frames.append(t)
    return frames


FONT = {
    "E": ["###", "#..", "##.", "#..", "###"],
    "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "I": ["#", "#", "#", "#", "#"],
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
}


def exit_sign():
    """Backlit sign: dark housing, green panel, lettering with a one-pixel glow."""
    housing, bezel_hi, bezel_lo, panel, glow, letters = "#1a211c", "#33403a", "#0c100d", "#0f3b20", "#2f8a4a", "#8dffa8"
    t = Tex(16)
    t.fill(housing)
    t.rect(0, 3, 15, 3, bezel_hi)
    t.rect(0, 4, 15, 11, panel)
    t.rect(0, 12, 15, 12, bezel_lo)
    lit = set()
    for ch, x0 in (("E", 2), ("X", 6), ("I", 10), ("T", 12)):
        for ry, row in enumerate(FONT[ch]):
            for rx, bit in enumerate(row):
                if bit == "#":
                    lit.add((x0 + rx, 5 + ry))
    for x, y in lit:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in lit and 4 <= y + dy <= 11:
                t.set(x + dx, y + dy, glow)
    for x, y in lit:
        t.set(x, y, letters)
    return t


# ---------------------------------------------------------------- Items
def key_fob():
    return Tex.from_ascii([
        "................",
        "..kkkk..........",
        ".kRrrrk.........",
        ".kr..rk.........",
        ".kr..rk.........",
        ".kRrrrkkkkkkk...",
        "..kkkkBHHHHBBk..",
        ".....kBllllBBk..",
        ".....kBlLLlBBk..",
        ".....kBBBBBBBk..",
        ".....kBBPPBBDk..",
        ".....kBBppBBDk..",
        ".....kBBBBBDDk..",
        ".....kBDDDDDDk..",
        "......kkkkkkk...",
        "................",
    ], {"k": "#0d0d10", "R": "#dfe3ec", "r": "#9da2b0", "B": "#2b2c33", "H": "#4a4c57", "D": "#1b1c22",
        "l": "#5d6070", "L": "#8a8ea0", "P": "#ff6a5e", "p": "#b82a26"})


EGG = [
    "................",
    "......####......",
    ".....######.....",
    "....########....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    "..############..",
    "..############..",
    "...##########...",
    "...##########...",
    "....########....",
    "......####......",
    "................",
]


def spawn_egg(base, spot, spots):
    """Shaded egg (lit from the top left) with clustered speckles and a dark outline."""
    body, marks = ramp(base, 4, spread=0.1, hue_shift=7), ramp(spot, 3, spread=0.1, hue_shift=6)
    t = Tex(16)
    for y, row in enumerate(EGG):
        for x, ch in enumerate(row):
            if ch == "#":
                lightness = (x - 7.5) * 0.55 + (y - 8) * 0.75
                t.set(x, y, body[3] if lightness < -4.2 else body[2] if lightness < 0.8 else body[1] if lightness < 4.4 else body[0])
    for x, y, cells in spots:
        for dx, dy in cells:
            if EGG[y + dy][x + dx] == "#":
                t.set(x + dx, y + dy, marks[1] if (x + dx) + (y + dy) < 17 else marks[0])
    t.outline(ramp(base, 4, spread=0.16, hue_shift=10)[0])
    return t


def fixture_spawn_egg():
    return spawn_egg("#b5a577", "#34302c", ((5, 3, CHIP), (9, 5, BLOB), (4, 8, SMEAR), (10, 9, CHIP), (6, 11, BLOB)))


def tube_moth_spawn_egg():
    return spawn_egg("#e0dcc4", "#8a7650", ((6, 3, BLOB), (9, 6, CHIP), (4, 7, CHIP), (8, 10, SMEAR), (5, 11, CHIP)))


# ---------------------------------------------------------------- Entities
def box_faces(u, v, w, h, d):
    """Rects (x, y, width, height) of the six faces of a standard Minecraft box-UV cube."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
    }


def paint(t, rect, fn):
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            t.set(x0 + x, y0 + y, fn(x, y, w, h))


def fixture_texture():
    """The Fixture. UV layout must match client/FixtureModel.java."""
    t = Tex(64)
    bulb_core, bulb_warm = "#fff6c4", "#ffe07a"

    def metal_side(x, y, w, h):  # lit from the front-left, with a machined joint every 9 px
        if h > 4 and y % 9 == 8:
            return METAL[0]
        if h > 4 and y % 9 == 0:
            return METAL[3]
        return METAL[2] if x == 0 else METAL[1]

    def pleats(x, y, w, h):  # fabric folds running top to bottom, hem at the bottom, lit rim at the top
        if y == h - 1:
            return SHADE[0]
        if y == 0:
            return SHADE[4]
        return (SHADE[3], SHADE[2], SHADE[1])[x % 3]

    def shade_top(x, y, w, h):
        edge = x in (0, w - 1) or y in (0, h - 1)
        return SHADE[1] if edge else SHADE[2]

    def underside(x, y, w, h):  # looking up into the shade: the bulb's glow, stepped
        d = math.hypot(x - (w - 1) / 2, y - (h - 1) / 2)
        return bulb_core if d < 1.6 else bulb_warm if d < 2.8 else "#b99a4c" if d < 4.2 else SHADE[0]

    base = box_faces(0, 0, 8, 1, 8)
    paint(t, base["top"], lambda x, y, w, h: METAL[3] if x in (0, 7) or y in (0, 7) else METAL[1] if 3 <= x <= 4 and 3 <= y <= 4 else METAL[2])
    paint(t, base["bottom"], lambda x, y, w, h: METAL[0])
    for side in ("east", "north", "west", "south"):
        paint(t, base[side], lambda x, y, w, h: METAL[1])

    pole = box_faces(0, 10, 2, 36, 2)
    arm = box_faces(8, 10, 1, 22, 1)
    hand = box_faces(16, 32, 3, 1, 1)
    for part in (pole, arm, hand):
        for side in ("east", "north", "west", "south"):
            paint(t, part[side], metal_side)
        paint(t, part["top"], lambda x, y, w, h: METAL[3])
        paint(t, part["bottom"], lambda x, y, w, h: METAL[0])

    lower = box_faces(16, 10, 10, 4, 10)
    upper = box_faces(32, 0, 6, 3, 6)
    for part in (lower, upper):
        for side in ("east", "north", "west", "south"):
            paint(t, part[side], pleats)
        paint(t, part["top"], shade_top)
    paint(t, lower["bottom"], underside)
    paint(t, upper["bottom"], lambda x, y, w, h: SHADE[0])
    paint(t, upper["top"], lambda x, y, w, h: "#14110d" if 2 <= x <= 3 and 2 <= y <= 3 else shade_top(x, y, w, h))
    # an old water stain on the front of the shade
    fx, fy = lower["north"][0], lower["north"][1]
    for dx, dy in ((6, 1), (7, 1), (6, 2), (7, 2), (7, 3 - 1)):
        t.set(fx + dx, fy + dy, SHADE[1] if dy == 1 else SHADE[0])

    bulb = box_faces(16, 26, 3, 2, 3)
    for side in ("east", "north", "west", "south"):
        paint(t, bulb[side], lambda x, y, w, h: bulb_core if y == 0 or x == 1 else bulb_warm)
    paint(t, bulb["top"], lambda x, y, w, h: METAL[2])
    paint(t, bulb["bottom"], lambda x, y, w, h: bulb_core if (x, y) == (1, 1) else bulb_warm)
    return t


def moth_texture():
    """Tube Moth. UV layout must match client/TubeMothModel.java."""
    fur = ramp("#b9b08d", 4, spread=0.09, hue_shift=7)
    wing = ramp("#e4e6dc", 4, spread=0.085, hue_shift=4)
    eye, dust = "#1b1512", "#8a7650"
    t = Tex(32)
    body = box_faces(0, 0, 2, 2, 5)
    # fuzzy thorax at the front, banded abdomen behind
    paint(t, body["top"], lambda x, y, w, h: fur[3] if y < 2 else fur[2] if y % 2 == 0 else fur[1])
    paint(t, body["bottom"], lambda x, y, w, h: fur[1] if y % 2 == 0 else fur[0])
    for side in ("east", "west"):
        paint(t, body[side], lambda x, y, w, h: (fur[2] if x % 2 == 0 else fur[1]) if y == 0 else fur[0])
    paint(t, body["north"], lambda x, y, w, h: eye if y == 0 else fur[2])
    paint(t, body["south"], lambda x, y, w, h: fur[1] if y == 0 else fur[0])

    def membrane(x, y, w, h):
        if x in (0, w - 1) or y in (0, h - 1):
            return wing[0]          # darker margin all the way round
        if y == h // 2:
            return wing[1]          # the main vein
        if (x, y) in ((1, 1), (4, 1)):
            return dust             # a dusty eye-spot on each half
        return wing[3] if y < h // 2 else wing[2]

    flat = box_faces(0, 8, 6, 0, 5)
    paint(t, flat["top"], membrane)
    paint(t, flat["bottom"], lambda x, y, w, h: wing[0] if x in (0, w - 1) or y in (0, h - 1) else wing[1])
    return t


def icon():
    img = wallpaper().image().resize((64, 64), Image.NEAREST)
    d = ImageDraw.Draw(img)
    d.rectangle((22, 14, 41, 63), fill=(5, 5, 8, 255))
    d.rectangle((20, 12, 43, 14), fill=(88, 66, 40, 255))
    d.rectangle((24, 2, 39, 10), fill=(15, 59, 32, 255))
    d.rectangle((26, 4, 37, 8), fill=(141, 255, 168, 255))
    return img


# ---------------------------------------------------------------- build + review
# name -> (kind, builder, tileable). tileable=False where the design deliberately frames the block
# edge (a ceiling tile's bevel, a lamp's surround) or is not a repeating surface at all.
TEXTURES = {
    "damp_carpet": ("block", damp_carpet, True),
    "wallpaper": ("block", wallpaper, True),
    "wallpaper_baseboard": ("block", wallpaper_baseboard, False),
    "ceiling_tile": ("block", ceiling_tile, False),
    "fluorescent_light": ("block", lambda: light_panel(True), False),
    "fluorescent_light_off": ("block", lambda: light_panel(False), False),
    "pool_tile": ("block", white_tile, True),
    "pool_tile_blue": ("block", blue_tile, True),
    "pool_light": ("block", pool_light, False),
    "garage_concrete": ("block", garage_concrete, True),
    "garage_floor": ("block", garage_floor, True),
    "garage_line": ("block", garage_line, True),
    "hazard_pillar": ("block", hazard_pillar, True),
    "sodium_lamp": ("block", sodium_lamp, False),
    "exit_sign": ("block", exit_sign, False),
    "key_fob": ("item", key_fob, False),
    "fixture_spawn_egg": ("item", fixture_spawn_egg, False),
    "tube_moth_spawn_egg": ("item", tube_moth_spawn_egg, False),
    "fixture": ("entity", fixture_texture, False),
    "tube_moth": ("entity", moth_texture, False),
}


def overview(built, path):
    """One sheet with every texture zoomed and tiled, for a single look at the whole family."""
    cells = []
    for name, (t, tileable) in built.items():
        zoom = render_zoom(t, scale=max(2, 128 // t.w))
        tiled = render_tiled(t, 3, scale=2) if tileable else None
        cells.append((name, zoom, tiled))
    cols, cw, ch = 4, 250, 150
    sheet = Image.new("RGBA", (cols * cw, ((len(cells) + cols - 1) // cols) * ch), (38, 38, 44, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, zoom, tiled) in enumerate(cells):
        x, y = (i % cols) * cw + 6, (i // cols) * ch + 4
        d.text((x, y), name, fill=(225, 225, 235, 255))
        sheet.alpha_composite(zoom, (x, y + 14))
        if tiled:
            sheet.alpha_composite(tiled, (x + zoom.width + 8, y + 14))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sheet.convert("RGB").save(path)


def main():
    built, warnings_found = {}, 0
    for name, (kind, builder, tileable) in TEXTURES.items():
        t = builder()
        t.save(os.path.join(TEX, kind, name + ".png"))
        msgs = preview(t, os.path.join(PREVIEWS, name + ".png"), tileable=tileable, name=name)
        warns = [m for m in msgs if m.startswith("WARN")]
        warnings_found += len(warns)
        summary = "; ".join(m for m in msgs if not m.startswith("WARN"))
        print(f"{name:24s} {summary}")
        for w in warns:
            print(f"{'':24s} {w}")
        built[name] = (t, tileable)

    frames = threshold_frames()
    anim = Anim(frames, frametime=10, interpolate=True)
    anim.save(os.path.join(TEX, "block", "threshold.png"))
    anim.preview(os.path.join(PREVIEWS, "threshold_frames.png"))
    msgs = validate(frames[0], tileable=True, name="threshold")
    warnings_found += sum(m.startswith("WARN") for m in msgs)
    print(f"{'threshold (%d frames)' % len(frames):24s} {'; '.join(msgs)}; frame-to-frame change {', '.join('%d%%' % round(f * 100) for f in anim.flicker_report())}")
    built["threshold"] = (frames[0], True)

    icon().save(os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms", "icon.png"))
    overview(built, os.path.join(PREVIEWS, "overview.png"))
    print(f"\n{len(built)} textures written to {TEX}\n{warnings_found} validator warning(s); previews in {PREVIEWS}")


if __name__ == "__main__":
    main()
