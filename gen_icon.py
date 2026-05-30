"""
Generate NoteSpese launcher icons (adaptive + legacy WebP).
Run: python3 gen_icon.py
"""
from PIL import Image, ImageDraw, ImageFont
import os, math

BASE = 512  # render at 512×512, then scale

# Brand colors
BLUE_LIGHT  = (0x19, 0x76, 0xD2)  # #1976D2  (top of gradient)
BLUE_DARK   = (0x0D, 0x47, 0xA1)  # #0D47A1  (bottom of gradient)
BLUE_BRAND  = (0x15, 0x65, 0xC0)  # #1565C0  (euro sign)
WHITE       = (255, 255, 255, 255)
ENTRY_COLOR = (144, 202, 249, 200) # #90CAF9 with alpha (entry lines)
SHADOW      = (0, 20, 80, 60)

FONT_PATH = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"


def lerp_color(c1, c2, t):
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def draw_rounded_rect(draw, x1, y1, x2, y2, r, fill):
    draw.rounded_rectangle([x1, y1, x2, y2], radius=r, fill=fill)


def create_icon_rgba(size: int) -> Image.Image:
    s = size / BASE           # scale factor
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Gradient background (top → bottom) ──────────────────────────────
    for y in range(size):
        t = y / size
        col = lerp_color(BLUE_LIGHT, BLUE_DARK, t) + (255,)
        draw.line([(0, y), (size - 1, y)], fill=col)

    # Subtle circular highlight (top-left) for depth
    hl = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    hl_d = ImageDraw.Draw(hl)
    hl_r = int(220 * s)
    hl_d.ellipse(
        [-hl_r // 2, -hl_r // 2, hl_r + hl_r // 4, hl_r + hl_r // 4],
        fill=(255, 255, 255, 22),
    )
    img = Image.alpha_composite(img, hl)
    draw = ImageDraw.Draw(img)

    # ── Card (white rounded rectangle) ──────────────────────────────────
    cx, cy = size // 2, size // 2
    cw, ch = int(280 * s), int(340 * s)
    card_l = cx - cw // 2
    card_t = int(85 * s)
    card_r = card_l + cw
    card_b = card_t + ch
    cr = int(28 * s)           # corner radius

    # Soft shadow (multiple passes)
    for i in range(5, 0, -1):
        sh_alpha = 14 * i
        sh = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        sh_d = ImageDraw.Draw(sh)
        sh_d.rounded_rectangle(
            [card_l + i, card_t + i * 2, card_r + i, card_b + i * 2],
            radius=cr + i,
            fill=(0, 20, 80, sh_alpha),
        )
        img = Image.alpha_composite(img, sh)
    draw = ImageDraw.Draw(img)

    draw_rounded_rect(draw, card_l, card_t, card_r, card_b, cr, WHITE)

    # ── Euro sign "€" ────────────────────────────────────────────────────
    # Draw the euro manually: arc (C-shape) + two horizontal bars
    euro_cx = cx
    euro_cy = card_t + int(150 * s)
    euro_r  = int(78 * s)
    stroke  = int(18 * s)
    blue    = BLUE_BRAND + (255,)

    # Arc bounding box
    arc_box = [
        euro_cx - euro_r, euro_cy - euro_r,
        euro_cx + euro_r, euro_cy + euro_r,
    ]
    # Draw thick arc: stacked thin arcs for width
    for delta in range(-stroke // 2, stroke // 2 + 1):
        r_off = euro_r + delta
        bb = [euro_cx - r_off, euro_cy - r_off,
              euro_cx + r_off, euro_cy + r_off]
        draw.arc(bb, start=38, end=322, fill=blue, width=2)

    # Two horizontal bars
    bar_h   = int(stroke * 0.82)
    bar_x1  = euro_cx - euro_r + int(8 * s)
    bar_x2  = euro_cx + int(24 * s)
    bar_y1  = euro_cy - int(16 * s)
    bar_y2  = euro_cy + int(20 * s)

    def bar(draw, x1, y, x2, h):
        draw.rounded_rectangle([x1, y, x2, y + h], radius=h // 2, fill=blue)

    bar(draw, bar_x1, bar_y1, bar_x2, bar_h)
    bar(draw, bar_x1, bar_y2, bar_x2, bar_h)

    # ── Three entry lines ─────────────────────────────────────────────────
    line_x1     = card_l + int(28 * s)
    line_widths = [int(220 * s), int(185 * s), int(148 * s)]
    line_h      = int(10 * s)
    line_y      = card_b - int(110 * s)
    line_gap    = int(24 * s)
    ec          = ENTRY_COLOR

    for i, lw in enumerate(line_widths):
        ly = line_y + i * line_gap
        draw.rounded_rectangle(
            [line_x1, ly, line_x1 + lw, ly + line_h],
            radius=line_h // 2,
            fill=ec,
        )

    # ── Small "euro coin" accent (top-right corner of card) ─────────────
    coin_cx = card_r - int(34 * s)
    coin_cy = card_t + int(34 * s)
    coin_r  = int(20 * s)
    draw.ellipse(
        [coin_cx - coin_r, coin_cy - coin_r,
         coin_cx + coin_r, coin_cy + coin_r],
        fill=(21, 101, 192, 40),   # subtle blue tint
    )

    return img


def save_icons():
    base_dir = "/home/user/NoteSpese/app/src/main/res"
    densities = {
        "mdpi":    48,
        "hdpi":    72,
        "xhdpi":   96,
        "xxhdpi":  144,
        "xxxhdpi": 192,
    }

    # Render once at BASE, then scale
    print(f"Rendering base icon at {BASE}×{BASE}…")
    base_img = create_icon_rgba(BASE)
    base_rgb = base_img.convert("RGB")

    for density, px in densities.items():
        folder = os.path.join(base_dir, f"mipmap-{density}")
        icon = base_rgb.resize((px, px), Image.LANCZOS)
        for name in ("ic_launcher", "ic_launcher_round"):
            path = os.path.join(folder, f"{name}.webp")
            icon.save(path, "WEBP", quality=90)
        print(f"  {density}: {px}×{px} ✓")

    # Also save a full-size PNG preview (512×512)
    preview_path = "/home/user/NoteSpese/icon_preview.png"
    base_img.save(preview_path)
    print(f"\nPreview saved → {preview_path}")


if __name__ == "__main__":
    save_icons()
