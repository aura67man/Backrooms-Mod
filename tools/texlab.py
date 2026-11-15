"""
texlab - a tiny Minecraft texture workbench.

Workflow: define a texture (ASCII grid + palette, or draw with helpers),
render previews (zoomed, tiled, isometric block, palette strip), LOOK at them,
then tweak. Validation flags common pixel-art problems.

    from texlab import *
    t = Tex(16)                       # 16x16 canvas
    t.fill("#7f7f7f")
    t.noise(["#6e6e6e", "#8a8a8a"], 0.3, seed=1)
    preview(t, "out/stone_preview.png")
"""
from __future__ import annotations
import math, random, os
from PIL import Image, ImageDraw
import numpy as np


# ---------- colour helpers ----------
def hex2rgba(c):
    if isinstance(c, (tuple, list)):
        return tuple(c) if len(c) == 4 else (*c, 255)
    c = c.lstrip("#")
    if len(c) == 3:
        c = "".join(ch * 2 for ch in c)
    r, g, b = int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16)
    a = int(c[6:8], 16) if len(c) == 8 else 255
    return (r, g, b, a)


def rgba2hex(p):
    return "#%02x%02x%02x" % tuple(p[:3])


def lum(p):
    return 0.2126 * p[0] + 0.7152 * p[1] + 0.0722 * p[2]


def shift(c, dh=0, ds=0, dv=0):
    """Hue-shift ramp helper: pixel artists shift hue as they darken/lighten.
    dh in degrees, ds/dv in -1..1."""
    import colorsys
    r, g, b, a = hex2rgba(c)
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    h = (h + dh / 360) % 1
    s = min(1, max(0, s + ds))
    v = min(1, max(0, v + dv))
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def ramp(base, steps=5, spread=0.14, hue_shift=8):
    """Make a dark->light ramp from one colour. Shadows drift cool, lights drift warm
    (reads far better than plain black/white mixing). Returns list of hex strings."""
    out = []
    mid = steps // 2
    for i in range(steps):
        k = i - mid
        out.append(rgba2hex(shift(base, dh=-k * hue_shift, ds=-k * 0.03, dv=k * spread)))
    return out


# ---------- the canvas ----------
class Tex:
    def __init__(self, w=16, h=None):
        h = h or w
        self.w, self.h = w, h
        self.px = np.zeros((h, w, 4), dtype=np.uint8)

    # -- construction --
    @classmethod
    def from_ascii(cls, rows, palette):
        """rows: list of equal-length strings. palette: {char: colour}. '.' defaults transparent."""
        pal = {".": (0, 0, 0, 0)}
        pal.update({k: hex2rgba(v) for k, v in palette.items()})
        h, w = len(rows), len(rows[0])
        t = cls(w, h)
        for y, row in enumerate(rows):
            if len(row) != w:
                raise ValueError(f"row {y} has length {len(row)}, expected {w}")
            for x, ch in enumerate(row):
                if ch not in pal:
                    raise ValueError(f"char {ch!r} at ({x},{y}) not in palette")
                t.px[y, x] = pal[ch]
        return t

    @classmethod
    def load(cls, path):
        im = Image.open(path).convert("RGBA")
        t = cls(*im.size)
        t.px = np.array(im)
        return t

    def copy(self):
        n = Tex(self.w, self.h)
        n.px = self.px.copy()
        return n

    # -- drawing --
    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y, x] = hex2rgba(c)

    def get(self, x, y):
        return tuple(self.px[y % self.h, x % self.w])

    def fill(self, c):
        self.px[:, :] = hex2rgba(c)

    def rect(self, x0, y0, x1, y1, c, outline=False):
        """Inclusive corners."""
        c = hex2rgba(c)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if outline and not (x in (x0, x1) or y in (y0, y1)):
                    continue
                self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy; x0 += sx
            if e2 <= dx:
                err += dx; y0 += sy

    def circle(self, cx, cy, r, c, fill=True):
        for y in range(self.h):
            for x in range(self.w):
                d = math.hypot(x - cx, y - cy)
                if (fill and d <= r) or (not fill and abs(d - r) < 0.7):
                    self.set(x, y, c)

    def noise(self, colours, density=0.3, seed=0, mask=None):
        """Scatter random colours over a fraction of pixels. Tiles trivially."""
        rnd = random.Random(seed)
        for y in range(self.h):
            for x in range(self.w):
                if mask is not None and not mask(x, y):
                    continue
                if rnd.random() < density:
                    self.set(x, y, rnd.choice(colours))

    def blotches(self, colours, count=6, size=(2, 4), seed=0):
        """Little clustered patches (stone, dirt, ores). Wraps around edges so it tiles."""
        rnd = random.Random(seed)
        for _ in range(count):
            cx, cy = rnd.randrange(self.w), rnd.randrange(self.h)
            c = rnd.choice(colours)
            n = rnd.randint(*size)
            for _ in range(n * 2):
                self.set((cx + rnd.randint(-1, 1)) % self.w, (cy + rnd.randint(-1, 1)) % self.h, c)

    def gradient_v(self, top, bottom):
        a, b = np.array(hex2rgba(top), float), np.array(hex2rgba(bottom), float)
        for y in range(self.h):
            t = y / max(1, self.h - 1)
            self.px[y, :] = (a * (1 - t) + b * t).astype(np.uint8)

    def dither(self, c1, c2, x0=0, y0=0, x1=None, y1=None, level=0.5):
        """Ordered (Bayer 4x4) dither between two colours inside a rect."""
        bayer = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]
        x1 = self.w - 1 if x1 is None else x1
        y1 = self.h - 1 if y1 is None else y1
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c1 if bayer[y % 4][x % 4] / 16 < level else c2)

    def bevel(self, light, dark, amount=1):
        """Classic block bevel: light top/left edge, dark bottom/right edge."""
        for i in range(amount):
            for k in range(self.w - i):
                if self.px[i, k, 3]: self.set(k, i, light)
            for k in range(self.h - i):
                if self.px[k, i, 3]: self.set(i, k, light)
            for k in range(i, self.w):
                self.set(k, self.h - 1 - i, dark)
            for k in range(i, self.h):
                self.set(self.w - 1 - i, k, dark)

    def outline(self, c, include_diagonals=False):
        """1px outline around opaque pixels (for items / sprites)."""
        c = hex2rgba(c)
        a = self.px[:, :, 3] > 0
        out = np.zeros_like(a)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if (dx or dy) and (include_diagonals or not (dx and dy)):
                    out |= np.roll(np.roll(a, dy, 0), dx, 1)
        out &= ~a
        self.px[out] = c

    def replace(self, old, new):
        o, n = hex2rgba(old), hex2rgba(new)
        m = np.all(self.px == o, axis=2)
        self.px[m] = n

    def flip_h(self):
        self.px = self.px[:, ::-1].copy()

    def shade_by(self, fn):
        """Apply fn(x, y, rgba) -> rgba to every pixel."""
        for y in range(self.h):
            for x in range(self.w):
                self.px[y, x] = hex2rgba(fn(x, y, tuple(self.px[y, x])))

    # -- io --
    def image(self):
        return Image.fromarray(self.px, "RGBA")

    def save(self, path):
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        self.image().save(path)

    def palette(self):
        """Unique opaque colours, sorted dark->light, with pixel counts."""
        flat = self.px.reshape(-1, 4)
        flat = flat[flat[:, 3] > 0]
        cols, counts = np.unique(flat, axis=0, return_counts=True)
        items = sorted(zip(map(tuple, cols), counts), key=lambda t: lum(t[0]))
        return items


# ---------- analysis ----------
def tile_seam_score(t: Tex):
    """How visible is the seam when tiled? Mean colour jump across the wrap edge vs
    the mean jump between ordinary neighbours. ~1.0 = seamless, >1.5 = visible seam."""
    p = t.px[:, :, :3].astype(float)
    inner = (np.abs(p[:, 1:] - p[:, :-1]).mean() + np.abs(p[1:] - p[:-1]).mean()) / 2
    wrap = (np.abs(p[:, 0] - p[:, -1]).mean() + np.abs(p[0] - p[-1]).mean()) / 2
    return wrap / max(inner, 1e-6)


def banding_score(t: Tex):
    """Fraction of pixels whose 4 neighbours are all a different colour (isolated 'noise' px).
    High = busy/noisy, low = clean clusters."""
    p = t.px
    iso = 0
    tot = 0
    for y in range(t.h):
        for x in range(t.w):
            c = tuple(p[y, x])
            n = [tuple(p[(y + dy) % t.h, (x + dx) % t.w]) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            tot += 1
            if all(m != c for m in n):
                iso += 1
    return iso / tot


def validate(t: Tex, tileable=False, name="texture"):
    msgs = []
    if t.w != t.h and t.w % t.h and t.h % t.w:
        msgs.append(f"size {t.w}x{t.h} isn't square or an animation strip")
    if t.w & (t.w - 1) or t.h & (t.h - 1):
        msgs.append(f"size {t.w}x{t.h} not a power of two (resource packs can break)")
    pal = t.palette()
    n = len(pal)
    msgs.append(f"{n} colours used")
    if n > 12 and t.w <= 16:
        msgs.append("WARN: lots of colours for 16px - Minecraft style usually 4-8 per texture")
    if n:
        lums = [lum(c) for c, _ in pal]
        spread = max(lums) - min(lums)
        if spread < 25:
            msgs.append(f"WARN: very low contrast (value spread {spread:.0f}/255) - will look flat/muddy")
        if spread > 200 and n > 3:
            msgs.append("note: very high contrast; fine for ores/gems, harsh for plain blocks")
        for i, (c1, _) in enumerate(pal):
            for c2, _ in pal[i + 1:]:
                dist = math.sqrt(sum((int(a) - int(b)) ** 2 for a, b in zip(c1[:3], c2[:3])))
                if 0 < dist < 12:
                    msgs.append(f"WARN: {rgba2hex(c1)} and {rgba2hex(c2)} are near-identical - merge them")
    b = banding_score(t)
    if b > 0.25:
        msgs.append(f"WARN: {b:.0%} of pixels are isolated singletons - looks like static, cluster them")
    if tileable:
        s = tile_seam_score(t)
        msgs.append(f"seam score {s:.2f} " + ("(good)" if s < 1.3 else "WARN: visible tiling seam"))
    return msgs


# ---------- renderers ----------
def _up(img, scale):
    return img.resize((img.width * scale, img.height * scale), Image.NEAREST)


def _checker(w, h, s=8):
    im = Image.new("RGBA", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(0, h, s):
        for x in range(0, w, s):
            col = (200, 200, 200, 255) if (x // s + y // s) % 2 == 0 else (160, 160, 160, 255)
            d.rectangle([x, y, x + s - 1, y + s - 1], fill=col)
    return im


def render_zoom(t: Tex, scale=None):
    scale = scale or max(1, 256 // t.w)
    bg = _checker(t.w * scale, t.h * scale)
    bg.alpha_composite(_up(t.image(), scale))
    return bg


def render_tiled(t: Tex, n=4, scale=None):
    scale = scale or max(1, 256 // (t.w * n) or 1) * 2
    big = Image.new("RGBA", (t.w * n, t.h * n))
    for j in range(n):
        for i in range(n):
            big.paste(t.image(), (i * t.w, j * t.h))
    return _up(big, scale)


def render_palette(t: Tex, sw=28):
    pal = t.palette()
    im = Image.new("RGBA", (max(1, len(pal)) * sw, sw + 14), (30, 30, 30, 255))
    d = ImageDraw.Draw(im)
    for i, (c, n) in enumerate(pal):
        d.rectangle([i * sw, 0, i * sw + sw - 1, sw - 1], fill=tuple(c))
        d.text((i * sw + 2, sw + 1), str(n), fill=(220, 220, 220, 255))
    return im


def render_iso_block(top: Tex, left: Tex = None, right: Tex = None, scale=12):
    """Isometric cube like the inventory icon. left/right default to top darkened (MC shades faces)."""
    left = left or top
    right = right or top

    def face_shade(im, f):
        a = np.array(im).astype(float)
        a[:, :, :3] *= f
        return Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA")

    T = _up(top.image(), scale)
    L = face_shade(_up(left.image(), scale), 0.8 if left is top else 1.0)
    R = face_shade(_up(right.image(), scale), 0.6 if right is top else 0.85)
    s = T.width
    W, H = int(s * 2), int(s * 2.1)
    out = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    def affine(face, a, b, c, d, e, f):
        return face.transform((W, H), Image.AFFINE, (a, b, c, d, e, f), Image.NEAREST)

    # top: unit square -> rhombus. dest = M*src+t  => PIL needs inverse
    def paste_face(face, origin, vx, vy):
        # map src (u,v) in [0,s] -> dest origin + u*vx/s + v*vy/s
        ox, oy = origin
        a, c = vx[0] / s, vy[0] / s
        d, e = vx[1] / s, vy[1] / s
        det = a * e - c * d
        inv = np.array([[e, -c], [-d, a]]) / det
        tx = -(inv[0, 0] * ox + inv[0, 1] * oy)
        ty = -(inv[1, 0] * ox + inv[1, 1] * oy)
        tr = face.transform((W, H), Image.AFFINE,
                            (inv[0, 0], inv[0, 1], tx, inv[1, 0], inv[1, 1], ty), Image.NEAREST)
        out.alpha_composite(tr)

    k = s * 0.5
    # top face rhombus: origin at left corner
    paste_face(T, (0, k * 0.9), (s * 0.5 * 2 / 2, -k * 0.9 * 1.0), (s * 0.5, k * 0.9))
    # left side
    paste_face(L, (0, k * 0.9), (s * 0.5, k * 0.9), (0, s * 0.95))
    # right side
    paste_face(R, (s * 0.5, k * 0.9 + k * 0.9), (s * 0.5, -k * 0.9), (0, s * 0.95))
    return out


def preview(t: Tex, path, tileable=True, name=None, extra_faces=None):
    """One contact sheet: zoom | 4x4 tiled | iso block | palette, plus validation text.
    This is the image I should LOOK at after every change."""
    name = name or os.path.splitext(os.path.basename(path))[0]
    zoom = render_zoom(t, scale=max(1, 224 // t.w))
    tiled = render_tiled(t, 4, scale=max(1, 224 // (t.w * 4)))
    tiled = tiled.resize((224, 224), Image.NEAREST) if tiled.width != 224 else tiled
    if t.w == t.h:
        faces = extra_faces or {}
        iso = render_iso_block(t, faces.get("left"), faces.get("right"), scale=max(1, 112 // t.w))
    else:
        iso = Image.new("RGBA", (10, 10))
    pal = render_palette(t)
    msgs = validate(t, tileable=tileable, name=name)

    pad = 12
    colw = [zoom.width, tiled.width, iso.width]
    W = sum(colw) + pad * 4
    H = max(zoom.height, tiled.height, iso.height) + pal.height + 24 + pad * 3 + 14 * (len(msgs) + 1)
    sheet = Image.new("RGBA", (max(W, pal.width + pad * 2), H), (38, 38, 44, 255))
    d = ImageDraw.Draw(sheet)
    x = pad
    for im, lab in ((zoom, "zoom"), (tiled, "tiled 4x4"), (iso, "block")):
        d.text((x, 4), lab, fill=(180, 180, 190, 255))
        sheet.alpha_composite(im, (x, 18))
        x += im.width + pad
    y = 18 + max(zoom.height, tiled.height, iso.height) + pad
    sheet.alpha_composite(pal, (pad, y))
    y += pal.height + pad
    d.text((pad, y), f"{name}  {t.w}x{t.h}", fill=(255, 255, 255, 255))
    for i, m in enumerate(msgs):
        col = (255, 190, 90, 255) if m.startswith("WARN") else (170, 200, 170, 255)
        d.text((pad, y + 14 * (i + 1)), m, fill=col)
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    sheet.convert("RGB").save(path)
    return msgs


def compare(textures: dict, path, scale=10):
    """Side-by-side of several textures (e.g. your stone vs dirt vs ore) at the same zoom,
    to check they belong to the same visual family."""
    ims = [(n, _up(t.image(), scale)) for n, t in textures.items()]
    W = sum(i.width for _, i in ims) + 12 * (len(ims) + 1)
    H = max(i.height for _, i in ims) + 40
    sheet = Image.new("RGBA", (W, H), (38, 38, 44, 255))
    d = ImageDraw.Draw(sheet)
    x = 12
    for n, im in ims:
        bg = _checker(im.width, im.height)
        bg.alpha_composite(im)
        sheet.alpha_composite(bg, (x, 22))
        d.text((x, 6), n, fill=(220, 220, 230, 255))
        x += im.width + 12
    sheet.convert("RGB").save(path)


def to_ascii(t: Tex):
    """Dump a texture back to ASCII + palette so I can edit pixel by pixel."""
    cols = {}
    sym = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    rows = []
    for y in range(t.h):
        r = ""
        for x in range(t.w):
            p = tuple(int(v) for v in t.px[y, x])
            if p[3] == 0:
                r += "."
            else:
                if p not in cols:
                    cols[p] = sym[len(cols)]
                r += cols[p]
        rows.append(r)
    return rows, {v: rgba2hex(k) for k, v in cols.items()}


# =====================================================================
# v2 additions
# =====================================================================
import json
from collections import deque


def _font():
    from PIL import ImageFont
    return ImageFont.load_default()


# ---- extra drawing -----------------------------------------------------
def _flood(self, x, y, c):
    """Bucket fill (4-way) from (x,y)."""
    c = hex2rgba(c)
    target = tuple(self.px[y, x])
    if target == c:
        return
    q = deque([(x, y)])
    while q:
        cx, cy = q.popleft()
        if not (0 <= cx < self.w and 0 <= cy < self.h) or tuple(self.px[cy, cx]) != target:
            continue
        self.px[cy, cx] = c
        q.extend([(cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)])


def _polygon(self, pts, c):
    d = ImageDraw.Draw(img := Image.new("L", (self.w, self.h), 0))
    d.polygon([(x + 0.5, y + 0.5) for x, y in pts], fill=255)
    m = np.array(img) > 0
    self.px[m] = hex2rgba(c)


def _mirror_x(self):
    """Left half -> right half (symmetrical sprites: swords, gems, faces)."""
    h = self.w // 2
    self.px[:, self.w - h:] = self.px[:, :h][:, ::-1]


def _light(self, dir=(-1, -1), strength=1, hi=None, lo=None):
    """Auto edge-lighting for sprites: pixels whose neighbour toward the light is empty get
    `hi`, pixels whose neighbour away from light is empty get `lo`. Quick depth for items."""
    a = self.px[:, :, 3] > 0
    out = self.px.copy()
    dx, dy = dir
    for y in range(self.h):
        for x in range(self.w):
            if not a[y, x]:
                continue
            tx, ty = x + dx, y + dy
            bx, by = x - dx, y - dy
            lit = not (0 <= tx < self.w and 0 <= ty < self.h and a[ty, tx])
            dark = not (0 <= bx < self.w and 0 <= by < self.h and a[by, bx])
            if lit and hi:
                out[y, x] = hex2rgba(hi)
            elif dark and lo:
                out[y, x] = hex2rgba(lo)
    self.px = out


def _remap(self, mapping):
    for old, new in mapping.items():
        self.replace(old, new)


def _quantize(self, palette):
    """Snap every pixel to the nearest colour of `palette` (hex list). Great for forcing a
    consistent pack palette across textures."""
    pal = np.array([hex2rgba(p)[:3] for p in palette], float)
    for y in range(self.h):
        for x in range(self.w):
            if self.px[y, x, 3]:
                d = ((pal - self.px[y, x, :3]) ** 2).sum(1)
                self.px[y, x, :3] = pal[d.argmin()].astype(np.uint8)


def _brick(self, brick, mortar, bw=8, bh=4, seed=0, var=None):
    """Running-bond bricks. var = list of alt brick colours for variation."""
    rnd = random.Random(seed)
    self.fill(mortar)
    for row, y0 in enumerate(range(0, self.h, bh)):
        off = (bw // 2) * (row % 2)
        for x0 in range(-off, self.w, bw):
            col = rnd.choice(var) if var else brick
            self.rect(max(0, x0 + 1), y0 + 1 if y0 + 1 < self.h else y0,
                      min(self.w - 1, x0 + bw - 1), min(self.h - 1, y0 + bh - 1), col)
            # wrap-around part for tiling
            if x0 < 0:
                self.rect(self.w + x0 + 1, y0 + 1, self.w - 1, min(self.h - 1, y0 + bh - 1), col)


def _planks(self, cols, mortar, bh=4, seed=0):
    rnd = random.Random(seed)
    self.fill(mortar)
    for y0 in range(0, self.h, bh):
        c = rnd.choice(cols)
        self.rect(0, y0, self.w - 1, y0 + bh - 2, c)
        for _ in range(3):
            self.set(rnd.randrange(self.w), y0 + rnd.randrange(bh - 1), rnd.choice(cols))


def _ore(self, ore_colours, count=5, seed=0, base_mask=None):
    """Drop 2-4px ore clusters (1 dark edge colour + 1-2 fill + 1 highlight) onto an existing base."""
    rnd = random.Random(seed)
    dark, mid, light = ore_colours[0], ore_colours[1], ore_colours[-1]
    for _ in range(count):
        cx, cy = rnd.randrange(1, self.w - 2), rnd.randrange(1, self.h - 2)
        cells = {(cx, cy)}
        for _ in range(rnd.randint(2, 4)):
            ax, ay = rnd.choice(list(cells))
            cells.add((ax + rnd.choice([-1, 0, 1]), ay + rnd.choice([-1, 0, 1])))
        for (x, y) in cells:
            self.set(x, y, mid)
        for (x, y) in cells:
            if (x + 1, y) not in cells or (x, y + 1) not in cells:
                self.set(x, y, dark)
        tl = min(cells, key=lambda p: p[0] + p[1])
        self.set(*tl, light)


for _n, _f in {"flood": _flood, "polygon": _polygon, "mirror_x": _mirror_x, "light": _light,
               "remap": _remap, "quantize": _quantize, "bricks": _brick, "planks": _planks,
               "ore": _ore}.items():
    setattr(Tex, _n, _f)


# ---- inspection --------------------------------------------------------
def render_grid_zoom(t: Tex, scale=None, labels=True):
    """Zoom with a pixel grid and x/y coordinates, so I can say exactly 'change (7,11)'."""
    scale = scale or max(8, 288 // t.w)
    m = 16 if labels else 0
    im = Image.new("RGBA", (t.w * scale + m, t.h * scale + m), (38, 38, 44, 255))
    bg = _checker(t.w * scale, t.h * scale, scale // 2 or 1)
    bg.alpha_composite(_up(t.image(), scale))
    im.alpha_composite(bg, (m, m))
    d = ImageDraw.Draw(im)
    f = _font()
    for i in range(t.w + 1):
        d.line([(m + i * scale, m), (m + i * scale, m + t.h * scale)], fill=(0, 0, 0, 70))
    for j in range(t.h + 1):
        d.line([(m, m + j * scale), (m + t.w * scale, m + j * scale)], fill=(0, 0, 0, 70))
    if labels:
        for i in range(t.w):
            d.text((m + i * scale + 2, 3), str(i), fill=(200, 200, 210, 255), font=f)
        for j in range(t.h):
            d.text((1, m + j * scale + scale // 2 - 4), str(j), fill=(200, 200, 210, 255), font=f)
    return im


def render_squint(t: Tex, scale=8):
    """Greyscale + tiny blur: shows value structure. If it's mush here, it's mush in-game."""
    g = t.image().convert("L").convert("RGBA")
    g = _up(g, scale).filter(__import__("PIL.ImageFilter", fromlist=["x"]).GaussianBlur(scale * 0.9))
    return g


def render_ingame(t: Tex, n=3):
    """Actual-size 1x and 2x tiles next to each other, like seeing a wall of the block from afar."""
    big = Image.new("RGBA", (t.w * n, t.h * n))
    for j in range(n):
        for i in range(n):
            big.paste(t.image(), (i * t.w, j * t.h))
    return _up(big, 2)


def inspect(t: Tex, path, name=None):
    """Detail sheet: gridded zoom with coordinates | squint test | in-game wall. Use to hunt single pixels."""
    gz = render_grid_zoom(t)
    sq = render_squint(t, scale=max(2, 144 // t.w))
    ig = render_ingame(t)
    pad = 12
    W = gz.width + sq.width + ig.width + pad * 4
    H = max(gz.height, sq.height, ig.height) + 40
    sheet = Image.new("RGBA", (W, H), (38, 38, 44, 255))
    d = ImageDraw.Draw(sheet)
    x = pad
    for im, lab in ((gz, "grid (x across, y down)"), (sq, "squint test"), (ig, "in-game wall 3x3")):
        d.text((x, 6), lab, fill=(200, 200, 210, 255))
        sheet.alpha_composite(im, (x, 22))
        x += im.width + pad
    sheet.convert("RGB").save(path)


def diff(a: Tex, b: Tex, path, scale=12):
    """Before/after with changed pixels outlined in magenta."""
    ch = np.any(a.px != b.px, axis=2)
    ims = []
    for t in (a, b):
        im = _checker(t.w * scale, t.h * scale)
        im.alpha_composite(_up(t.image(), scale))
        ims.append(im)
    d = ImageDraw.Draw(ims[1])
    for y, x in zip(*np.nonzero(ch)):
        d.rectangle([x * scale, y * scale, x * scale + scale - 1, y * scale + scale - 1], outline=(255, 0, 255, 255))
    sheet = Image.new("RGBA", (a.w * scale * 2 + 36, a.h * scale + 30), (38, 38, 44, 255))
    sheet.alpha_composite(ims[0], (12, 22))
    sheet.alpha_composite(ims[1], (24 + a.w * scale, 22))
    ImageDraw.Draw(sheet).text((12, 6), f"before / after  ({int(ch.sum())} px changed)", fill=(220, 220, 230, 255))
    sheet.convert("RGB").save(path)
    return int(ch.sum())


# ---- animation ---------------------------------------------------------
class Anim:
    """List of same-size Tex frames -> vertical strip + .png.mcmeta (resource-pack format)."""

    def __init__(self, frames, frametime=3, interpolate=False, order=None):
        self.frames = frames
        self.frametime = frametime
        self.interpolate = interpolate
        self.order = order  # e.g. [0,1,2,1] to ping-pong without duplicating frames

    def strip(self):
        w, h = self.frames[0].w, self.frames[0].h
        im = Image.new("RGBA", (w, h * len(self.frames)))
        for i, f in enumerate(self.frames):
            im.paste(f.image(), (0, i * h))
        return im

    def save(self, path):
        os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
        self.strip().save(path)
        anim = {"frametime": self.frametime}
        if self.interpolate:
            anim["interpolate"] = True
        if self.order:
            anim["frames"] = self.order
        with open(path + ".mcmeta", "w") as fh:
            json.dump({"animation": anim}, fh, indent=2)

    def preview(self, path, scale=12):
        """Filmstrip PNG (for me to look at) + looping GIF (for you)."""
        order = self.order or list(range(len(self.frames)))
        w, h = self.frames[0].w * scale, self.frames[0].h * scale
        sheet = Image.new("RGBA", (len(self.frames) * (w + 8) + 8, h + 28), (38, 38, 44, 255))
        d = ImageDraw.Draw(sheet)
        for i, f in enumerate(self.frames):
            bg = _checker(w, h)
            bg.alpha_composite(_up(f.image(), scale))
            sheet.alpha_composite(bg, (8 + i * (w + 8), 22))
            d.text((8 + i * (w + 8), 6), f"frame {i}", fill=(220, 220, 230, 255))
        sheet.convert("RGB").save(path)
        gif = [_up(self.frames[i].image(), scale).convert("RGB") for i in order]
        gif[0].save(os.path.splitext(path)[0] + ".gif", save_all=True, append_images=gif[1:],
                    duration=self.frametime * 50, loop=0)

    def flicker_report(self):
        """Frame-to-frame % pixels changed. Good flicker ~10-35%; <5% invisible, >50% strobing."""
        out = []
        n = len(self.frames)
        for i in range(n):
            a, b = self.frames[i].px, self.frames[(i + 1) % n].px
            out.append(float(np.any(a != b, axis=2).mean()))
        return out
