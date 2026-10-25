#!/usr/bin/env python3
"""Generate SalahMate launcher icons and brand emblem (original artwork, drawn from scratch).

Design: an eight-pointed geometric star (two overlapped squares) enclosing a mihrab arch,
with a crescent moon in the arch's spandrel. Palette: deep emerald + antique gold.
Run: python3 tools/generate_icons.py
"""
import math, os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res")

EMERALD_DARK = (10, 61, 50)
EMERALD = (14, 92, 74)
EMERALD_LIGHT = (23, 124, 100)
GOLD = (201, 162, 39)
GOLD_LIGHT = (231, 199, 106)
CREAM = (247, 243, 231)

SS = 4  # supersample factor


def draw_emblem(size, bg=True, pad_ratio=0.0, transparent=False):
    S = size * SS
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if bg and not transparent:
        # vertical gradient
        for y in range(S):
            t = y / max(1, S - 1)
            r = int(EMERALD_DARK[0] + (EMERALD_LIGHT[0] - EMERALD_DARK[0]) * t)
            g = int(EMERALD_DARK[1] + (EMERALD_LIGHT[1] - EMERALD_DARK[1]) * t)
            b = int(EMERALD_DARK[2] + (EMERALD_LIGHT[2] - EMERALD_DARK[2]) * t)
            d.line([(0, y), (S, y)], fill=(r, g, b, 255))

    pad = S * pad_ratio
    c = S / 2
    R = (S / 2) - pad          # outer star radius
    gold_w = max(2, int(S * 0.026))

    def bg_at(y):
        t = y / max(1, S - 1)
        t = min(1.0, max(0.0, t))
        return (int(EMERALD_DARK[0] + (EMERALD_LIGHT[0] - EMERALD_DARK[0]) * t),
                int(EMERALD_DARK[1] + (EMERALD_LIGHT[1] - EMERALD_DARK[1]) * t),
                int(EMERALD_DARK[2] + (EMERALD_LIGHT[2] - EMERALD_DARK[2]) * t), 255)

    # --- eight point star: two squares rotated 45 deg, drawn as outlines ---
    def square(rot, radius, width, color):
        pts = []
        for k in range(4):
            a = math.radians(rot + 90 * k)
            pts.append((c + radius * math.cos(a), c + radius * math.sin(a)))
        d.line(pts + [pts[0]], fill=color, width=width, joint="curve")

    square(0, R * 0.92, gold_w, GOLD)
    square(45, R * 0.92, gold_w, GOLD)

    # --- mihrab arch (pointed) inside ---
    aw = R * 0.50          # half width of arch
    base_y = c + R * 0.44  # arch base
    spring_y = c - R * 0.02
    apex_y = c - R * 0.50
    arch_w = max(2, int(S * 0.026))

    left = (c - aw, base_y)
    right = (c + aw, base_y)
    # shoulder points then pointed apex via quadratic sampling
    steps = 60
    left_curve = []
    for i in range(steps + 1):
        t = i / steps
        # quadratic bezier from (c-aw, spring_y) to apex with control (c-aw, apex_y)
        x = (1 - t) ** 2 * (c - aw) + 2 * (1 - t) * t * (c - aw * 0.55) + t ** 2 * c
        y = (1 - t) ** 2 * spring_y + 2 * (1 - t) * t * apex_y + t ** 2 * apex_y
        left_curve.append((x, y))
    right_curve = [(2 * c - x, y) for (x, y) in reversed(left_curve)]
    d.line([left] + left_curve + right_curve + [right], fill=GOLD_LIGHT, width=arch_w, joint="curve")
    # arch base line
    d.line([left, right], fill=GOLD_LIGHT, width=arch_w)

    # --- crescent inside the arch ---
    cr = R * 0.155
    ccx, ccy = c - R * 0.02, c + R * 0.10
    d.ellipse([ccx - cr, ccy - cr, ccx + cr, ccy + cr], fill=CREAM)
    off = cr * 0.46
    d.ellipse([ccx - cr + off, ccy - cr - off * 0.18,
               ccx + cr + off, ccy + cr - off * 0.18], fill=bg_at(ccy))

    return img.resize((size, size), Image.LANCZOS)


def rounded_mask(size, radius_ratio=0.22):
    m = Image.new("L", (size * SS, size * SS), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size * SS - 1, size * SS - 1],
                                        radius=int(size * SS * radius_ratio), fill=255)
    return m.resize((size, size), Image.LANCZOS)


def circle_mask(size):
    m = Image.new("L", (size * SS, size * SS), 0)
    ImageDraw.Draw(m).ellipse([0, 0, size * SS - 1, size * SS - 1], fill=255)
    return m.resize((size, size), Image.LANCZOS)


def write(path, img, mask=None):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = img.copy()
    if mask is not None:
        out.putalpha(mask)
    out.save(path)


DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for dens, px in DENSITIES.items():
    base = draw_emblem(px)
    write(f"{RES}/mipmap-{dens}/ic_launcher.png", base, rounded_mask(px))
    write(f"{RES}/mipmap-{dens}/ic_launcher_round.png", base, circle_mask(px))
    # adaptive foreground (transparent, extra safe-zone padding), 108dp canvas
    fg_px = int(px * 108 / 48)
    fg = draw_emblem(fg_px, bg=False, pad_ratio=0.255, transparent=True)
    write(f"{RES}/mipmap-{dens}/ic_launcher_foreground.png", fg)
    # adaptive background
    bg = draw_emblem(fg_px)
    write(f"{RES}/mipmap-{dens}/ic_launcher_background.png", bg)

# Play-store / docs artwork
write(f"{ROOT}/docs/brand/icon-512.png", draw_emblem(512), rounded_mask(512))
write(f"{ROOT}/docs/brand/emblem-1024.png", draw_emblem(1024, transparent=True), None)
print("icons generated")
