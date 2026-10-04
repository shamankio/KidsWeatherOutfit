#!/usr/bin/env python3
"""Remove the translucent rounded "card" baked into the bottom third of bg_sunny.

The card is a flat white overlay: observed = orig * (1 - a) + 255 * a, with a ~= 0.425, equal in R, G and B,
a straight top edge near y = 1980 with rounded corners, full width, running to the bottom. It is not blurred
(texture inside, rescaled by 1 / (1 - a), matches the texture outside), so the original is recovered exactly:

    orig = (observed - 255 * a) / (1 - a)

The overlay strength, edge height and corner radius are fitted from the image rather than guessed: `a` by
least squares across the edge, then the edge y and radius by minimising the luminance jump across the seam
after correction.

    tools/assets/.venv/bin/python tools/assets/remove_baked_card.py
"""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage
from scipy.special import erf

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "design/raw_backgrounds/bg_sunny.webp"
DST = ROOT / "design/raw_backgrounds/bg_sunny_fixed.png"

EDGE_GUESS_Y = 1980
ALPHA_GUESS = 0.425


def signed_distance(yy: np.ndarray, xx: np.ndarray, w: int, top: float, radius: float, overhang: float) -> np.ndarray:
    """Approximate signed distance (px, >0 outside the card) to a rect with circular top corners.

    The card is slightly wider than the image (`overhang` px per side), so the corner arc is cut off before
    it reaches its vertical tangent at the border. Ignoring that leaves a bright wedge in the outer pixels.
    """
    dy = top + radius - yy                       # >0 above the straight part of the corner arcs
    dx = radius - np.minimum(xx + overhang, w - xx + overhang)   # >0 within `radius` of a side
    corner = (dy > 0) & (dx > 0)
    return np.where(corner, np.hypot(dx, dy) - radius, top - yy)


def coverage(
    h: int, w: int, top: float, radius: float, overhang: float, sigma: float, rows: tuple = None
) -> np.ndarray:
    """Fraction (0..1) of each pixel covered by the card: a full-width rect with rounded top corners.

    The edge is softened with a gaussian of width `sigma`; the source edge is spread over ~3 rows, so a
    hard 1 px anti-aliased edge leaves a dark/bright line. `rows=(y0, y1)` computes only that row window.
    """
    y0, y1 = rows if rows else (0, h)
    yy, xx = np.mgrid[y0:y1, 0:w].astype(np.float64) + 0.5
    dist = signed_distance(yy, xx, w, top, radius, overhang)
    return 0.5 * (1.0 - erf(dist / (sigma * np.sqrt(2.0))))


def fit_alpha(img: np.ndarray, top: int) -> float:
    """Least squares for a: just inside the card, obs ~ orig*(1-a) + 255a with orig ~ the row just outside."""
    above = img[top - 14 : top - 6, 120:-120].mean(axis=(0, 1))
    below = img[top + 6 : top + 14, 120:-120].mean(axis=(0, 1))
    return float(np.sum((below - above) * (255 - above)) / np.sum((255 - above) ** 2))


def fit_corner(img: np.ndarray) -> tuple:
    """Corner radius and overhang: least-squares circle through the card's observed top edge, both corners
    together, using every column from the image border inward.

    The seam metric ignores the corners, and total variation there is dominated by scene texture, so
    the edge itself (row of the largest vertical luminance step per column) is the usable signal.
    """
    lum = img.mean(axis=2)
    w = lum.shape[1]
    pts = []  # (distance from the nearest side, observed edge row)
    for x in range(0, 60):
        for col in (x, w - 1 - x):
            step = lum[EDGE_GUESS_Y - 34 : EDGE_GUESS_Y + 86, col] - lum[EDGE_GUESS_Y - 38 : EDGE_GUESS_Y + 82, col]
            pts.append((x + 0.5, EDGE_GUESS_Y - 36 + int(np.argmax(step))))
    d = np.array([p[0] for p in pts])
    ys = np.array([p[1] for p in pts], dtype=np.float64)

    def model(top: float, r: float, c: float) -> np.ndarray:
        u = np.clip(r - (d + c), 0.0, r)             # horizontal distance from the arc's centre column
        return np.where(d + c < r, top + r - np.sqrt(np.maximum(r * r - u * u, 0.0)), top)

    err, _, radius, overhang = min(
        (np.mean((model(t, r, c) - ys) ** 2), t, r, c)
        for t in np.arange(EDGE_GUESS_Y - 6, EDGE_GUESS_Y + 6, 0.5)
        for r in range(16, 70)
        for c in np.arange(0.0, 8.01, 0.5)
    )
    return float(radius), float(overhang), float(err**0.5)


def correct(
    img: np.ndarray, top: float, radius: float, overhang: float, alpha: float, sigma: float, rows: tuple = None
) -> np.ndarray:
    y0, y1 = rows if rows else (0, img.shape[0])
    a = (alpha * coverage(img.shape[0], img.shape[1], top, radius, overhang, sigma, rows))[..., None]
    return np.clip((img[y0:y1] - 255.0 * a) / (1.0 - a), 0.0, 255.0)


BAND_PX = 5.0  # half-width of the band along the old edge that is re-interpolated (codec ringing)
SEAM_ROWS = (EDGE_GUESS_Y - 24, EDGE_GUESS_Y + 26)  # strip of rows used to fit the edge


def seam_residual(strip: np.ndarray, top: float) -> float:
    """After correction the rows around the edge must continue smoothly. Column-averaged luminance is
    compared with a straight line through the rows 8..4 above and 4..8 below the edge; returns the rms
    deviation (luminance levels) over the 10 rows around the edge. Corners are excluded."""
    lum = strip[:, 150:-150, :].mean(axis=(1, 2))
    y = np.arange(SEAM_ROWS[0], SEAM_ROWS[1], dtype=np.float64)
    ref = (np.abs(y - top) >= 4) & (np.abs(y - top) <= 8)
    line = np.polyval(np.polyfit(y[ref], lum[ref], 1), y)
    near = np.abs(y - top) < 5
    return float(np.sqrt(np.mean((lum[near] - line[near]) ** 2)))


def smooth_band(fixed: np.ndarray, top: float, radius: float, overhang: float) -> np.ndarray:
    """Re-interpolate the thin band along the old card edge from the clean pixels on either side.

    The lossy source rings around the sharp card edge (over/undershoot of a few levels, and a much
    softer blue channel), which no single edge model inverts exactly. The grass there is a smooth
    gradient, so each band pixel is a linear blend of the corrected image sampled BAND_PX outside and
    BAND_PX inside, along the edge normal (so the rounded corners are handled too).
    """
    h, w, _ = fixed.shape
    y0, y1 = int(top - radius - 8), int(top + radius + 12)
    yy, xx = np.mgrid[y0:y1, 0:w].astype(np.float64) + 0.5
    dist = signed_distance(yy, xx, w, top, radius, overhang)  # >0 outside the card
    gy, gx = np.gradient(dist)
    norm = np.maximum(np.hypot(gx, gy), 1e-9)
    # image y grows downward, dist grows upward: "outside" is along +n where n = grad(dist) / |grad|
    ny, nx = gy / norm, gx / norm

    def sample(shift: np.ndarray) -> np.ndarray:  # shift = signed distance along n for every pixel
        coords = [yy - 0.5 - y0 + shift * ny, xx - 0.5 + shift * nx]
        return np.stack(
            [ndimage.map_coordinates(fixed[y0:y1, :, c], coords, order=1, mode="nearest") for c in range(3)], axis=-1
        )

    band = np.abs(dist) < BAND_PX
    outside = sample(BAND_PX - dist)    # the pixel BAND_PX outside the edge, on this normal
    inside = sample(-BAND_PX - dist)    # the pixel BAND_PX inside the edge
    t = np.clip((dist + BAND_PX) / (2 * BAND_PX), 0.0, 1.0)[..., None]
    blended = inside * (1.0 - t) + outside * t
    out = fixed.copy()
    region = out[y0:y1]
    region[band] = blended[band]
    return out


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--src", default=str(SRC))
    p.add_argument("--dst", default=str(DST))
    args = p.parse_args()

    img = np.array(Image.open(args.src).convert("RGB")).astype(np.float64)
    h, w, _ = img.shape
    print(f"{args.src}: {w}x{h}")

    alpha = fit_alpha(img, EDGE_GUESS_Y)
    print(f"fitted alpha = {alpha:.4f} (guess {ALPHA_GUESS})")

    radius, overhang, rms = fit_corner(img)
    print(f"corner radius = {radius:.0f} px, side overhang = {overhang:.1f} px (circle fit to the edge, rms {rms:.2f} px)")

    strip = img[SEAM_ROWS[0] : SEAM_ROWS[1]]
    before = seam_residual(strip, EDGE_GUESS_Y + 1.0)
    resid, top, sigma = min(
        (seam_residual(correct(img, t, radius, overhang, alpha, sg, SEAM_ROWS), t), t, sg)
        for t in np.arange(EDGE_GUESS_Y - 1, EDGE_GUESS_Y + 5, 0.1)
        for sg in np.arange(0.3, 2.01, 0.1)
    )
    print(f"edge y = {top:.1f}, edge softness sigma = {sigma:.1f} px")
    print(f"edge-row residual: {before:.1f} before -> {resid:.2f} after (target < 1.5 luminance levels)")

    fixed = smooth_band(correct(img, top, radius, overhang, alpha, sigma), top, radius, overhang)
    Image.fromarray(np.round(fixed).astype(np.uint8)).save(args.dst, "PNG")
    print(f"wrote {args.dst}")


if __name__ == "__main__":
    main()
