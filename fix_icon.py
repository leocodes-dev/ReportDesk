"""Re-encode reportdesk-icon.png as a clean, minimal Web PNG.

Output contains ONLY the essential chunks (IHDR, IDAT, IEND): no gAMA,
sRGB, pHYs, tEXt or any other auxiliary metadata. 8-bit RGBA keeps the
transparency a game icon needs.
"""

import os
import shutil
import struct
import sys
import zlib

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "icon", "reportdesk-icon.png")
SIDE = 512


def png_chunk(tag: bytes, data: bytes) -> bytes:
    return (
        struct.pack(">I", len(data))
        + tag
        + data
        + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    )


def make_square(image: Image.Image) -> Image.Image:
    w, h = image.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return image.crop((left, top, left + side, top + side))


def resample(image: Image.Image, size: int) -> Image.Image:
    # Two-stage LANCZOS: a gentle intermediate stop preserves detail when
    # the reduction ratio is large (e.g. 2048 -> 512).
    intermediate = max(size * 2, image.size[0] // 2)
    stage1 = image.resize((intermediate, intermediate), Image.LANCZOS)
    return stage1.resize((size, size), Image.LANCZOS)


def clean_png(image: Image.Image) -> bytes:
    """Encode an RGBA image as the minimal possible PNG.

    Each scanline is prefixed with filter byte 0 (None) and the whole
    pixel buffer is zlib-compressed into a single IDAT chunk.
    """
    w, h = image.size
    # Pillow's tobytes() for "RGBA" is tightly packed rows, already the
    # scanline data we need; just prepend the filter bytes.
    pixel_data = b"".join(
        b"\x00" + image.crop((0, y, w, y + 1)).tobytes() for y in range(h)
    )

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)  # 8-bit RGBA
    return (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", ihdr)
        + png_chunk(b"IDAT", zlib.compress(pixel_data, 9))
        + png_chunk(b"IEND", b"")
    )


def main() -> int:
    with Image.open(SRC) as raw:
        mode = raw.mode
        orig_size = raw.size
        print(f"source: {orig_size[0]}x{orig_size[1]} mode={mode}")

        if min(orig_size) > SIDE:
            final = resample(make_square(raw.convert("RGBA")), SIDE)
            print(f"down-sampled {orig_size[0]}x{orig_size[1]} -> {SIDE}x{SIDE}")
        else:
            final = raw.convert("RGBA").resize((SIDE, SIDE), Image.LANCZOS)
            print(f"resized {orig_size[0]}x{orig_size[1]} -> {SIDE}x{SIDE}")

    if final.size != (SIDE, SIDE):
        print(f"error: output is {final.size}, expected ({SIDE}, {SIDE})")
        return 1

    tmp = SRC + ".tmp"
    with open(tmp, "wb") as fh:
        fh.write(clean_png(final))
    shutil.move(tmp, SRC)

    size = os.path.getsize(SRC)
    print(
        f"wrote {SRC}: {SIDE}x{SIDE} 8-bit RGBA, "
        f"chunks: IHDR+IDAT+IEND only, {size} bytes ({size / 1024:.1f} KB)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
