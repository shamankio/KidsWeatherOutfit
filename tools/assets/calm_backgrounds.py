#!/usr/bin/env python3
"""Fix two background art problems by processing the existing renders (stage 4).

    storm  bg_storm: the cloud base is a ruler-straight warm-brown line above a flat pale sky.
           Dissolve it: blend with a vertically blurred copy under a noise-warped mask, plus a faint haze.
    rain   bg_rain: the bottom third is detailed grass and puddles (~20x the detail of the top third), so
           UI placed there fights the art. Progressively blur and desaturate toward the local mean colour;
           the top of the image is left bit-identical.

Both write a lossless PNG next to the raw render and print the measurement the fix is judged by.

    tools/assets/.venv/bin/python tools/assets/calm_backgrounds.py storm
    tools/assets/.venv/bin/python tools/assets/calm_backgrounds.py rain
"""
import argparse
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[2]
RAW = ROOT / "design/raw_backgrounds"

# storm
STORM_BLUR_SIGMA = 40.0     # vertical blur that melts the cloud base (horizontal detail is kept)
STORM_HALF_WINDOW = 150     # blend window half-height around the seam, px
STORM_WARP_PX = 40.0        # amplitude of the noise that makes the blend boundary irregular
STORM_HAZE_ALPHA = 0.28     # haze strength just under the seam
STORM_HAZE_FALL_PX = 250.0

# rain
RAIN_RAMP_START = 1700      # no change above this row (of 2752)
RAIN_MAX_SIGMA = 16.0       # blur at the bottom edge, px
RAIN_MEAN_SIGMA = 60.0      # "local mean colour" scale
RAIN_MEAN_PULL = 0.30       # how far the bottom edge is pulled toward the local mean


def smoothstep(x: np.ndarray) -> np.ndarray:
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3.0 - 2.0 * x)


def load(name: str) -> np.ndarray:
    return np.array(Image.open(RAW / name).convert("RGB")).astype(np.float64)


def save(img: np.ndarray, name: str) -> None:
    Image.fromarray(np.round(np.clip(img, 0, 255)).astype(np.uint8)).save(RAW / name, "PNG")
    print(f"wrote {RAW / name}")


def row_step(img: np.ndarray, y0: int, y1: int) -> float:
    """Largest change of the row-mean luminance between neighbouring rows inside [y0, y1)."""
    row = img.mean(axis=(1, 2))[y0:y1]
    return float(np.abs(np.diff(row)).max())


def detail(img: np.ndarray, y0: int, y1: int) -> float:
    """Mean vertical-gradient energy over rows y0..y1 (the metric used to compare the thirds)."""
    return float(np.abs(np.diff(img.mean(axis=2)[y0:y1], axis=0)).mean())


def low_freq_noise(width: int, scale: float, seed: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = ndimage.gaussian_filter1d(rng.standard_normal(width), scale, mode="wrap")
    return n / np.abs(n).max()


def run_storm(args) -> None:
    img = load("bg_storm.webp")
    h, w, _ = img.shape
    lum_row = img.mean(axis=(1, 2))
    lo, hi = int(h * 0.55), int(h * 0.85)  # the seam is in the lower middle; ignore clouds/hills elsewhere
    seam = lo + int(np.argmax(np.abs(np.diff(lum_row[lo:hi]))))
    win = (seam - STORM_HALF_WINDOW - 60, seam + STORM_HALF_WINDOW + 60)
    before = row_step(img, *win)
    print(f"seam at y = {seam}; largest row step in the window: {before:.2f}")

    blurred = ndimage.gaussian_filter1d(img, STORM_BLUR_SIGMA, axis=0, mode="nearest")
    yy = np.arange(h, dtype=np.float64)[:, None]
    warp = (low_freq_noise(w, 90.0, seed=7) * STORM_WARP_PX)[None, :]
    d = np.abs(yy - seam + warp)                       # distance from the (warped) seam
    # 1 within 30 px of the seam, fading to 0 at STORM_HALF_WINDOW
    weight = 1.0 - smoothstep((d - 30.0) / (STORM_HALF_WINDOW - 30.0))
    out = img * (1.0 - weight[..., None]) + blurred * weight[..., None]

    # haze: the cloud-underside tone, strongest just below the seam, fading into the sky
    under = img[seam - 30 : seam - 6].mean(axis=(0, 1))
    below = np.clip((yy - seam + warp) / STORM_HAZE_FALL_PX, 0.0, 1.0)
    onset = smoothstep((yy + warp - (seam - 120.0)) / 110.0)   # fade the haze in; a hard start leaves a visible line
    haze = STORM_HAZE_ALPHA * (1.0 - smoothstep(below)) * onset
    out = out * (1.0 - haze[..., None]) + under * haze[..., None]

    after = row_step(out, *win)
    print(f"largest row step after: {after:.2f}  (target < 1.5)  {'OK' if after < 1.5 else 'NOT MET'}")
    outside = np.abs(np.arange(h) - seam) > STORM_HALF_WINDOW + STORM_WARP_PX + STORM_HAZE_FALL_PX
    drift = float(np.abs(out[outside] - img[outside]).max())
    print(f"max change outside the window (hills, upper clouds): {drift:.2f}")
    save(out, "bg_storm_fixed.png")


def run_rain(args) -> None:
    img = load("bg_rain.webp")
    h, w, _ = img.shape
    third = h // 3
    print(f"detail (vertical gradient energy), top / middle / bottom third: "
          f"{detail(img, 0, third):.2f} / {detail(img, third, 2 * third):.2f} / {detail(img, 2 * third, h):.2f}")

    t = smoothstep((np.arange(h, dtype=np.float64) - RAIN_RAMP_START) / (h - RAIN_RAMP_START))  # 0..1 down the ramp
    sigmas = np.linspace(0.0, RAIN_MAX_SIGMA, 9)
    stack = [img] + [ndimage.gaussian_filter(img, (s, s, 0), mode="nearest") for s in sigmas[1:]]
    target = t * RAIN_MAX_SIGMA
    idx = np.clip(np.searchsorted(sigmas, target, side="right") - 1, 0, len(sigmas) - 2)
    frac = ((target - sigmas[idx]) / (sigmas[1] - sigmas[0]))[:, None, None]
    out = np.empty_like(img)
    for i in range(len(sigmas) - 1):                    # per-row interpolation between the two nearest blur levels
        rows = idx == i
        if rows.any():
            out[rows] = stack[i][rows] * (1.0 - frac[rows]) + stack[i + 1][rows] * frac[rows]

    mean = ndimage.gaussian_filter(img, (RAIN_MEAN_SIGMA, RAIN_MEAN_SIGMA, 0), mode="nearest")
    pull = (RAIN_MEAN_PULL * t)[:, None, None]
    out = out * (1.0 - pull) + mean * pull

    print(f"detail after, top / middle / bottom third: "
          f"{detail(out, 0, third):.2f} / {detail(out, third, 2 * third):.2f} / {detail(out, 2 * third, h):.2f}")
    bottom = detail(out, 2 * third, h)
    print(f"bottom third target <= 1.0: {'OK' if bottom <= 1.0 else 'NOT MET'}")
    same = float(np.abs(out[:RAIN_RAMP_START] - img[:RAIN_RAMP_START]).max())
    print(f"max change above row {RAIN_RAMP_START}: {same:.4f} (should be 0)")
    save(out, "bg_rain_fixed.png")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("storm").set_defaults(fn=run_storm)
    sub.add_parser("rain").set_defaults(fn=run_rain)
    args = p.parse_args()
    args.fn(args)


if __name__ == "__main__":
    main()
