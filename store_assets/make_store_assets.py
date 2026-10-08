"""Play Store graphics banata hai: icon-512.png aur feature-graphic-1024x500.png.
Chalane ke liye: pip install pillow && python make_store_assets.py
Colors/text neeche edit karke dobara chala sakte ho."""
import os
from PIL import Image, ImageDraw, ImageFont

out = os.path.dirname(os.path.abspath(__file__))
BG = (14, 26, 36)
ICE = (127, 216, 255)
EMBER = (255, 159, 67)
RED = (217, 93, 57)
DARK = (43, 47, 54)


def train(d, x, y, s):
    d.rectangle([x, y - 14 * s, x + 90 * s, y - 8 * s], fill=DARK)
    d.rounded_rectangle([x + 34 * s, y - 46 * s, x + 90 * s, y - 14 * s], radius=6 * s, fill=EMBER)
    d.rectangle([x, y - 58 * s, x + 36 * s, y - 14 * s], fill=RED)
    d.rectangle([x + 9 * s, y - 48 * s, x + 27 * s, y - 32 * s], fill=(207, 233, 255))
    d.rectangle([x + 70 * s, y - 60 * s, x + 80 * s, y - 46 * s], fill=DARK)
    for cx in (18, 48, 76):
        d.ellipse([x + (cx - 10) * s, y - 30 * s + 8 * s, x + (cx + 10) * s, y - 10 * s + 8 * s], fill=DARK)
    d.rectangle([x - 300 * s, y + 10 * s, x + 400 * s, y + 14 * s], fill=(255, 255, 255))


icon = Image.new("RGB", (512, 512), BG)
train(ImageDraw.Draw(icon), 110, 330, 3.4)
icon.save(os.path.join(out, "icon-512.png"))

banner = Image.new("RGB", (1024, 500), BG)
d = ImageDraw.Draw(banner)
train(d, 80, 380, 3.2)
d.rectangle([0, 396, 1024, 404], fill=(255, 255, 255))
try:
    big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 78)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 32)
except Exception:
    big = small = ImageFont.load_default()
d.text((420, 120), "Frost Express", font=big, fill=ICE)
d.text((424, 225), "The world froze. The train didn't.", font=small, fill=(234, 244, 250))
banner.save(os.path.join(out, "feature-graphic-1024x500.png"))
print("Done")
