#!/usr/bin/env python3
"""Generate the 64x64 pixel-art Keep My Sword 2.0 icon (nearest-neighbor upscaled to 512x512)."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
SCALE = 8


def hex_rgba(h: str, a: int = 255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def blend(dst, src):
    sr, sg, sb, sa = src
    if sa == 0:
        return dst
    if sa == 255:
        return src
    dr, dg, db, da = dst
    inv = 255 - sa
    out_a = sa + (da * inv) // 255
    if out_a == 0:
        return (0, 0, 0, 0)
    r = (sr * sa + dr * da * inv // 255) // out_a
    g = (sg * sa + dg * da * inv // 255) // out_a
    b = (sb * sa + db * da * inv // 255) // out_a
    return (r, g, b, out_a)


def write_png(path: Path, width: int, height: int, rgba_bytes: bytes):
    def chunk(tag: bytes, data: bytes) -> bytes:
        return (
            struct.pack("!I", len(data))
            + tag
            + data
            + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        )

    raw = bytearray()
    stride = width * 4
    for y in range(height):
        raw.append(0)
        raw.extend(rgba_bytes[y * stride : (y + 1) * stride])

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    png.extend(chunk(b"IHDR", struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)))
    png.extend(chunk(b"IDAT", zlib.compress(bytes(raw), 9)))
    png.extend(chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(bytes(png))


def inside_rounded_rect(x: int, y: int, x0: int, y0: int, x1: int, y1: int, r: int) -> bool:
    if x < x0 or x > x1 or y < y0 or y > y1:
        return False
    cx = x0 + r if x < x0 + r else (x1 - r if x > x1 - r else x)
    cy = y0 + r if y < y0 + r else (y1 - r if y > y1 - r else y)
    dx = x - cx
    dy = y - cy
    return dx * dx + dy * dy <= r * r


def draw_icon():
    canvas = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def put(x: int, y: int, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            canvas[y][x] = blend(canvas[y][x], color)

    def fill_rect(x0: int, y0: int, x1: int, y1: int, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                put(x, y, color)

    # 1. Warm forge-slate rounded badge with colored bronze-gold bevel border
    outer_border = hex_rgba("#3B282A")
    gold_top = hex_rgba("#C98E52")
    gold_mid = hex_rgba("#8C5B38")
    gold_bot = hex_rgba("#5A3A2B")
    bg_top = hex_rgba("#231E2D")
    bg_bot = hex_rgba("#382328")

    for y in range(SIZE):
        for x in range(SIZE):
            if not inside_rounded_rect(x, y, 2, 2, 61, 61, 11):
                continue
            if not inside_rounded_rect(x, y, 3, 3, 60, 60, 10):
                put(x, y, outer_border)
            elif not inside_rounded_rect(x, y, 5, 5, 58, 58, 8):
                # Bevelled warm bronze inner rim
                if y < 22 or x < 16:
                    put(x, y, gold_top if (x + y) % 5 != 0 else gold_mid)
                elif y > 44:
                    put(x, y, gold_bot)
                else:
                    put(x, y, gold_mid)
            else:
                t = (y - 5) / 53.0
                r = int(bg_top[0] * (1 - t) + bg_bot[0] * t)
                g = int(bg_top[1] * (1 - t) + bg_bot[1] * t)
                b = int(bg_top[2] * (1 - t) + bg_bot[2] * t)
                # Subtle warm ember glow behind sword crack & anvil
                dx = x - 33
                dy = y - 36
                dist2 = dx * dx + dy * dy
                if dist2 < 260:
                    glow = max(0.0, 1.0 - dist2 / 260.0)
                    r = min(255, int(r + 58 * glow))
                    g = min(255, int(g + 28 * glow))
                    b = min(255, int(b + 10 * glow))
                put(x, y, (r, g, b, 255))

    # Subtle stone floor shadow under anvil
    for x in range(16, 52):
        put(x, 53, hex_rgba("#1B141D", 200))
        put(x, 54, hex_rgba("#1B141D", 140))

    # 2. Sturdy Minecraft-inspired blacksmith anvil (lower-right / center)
    anvil_outline = hex_rgba("#1F2330")
    anvil_dark = hex_rgba("#383E4F")
    anvil_mid = hex_rgba("#545D73")
    anvil_light = hex_rgba("#7E89A3")
    anvil_hi = hex_rgba("#AAB6CC")
    anvil_steel_edge = hex_rgba("#8D99B2")

    def draw_anvil_row(y: int, x0: int, x1: int, fill_c, hi_left: int = 0, cool_right: int = 0):
        put(x0 - 1, y, anvil_outline)
        put(x1 + 1, y, anvil_outline)
        for x in range(x0, x1 + 1):
            c = fill_c
            if hi_left and x < x0 + hi_left:
                c = anvil_light
            elif cool_right and x > x1 - cool_right:
                c = anvil_steel_edge if fill_c == anvil_hi else anvil_mid
            put(x, y, c)

    # Top outline of anvil face
    for x in range(21, 51):
        put(x, 34, anvil_outline)
    # Anvil top face (bright cool steel)
    draw_anvil_row(35, 20, 50, anvil_hi, cool_right=3)
    draw_anvil_row(36, 19, 50, anvil_light, cool_right=3)
    draw_anvil_row(37, 20, 49, anvil_mid, hi_left=4)
    draw_anvil_row(38, 22, 48, anvil_mid, hi_left=3)
    draw_anvil_row(39, 25, 46, anvil_dark, hi_left=2)
    # Narrow waist
    for y in range(40, 46):
        draw_anvil_row(y, 29, 42, anvil_dark, hi_left=3)
        put(33, y, anvil_mid)
        put(34, y, anvil_mid)
    # Flared base
    draw_anvil_row(46, 26, 45, anvil_mid, hi_left=3)
    draw_anvil_row(47, 24, 47, anvil_mid, hi_left=4)
    draw_anvil_row(48, 22, 49, anvil_light, hi_left=4)
    draw_anvil_row(49, 21, 50, anvil_mid, hi_left=3)
    draw_anvil_row(50, 21, 50, anvil_dark, hi_left=2)
    draw_anvil_row(51, 21, 50, anvil_dark)
    for x in range(21, 51):
        put(x, 52, anvil_outline)

    # 3. Resilient cracked diamond sword leaning diagonally (top-right tip to bottom-left hilt)
    # Parametric diagonal along u = (x - y), v = (x + y)
    # Tip near (48, 12), Hilt near (14, 46) -> x + y around 60
    blade_outline = hex_rgba("#12394A")
    blade_dark = hex_rgba("#1E7A8C")
    blade_mid = hex_rgba("#38C4C4")
    blade_light = hex_rgba("#7CECE8")
    blade_hi = hex_rgba("#D6FFFA")
    crack_glow = hex_rgba("#FF9E2C")
    crack_core = hex_rgba("#FFF5B8")

    # Draw sleek tapered blade from step s=0 (near crossguard at (24,36)) to s=25 (sharp tip at (49,11))
    for s in range(0, 26):
        cx = 24 + s
        cy = 36 - s
        half_w = 2 if s < 19 else (1 if s < 23 else 0)
        for d in range(-half_w - 1, half_w + 2):
            px = cx + d
            py = cy + d
            for ox, oy in ((0, 0), (1, 0)):
                x, y = px + ox, py + oy
                if abs(d) == half_w + 1:
                    put(x, y, blade_outline)
                elif half_w == 0:
                    put(x, y, blade_hi)
                elif d == -half_w:
                    put(x, y, blade_hi if s > 2 else blade_light)
                elif d == 0:
                    # Crisp central spine highlight running to the tip
                    put(x, y, blade_hi if s >= 19 else blade_light)
                elif d < 0:
                    put(x, y, blade_mid)
                else:
                    put(x, y, blade_dark)
    # Sharp diagonal sword point at (50, 10)
    put(49, 10, blade_outline)
    put(50, 9, blade_outline)
    put(51, 10, blade_outline)
    put(51, 11, blade_outline)
    put(50, 10, blade_hi)
    put(50, 11, blade_light)

    # Glowing kintsugi (golden weld) fracture line across the middle of the blade
    crack_pixels = [
        (33, 24, crack_glow),
        (34, 24, crack_core),
        (34, 25, crack_glow),
        (35, 25, crack_core),
        (35, 26, crack_core),
        (36, 26, crack_glow),
        (36, 27, crack_core),
        (37, 27, crack_core),
        (38, 28, crack_glow),
        (38, 27, hex_rgba("#2B161A", 200)),
        (35, 27, hex_rgba("#2B161A", 210)),
        (36, 28, hex_rgba("#2B161A", 210)),
        (37, 28, hex_rgba("#2B161A", 210)),
    ]
    for x, y, c in crack_pixels:
        put(x, y, c)

    # Warm golden-bronze crossguard centered at (23, 37), solid diagonal fill
    guard_outline = hex_rgba("#3D2314")
    guard_dark = hex_rgba("#9E6424")
    guard_mid = hex_rgba("#D99B38")
    guard_hi = hex_rgba("#FFE285")

    for t in range(-6, 7):
        gx = 23 + t
        gy = 37 + t
        for w in (-2, -1, 0, 1, 2):
            px = gx + w
            py = gy - w
            for ox, oy in ((0, 0), (1, 0)):
                x, y = px + ox, py + oy
                if abs(t) == 6 or abs(w) == 2:
                    put(x, y, guard_outline)
                elif w == -1:
                    put(x, y, guard_hi)
                elif w == 0:
                    put(x, y, guard_mid)
                else:
                    put(x, y, guard_dark)
    # Center gem on crossguard
    fill_rect(22, 36, 24, 38, hex_rgba("#29A8B8"))
    put(22, 36, hex_rgba("#B0FFF9"))

    # Leather grip from (21, 39) down to (15, 45), solid diagonal fill
    grip_outline = hex_rgba("#2B1712")
    grip_dark = hex_rgba("#593020")
    grip_mid = hex_rgba("#874C32")
    grip_hi = hex_rgba("#B06D4A")
    for s in range(1, 8):
        cx = 22 - s
        cy = 38 + s
        for d in (-2, -1, 0, 1, 2):
            px = cx + d
            py = cy + d
            for ox, oy in ((0, 0), (1, 0)):
                x, y = px + ox, py + oy
                if abs(d) == 2:
                    put(x, y, grip_outline)
                elif d == -1:
                    put(x, y, grip_hi if s % 2 == 0 else grip_mid)
                elif d == 0:
                    put(x, y, grip_mid if s % 2 == 0 else grip_dark)
                else:
                    put(x, y, grip_dark)

    # Golden pommel at (13, 47)
    for dy in range(-3, 4):
        for dx in range(-3, 4):
            dist = abs(dx) + abs(dy)
            if dist == 4:
                put(13 + dx, 47 + dy, guard_outline)
            elif dist <= 3:
                if dx + dy < 0:
                    put(13 + dx, 47 + dy, guard_hi)
                elif dx + dy == 0:
                    put(13 + dx, 47 + dy, guard_mid)
                else:
                    put(13 + dx, 47 + dy, guard_dark)

    # 4. Warm forge sparks floating near the crack & anvil
    sparks = [
        (31, 20, hex_rgba("#FFE58F")),
        (32, 20, hex_rgba("#FF9F38", 180)),
        (41, 22, hex_rgba("#FFD166")),
        (42, 31, hex_rgba("#FF8C42")),
        (28, 28, hex_rgba("#FFF3B0", 210)),
        (46, 28, hex_rgba("#FFAD42", 180)),
        (18, 22, hex_rgba("#FFE08A", 140)),
    ]
    for x, y, c in sparks:
        put(x, y, c)

    # Nearest-neighbor upscale 64x64 -> 512x512
    out_w = SIZE * SCALE
    out_h = SIZE * SCALE
    buf = bytearray(out_w * out_h * 4)
    for y in range(SIZE):
        row = canvas[y]
        for x in range(SIZE):
            r, g, b, a = row[x]
            for sy in range(SCALE):
                base = ((y * SCALE + sy) * out_w + x * SCALE) * 4
                for sx in range(SCALE):
                    idx = base + sx * 4
                    buf[idx] = r
                    buf[idx + 1] = g
                    buf[idx + 2] = b
                    buf[idx + 3] = a

    data = bytes(buf)
    write_png(ROOT / "common/src/main/resources/icon.png", out_w, out_h, data)
    write_png(ROOT / "docs/artwork/icon.png", out_w, out_h, data)
    print("Generated 512x512 icons from 64x64 pixel art.")


if __name__ == "__main__":
    draw_icon()
