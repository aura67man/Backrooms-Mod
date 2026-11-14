#!/usr/bin/env python3
"""Generates every texture in the mod procedurally (no hand-drawn or third-party art).
Usage: python3 tools/gen_textures.py"""
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms", "textures")


def clamp(v):
    return max(0, min(255, int(v)))


def jitter(rgb, rng, amount):
    d = rng.randint(-amount, amount)
    return tuple(clamp(c + d) for c in rgb)


def noise_tile(base, amount, seed, size=16):
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = jitter(base, rng, amount) + (255,)
    return img


def blotch(img, colour, count, radius, seed, alpha=110):
    """Soft darker/lighter patches that wrap around the tile edges."""
    rng = random.Random(seed)
    w, h = img.size
    px = img.load()
    for _ in range(count):
        cx, cy, r = rng.randrange(w), rng.randrange(h), rng.uniform(radius * 0.6, radius)
        for y in range(h):
            for x in range(w):
                dx = min(abs(x - cx), w - abs(x - cx))
                dy = min(abs(y - cy), h - abs(y - cy))
                d = (dx * dx + dy * dy) ** 0.5
                if d < r:
                    a = alpha * (1 - d / r) / 255
                    o = px[x, y]
                    px[x, y] = tuple(clamp(o[i] * (1 - a) + colour[i] * a) for i in range(3)) + (255,)


def save(img, kind, name):
    path = os.path.join(TEX, kind)
    os.makedirs(path, exist_ok=True)
    img.save(os.path.join(path, name + ".png"))


# ---------------------------------------------------------------- Level 0
def damp_carpet():
    img = noise_tile((146, 128, 78), 9, 1)
    blotch(img, (92, 82, 52), 4, 5, 2)
    px = img.load()
    rng = random.Random(3)
    for _ in range(40):  # carpet fibres
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = jitter((168, 150, 98), rng, 6) + (255,)
    return img


def wallpaper(baseboard=False):
    img = noise_tile((201, 178, 92), 4, 10)
    px = img.load()
    rng = random.Random(11)
    for y in range(16):
        for x in range(16):
            if x % 8 == 0:  # pinstripe
                px[x, y] = jitter((178, 154, 74), rng, 3) + (255,)
            # small repeating chevron motif
            if (x % 8, y % 8) in ((4, 2), (3, 3), (5, 3), (4, 6)):
                px[x, y] = jitter((214, 194, 112), rng, 3) + (255,)
    blotch(img, (170, 146, 70), 2, 6, 12, alpha=60)
    if baseboard:
        for y in range(12, 16):
            for x in range(16):
                c = (88, 66, 40) if y > 12 else (116, 90, 56)
                px[x, y] = jitter(c, rng, 5) + (255,)
    return img


def ceiling_tile():
    img = noise_tile((214, 210, 194), 4, 20)
    px = img.load()
    rng = random.Random(21)
    for _ in range(34):  # acoustic pinholes
        px[rng.randrange(1, 15), rng.randrange(1, 15)] = jitter((168, 164, 150), rng, 8) + (255,)
    for i in range(16):
        for x, y in ((i, 0), (0, i)):
            px[x, y] = jitter((176, 172, 158), rng, 4) + (255,)
    return img


def light_panel(frame_fn, glow, tube, seed, dim=False):
    img = frame_fn()
    px = img.load()
    rng = random.Random(seed)
    for y in range(2, 14):
        for x in range(2, 14):
            c = tube if x in (5, 6, 9, 10) else glow
            if x in (2, 13) or y in (2, 13):
                c = tuple(int(v * 0.8) for v in c)
            px[x, y] = jitter(c, rng, 3 if not dim else 6) + (255,)
    return img


# ---------------------------------------------------------------- Poolrooms
def pool_tile(base, grout, seed, tint=6):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(seed)
    shades = {}
    for y in range(16):
        for x in range(16):
            if x % 4 == 3 or y % 4 == 3:
                px[x, y] = jitter(grout, rng, 3) + (255,)
            else:
                key = (x // 4, y // 4)
                if key not in shades:
                    shades[key] = jitter(base, rng, tint)
                c = shades[key]
                if x % 4 == 0 and y % 4 == 0:  # glaze highlight
                    c = tuple(clamp(v + 12) for v in c)
                px[x, y] = c + (255,)
    return img


def pool_light():
    img = pool_tile((236, 243, 246), (196, 210, 216), 30)
    px = img.load()
    for y in range(2, 14):
        for x in range(2, 14):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            px[x, y] = (clamp(232 - d * 6), clamp(255 - d * 2), clamp(250 - d * 2), 255)
    return img


# ---------------------------------------------------------------- Car park
def garage_concrete():
    img = noise_tile((124, 124, 120), 6, 40)
    blotch(img, (100, 100, 98), 3, 5, 41, alpha=70)
    px = img.load()
    rng = random.Random(42)
    for x in range(16):  # formwork seam
        px[x, 7] = jitter((98, 98, 95), rng, 4) + (255,)
    for _ in range(6):  # tie holes
        px[rng.randrange(16), rng.choice((2, 12))] = (84, 84, 82, 255)
    return img


def garage_floor():
    img = noise_tile((66, 66, 70), 5, 50)
    blotch(img, (38, 38, 42), 3, 4, 51, alpha=120)
    return img


def garage_line():
    img = garage_floor()
    px = img.load()
    rng = random.Random(52)
    for y in range(16):
        for x in range(6, 10):
            if rng.random() > 0.12:  # worn paint
                px[x, y] = jitter((206, 170, 44), rng, 10) + (255,)
    return img


def hazard_pillar():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(60)
    for y in range(16):
        for x in range(16):
            c = (212, 172, 36) if ((x + y) // 4) % 2 == 0 else (34, 34, 36)
            if rng.random() < 0.08:
                c = (120, 118, 112)  # chipped paint showing concrete
            px[x, y] = jitter(c, rng, 7) + (255,)
    return img


# ---------------------------------------------------------------- Shared
def threshold():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    rng = random.Random(70)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            v = clamp(4 + d * 1.6 + rng.randint(-2, 2))
            px[x, y] = (v, clamp(v * 0.9), clamp(v * 1.5 + (4 if y % 3 == 0 else 0)), 255)
    return img


FONT = {
    "E": ["###", "#..", "##.", "#..", "###"],
    "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
    "I": ["###", ".#.", ".#.", ".#.", "###"],
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
}


def exit_sign():
    img = noise_tile((22, 34, 26), 3, 80)
    px = img.load()
    for i in range(16):
        for x, y in ((i, 3), (i, 12)):
            px[x, y] = (10, 16, 12, 255)
    for y in range(4, 12):
        for x in range(16):
            px[x, y] = (16, 58, 30, 255)
    for n, ch in enumerate("EXIT"):
        for ry, row in enumerate(FONT[ch]):
            for rx, bit in enumerate(row):
                if bit == "#":
                    px[1 + n * 4 + rx - (1 if n == 3 else 0) + (0 if n < 3 else 1), 5 + ry] = (126, 255, 150, 255)
    return img


# ---------------------------------------------------------------- Items
def key_fob():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((1, 1, 6, 6), outline=(170, 170, 178, 255))  # key ring
    d.rounded_rectangle((5, 5, 13, 14), radius=2, fill=(30, 30, 34, 255), outline=(12, 12, 14, 255))
    d.rectangle((7, 7, 11, 8), fill=(70, 70, 78, 255))  # lock button
    d.rectangle((8, 10, 10, 12), fill=(196, 40, 36, 255))  # panic button
    img.putpixel((9, 11), (255, 120, 110, 255))
    return img


def spawn_egg(base, spots, seed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.2, (y - 8.2) / (6.8 if y < 8 else 5.8)
            if dx * dx + dy * dy <= 1:
                shade = 1.0 - 0.25 * max(0, dx + dy)
                c = spots if rng.random() < 0.22 else base
                px[x, y] = tuple(clamp(v * shade) for v in c) + (255,)
    return img


# ---------------------------------------------------------------- Entities
def paint_box(img, u, v, w, h, d, colour_fn):
    """Paint the six faces of a standard Minecraft box-UV cube. colour_fn(face, x, y, fw, fh) -> rgba."""
    faces = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h),
    }
    px = img.load()
    for face, (fx, fy, fw, fh) in faces.items():
        for y in range(fh):
            for x in range(fw):
                px[fx + x, fy + y] = colour_fn(face, x, y, fw, fh)


def fixture_texture():
    """The Fixture: a tall floor-lamp-shaped stalker. Layout must match FixtureModel."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    rng = random.Random(90)

    def metal(face, x, y, fw, fh):
        c = (38, 36, 34) if (x + y) % 5 else (54, 50, 46)
        return jitter(c, rng, 5) + (255,)

    def shade(face, x, y, fw, fh):
        if face == "bottom":  # looking up into the shade: dim sickly glow
            dd = ((x - fw / 2 + 0.5) ** 2 + (y - fh / 2 + 0.5) ** 2) ** 0.5
            return (clamp(214 - dd * 22), clamp(196 - dd * 24), clamp(120 - dd * 20), 255)
        if face == "top":
            return jitter((112, 100, 74), rng, 6) + (255,)
        c = (176, 160, 118) if x % 3 else (150, 134, 96)  # pleated fabric
        if y >= fh - 1:
            c = (96, 84, 60)  # hem
        stain = 0.82 if rng.random() < 0.12 else 1.0
        return jitter(tuple(int(k * stain) for k in c), rng, 5) + (255,)

    def bulb(face, x, y, fw, fh):
        return jitter((238, 226, 150), rng, 8) + (255,)

    def arm(face, x, y, fw, fh):
        c = (30, 28, 27) if y % 6 else (62, 58, 54)  # jointed like a desk-lamp arm
        return jitter(c, rng, 4) + (255,)

    paint_box(img, 0, 0, 8, 1, 8, metal)       # base
    paint_box(img, 0, 10, 2, 26, 2, metal)     # pole
    paint_box(img, 8, 10, 1, 18, 1, arm)       # arms (shared)
    paint_box(img, 16, 10, 10, 4, 10, shade)   # lower shade
    paint_box(img, 32, 0, 6, 3, 6, shade)      # upper shade
    paint_box(img, 0, 40, 2, 2, 2, bulb)       # bulb
    paint_box(img, 12, 40, 3, 1, 1, arm)       # fingers (shared)
    return img


def moth_texture():
    """Tube Moth. Layout must match TubeMothModel."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rng = random.Random(95)

    def body(face, x, y, fw, fh):
        if face == "north" and y == 0 and x in (0, fw - 1):
            return (20, 16, 14, 255)  # eyes
        c = (196, 188, 160) if (y + x) % 3 else (150, 140, 112)
        return jitter(c, rng, 6) + (255,)

    def wing(face, x, y, fw, fh):
        edge = x == 0 or y == 0 or x == fw - 1 or y == fh - 1
        c = (176, 170, 142) if edge else (226, 222, 198)
        if (x, y) in ((2, 2), (3, 2)):
            c = (120, 104, 70)  # dusty wing spot
        return jitter(c, rng, 5) + (235,)

    paint_box(img, 0, 0, 2, 2, 5, body)
    paint_box(img, 0, 8, 6, 0, 5, wing)
    return img


def icon():
    img = Image.new("RGBA", (64, 64))
    wall = wallpaper().resize((64, 64), Image.NEAREST)
    img.paste(wall)
    d = ImageDraw.Draw(img)
    d.rectangle((22, 14, 41, 63), fill=(6, 6, 9, 255))
    d.rectangle((20, 12, 43, 14), fill=(88, 66, 40, 255))
    d.rectangle((26, 6, 37, 10), fill=(16, 58, 30, 255))
    d.rectangle((28, 7, 35, 9), fill=(126, 255, 150, 255))
    return img


def main():
    save(damp_carpet(), "block", "damp_carpet")
    save(wallpaper(), "block", "wallpaper")
    save(wallpaper(True), "block", "wallpaper_baseboard")
    save(ceiling_tile(), "block", "ceiling_tile")
    save(light_panel(ceiling_tile, (250, 246, 214), (255, 255, 240), 22), "block", "fluorescent_light")
    save(light_panel(ceiling_tile, (104, 104, 96), (128, 128, 118), 23, dim=True), "block", "fluorescent_light_off")
    save(pool_tile((236, 243, 246), (196, 210, 216), 30), "block", "pool_tile")
    save(pool_tile((72, 152, 192), (44, 104, 140), 31, tint=12), "block", "pool_tile_blue")
    save(pool_light(), "block", "pool_light")
    save(garage_concrete(), "block", "garage_concrete")
    save(garage_floor(), "block", "garage_floor")
    save(garage_line(), "block", "garage_line")
    save(hazard_pillar(), "block", "hazard_pillar")
    save(light_panel(garage_concrete, (255, 168, 58), (255, 206, 120), 61), "block", "sodium_lamp")
    save(threshold(), "block", "threshold")
    save(exit_sign(), "block", "exit_sign")
    save(key_fob(), "item", "key_fob")
    save(spawn_egg((176, 160, 118), (38, 36, 34), 96), "item", "fixture_spawn_egg")
    save(spawn_egg((226, 222, 198), (120, 104, 70), 97), "item", "tube_moth_spawn_egg")
    save(fixture_texture(), "entity", "fixture")
    save(moth_texture(), "entity", "tube_moth")
    icon().save(os.path.join(ROOT, "src", "main", "resources", "assets", "backrooms", "icon.png"))
    print("textures written to", TEX)


if __name__ == "__main__":
    main()
