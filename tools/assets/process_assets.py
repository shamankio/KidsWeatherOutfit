#!/usr/bin/env python3
"""Post-process raw AI illustrations into app assets (stage 4, Prompt A.1).

    characters   cut out the 24 raw characters -> outfit_{set}_{gender}.webp (1600 px tall)
    backgrounds  cover-crop the 8 raw backgrounds -> bg_{weather}.webp (1080x2400)

Run from the repo root with the venv in tools/assets/.venv:

    tools/assets/.venv/bin/python tools/assets/process_assets.py characters [--thumbs]
    tools/assets/.venv/bin/python tools/assets/process_assets.py backgrounds
"""
import argparse
import csv
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res/drawable-nodpi"

SETS = ["heat", "warm", "warm_rain", "cool", "cool_rain", "wind", "cold", "cold_rain",
        "winter_light", "frost_hard", "snowfall", "rain_wind"]
GENDERS = ["boy", "girl"]
WEATHERS = ["sunny", "cloudy", "rain", "storm", "snow", "frost", "wind", "evening"]

PADDING_PX = 24
TARGET_HEIGHT = 1600
ALPHA_FLOOR = 24  # alpha at or below this is zeroed: kills the faint contact shadow
BG_MIN_CHANNEL = 205  # near-white background incl. the faint grey contact shadow
BG_MAX_CHROMA = 14    # ...but only unsaturated pixels, so pink/cream/blonde never count as background
POCKET_MIN_CHANNEL = 250  # enclosed pockets of the white backdrop: pure white...
POCKET_MAX_CHROMA = 5
POCKET_MIN_AREA_PX = 300  # ...and big enough not to be a highlight on the figure
ERODE_PX = 7         # restored pixels stay this far from the figure edge (MinFilter size, odd)
THUMB_SIZE = 512
BG_SIZE = (1080, 2400)


def read_mapping(path: Path, expected: set, key_cols: tuple) -> list:
    if not path.exists():
        sys.exit(f"missing mapping file: {path}")
    with path.open(newline="") as f:
        rows = list(csv.DictReader(f))
    found = {tuple(r[c] for c in key_cols) for r in rows}
    missing = expected - found
    extra = found - expected
    if missing:
        sys.exit(f"{path.name}: missing combinations: {sorted(missing)}")
    if extra:
        sys.exit(f"{path.name}: unexpected combinations: {sorted(extra)}")
    return rows


def make_session():
    """CPU only: on Apple Silicon onnxruntime picks the CoreML provider, which can hang for minutes
    compiling the model for the Neural Engine (stuck in CoreMLExecution loadModel)."""
    from rembg import new_session

    return new_session("isnet-general-use", providers=["CPUExecutionProvider"])


def restore_enclosed_whites(rgb: np.ndarray, alpha: np.ndarray) -> np.ndarray:
    """Background is pure white, so anything that is NOT near-white background connected to the frame
    border is part of the figure. rembg reads white/pink panels (an umbrella canopy, a white beanie) as
    background and punches holes in them; this restores those pixels. Returns a boolean mask of pixels
    to add. It is eroded so figure edges keep rembg's soft alpha instead of a hard fringe."""
    from scipy import ndimage

    mx, mn = rgb.max(axis=2).astype(int), rgb.min(axis=2).astype(int)
    near_white = (mn >= BG_MIN_CHANNEL) & (mx - mn <= BG_MAX_CHROMA)
    labels, _ = ndimage.label(near_white)
    border = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    background = np.isin(labels, border[border != 0])

    # Enclosed background pockets (between an arm and the head, under a hat brim...) are not connected
    # to the border, but they are large and pure white, unlike shaded panels on the figure itself.
    pure = (mn >= POCKET_MIN_CHANNEL) & (mx - mn <= POCKET_MAX_CHROMA)
    pocket_labels, count = ndimage.label(pure)
    if count:
        areas = np.bincount(pocket_labels.ravel())
        big = np.flatnonzero(areas >= POCKET_MIN_AREA_PX)
        background |= np.isin(pocket_labels, big[big != 0])

    figure = Image.fromarray((~background).astype(np.uint8) * 255).filter(ImageFilter.MinFilter(ERODE_PX))
    return (np.array(figure) > 0) & (alpha < 255)


def cut_out(img: Image.Image, session) -> Image.Image:
    from rembg import remove

    rgb_img = img.convert("RGB")
    out = np.array(
        remove(
            rgb_img,
            session=session,
            alpha_matting=True,
            alpha_matting_foreground_threshold=240,
            alpha_matting_background_threshold=10,
            alpha_matting_erode_size=10,
        ).convert("RGBA")
    )
    raw = np.array(rgb_img)
    restore = restore_enclosed_whites(raw, out[..., 3])
    out[restore, :3] = raw[restore]
    out[restore, 3] = 255
    out[out[..., 3] <= ALPHA_FLOOR, 3] = 0
    return Image.fromarray(out, "RGBA")


def trim_and_pad(img: Image.Image) -> Image.Image:
    bbox = img.getchannel("A").getbbox()
    if bbox is None:
        raise ValueError("image is fully transparent after background removal")
    img = img.crop(bbox)
    padded = Image.new("RGBA", (img.width + 2 * PADDING_PX, img.height + 2 * PADDING_PX), (0, 0, 0, 0))
    padded.paste(img, (PADDING_PX, PADDING_PX))
    return padded


def thumb(img: Image.Image) -> Image.Image:
    """Square crop of the UPPER part of the figure, centred horizontally, from the bbox top down."""
    side = min(img.width, img.height)
    left = (img.width - side) // 2
    return img.crop((left, 0, left + side, side)).resize((THUMB_SIZE, THUMB_SIZE), Image.LANCZOS)


def run_characters(args) -> None:
    expected = {(s, g) for s in SETS for g in GENDERS}
    rows = read_mapping(Path(args.mapping), expected, ("set", "gender"))
    src_dir = Path(args.src)
    out_dir = Path(args.out) / "characters"
    thumbs_dir = Path(args.out) / "thumbs"
    out_dir.mkdir(parents=True, exist_ok=True)
    if args.thumbs:
        thumbs_dir.mkdir(parents=True, exist_ok=True)

    absent = [r["source_filename"] for r in rows if not (src_dir / r["source_filename"]).exists()]
    if absent:
        sys.exit(f"source files listed in mapping but absent in {src_dir}: {absent}")

    session = make_session()
    summary, warnings = [], []
    for r in rows:
        src = src_dir / r["source_filename"]
        name = f"outfit_{r['set']}_{r['gender']}.webp"
        raw = Image.open(src)
        trimmed = trim_and_pad(cut_out(raw, session))
        if trimmed.height < TARGET_HEIGHT:
            warnings.append(f"{src.name}: trimmed height {trimmed.height}px < {TARGET_HEIGHT}px, not upscaled")
            final = trimmed
        else:
            w = round(trimmed.width * TARGET_HEIGHT / trimmed.height)
            final = trimmed.resize((w, TARGET_HEIGHT), Image.LANCZOS)
        dest = out_dir / name
        final.save(dest, "WEBP", quality=90, lossless=False, method=6)
        summary.append((name, f"{raw.width}x{raw.height}", f"{final.width}x{final.height}", dest.stat().st_size // 1024))
        if args.thumbs:
            thumb(final).save(thumbs_dir / f"outfit_{r['set']}_{r['gender']}_thumb.webp", "WEBP", quality=90)
        print(f"done {name}", flush=True)

    print_summary(summary, warnings)


def run_backgrounds(args) -> None:
    expected = {(w,) for w in WEATHERS}
    rows = read_mapping(Path(args.bg_mapping), expected, ("weather",))
    src_dir = Path(args.bg_src)
    out_dir = Path(args.out) / "backgrounds"
    out_dir.mkdir(parents=True, exist_ok=True)

    absent = [r["source_filename"] for r in rows if not (src_dir / r["source_filename"]).exists()]
    if absent:
        sys.exit(f"source files listed in mapping but absent in {src_dir}: {absent}")

    tw, th = BG_SIZE
    summary = []
    for r in rows:
        raw = Image.open(src_dir / r["source_filename"]).convert("RGB")
        scale = max(tw / raw.width, th / raw.height)
        resized = raw.resize((round(raw.width * scale), round(raw.height * scale)), Image.LANCZOS)
        left, top = (resized.width - tw) // 2, (resized.height - th) // 2
        final = resized.crop((left, top, left + tw, top + th))
        name = f"bg_{r['weather']}.webp"
        dest = out_dir / name
        final.save(dest, "WEBP", quality=85, method=6)
        summary.append((name, f"{raw.width}x{raw.height}", f"{tw}x{th}", dest.stat().st_size // 1024))
    print_summary(summary, [])


def print_summary(rows: list, warnings: list) -> None:
    print(f"\n{'output file':34}{'source':>12}{'final':>12}{'KB':>8}")
    for name, src, final, kb in rows:
        print(f"{name:34}{src:>12}{final:>12}{kb:>8}")
    for w in warnings:
        print(f"WARNING: {w}", file=sys.stderr)
    if warnings:
        print(f"\n{len(warnings)} file(s) need regenerating (too short)", file=sys.stderr)


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--out", default=str(ROOT / "tools/assets/out"))
    sub = p.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("characters")
    c.add_argument("--src", default=str(ROOT / "design/raw_characters"))
    c.add_argument("--mapping", default=str(ROOT / "tools/assets/mapping.csv"))
    c.add_argument("--thumbs", action="store_true", help="also write 512x512 upper-body thumbnails")
    c.set_defaults(fn=run_characters)

    b = sub.add_parser("backgrounds")
    b.add_argument("--bg-src", default=str(ROOT / "design/raw_backgrounds"))
    b.add_argument("--bg-mapping", default=str(ROOT / "tools/assets/bg_mapping.csv"))
    b.set_defaults(fn=run_backgrounds)

    args = p.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
