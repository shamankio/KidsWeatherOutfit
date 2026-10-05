# Asset processing

`process_assets.py` turns the raw AI illustrations into the app's drawables (stage 4, Prompt A.1).

| Input | Output |
|---|---|
| `design/raw_characters/outfit_{set}_{gender}.webp` (24, white backdrop, 1792x2400) | `outfit_{set}_{gender}.webp`: transparent, trimmed, 24 px padding, 1600 px tall, WebP q90 |
| `design/raw_backgrounds/*` (8) + `tools/assets/bg_mapping.csv` | `bg_{weather}.webp`: 1080x2400 cover crop, WebP q85 |

`bg_sunny` is fed from `design/raw_backgrounds/bg_sunny_fixed.png`, not the raw WebP: the AI render had a translucent
rounded "card" baked into its bottom third. `remove_baked_card.py` reverses it exactly (see below). `bg_mapping.csv` maps
each source file to its weather.

Output goes to `tools/assets/out/` (gitignored); copy the results into `app/src/main/res/drawable-nodpi/`.

## Setup (once)

```bash
python3 -m venv tools/assets/.venv
tools/assets/.venv/bin/pip install "rembg[cpu]" pillow numpy scipy onnxruntime
```

## Run

```bash
tools/assets/.venv/bin/python tools/assets/process_assets.py characters            # ~5 min for 24 files
tools/assets/.venv/bin/python tools/assets/process_assets.py characters --thumbs   # + 512x512 upper-body thumbs
tools/assets/.venv/bin/python tools/assets/process_assets.py backgrounds
tools/assets/.venv/bin/python tools/assets/process_assets.py thumbs                # dedicated thumbs, see below
```

It fails loudly if `mapping.csv` is missing one of the 24 set/gender combinations or a listed source file is absent,
and prints a warning for any figure shorter than 1600 px after trimming (never upscaled: regenerate those).

## Dedicated thumbnails (warm, cool)

The strip and `OutfitThumbnail` crop the full figure to a square as wide as the figure. For the narrow warm and cool
figures (516-674 px) that is little more than a face and a collar, so the two sets were hard to tell from each other.
`process_assets.py thumbs` cuts `outfit_{warm,cool}_{boy,girl}_thumb.webp` (512x512, transparent) from the finished
drawables with a 1050 px square (`WIDE_THUMBS`), which brings in the torso and, for cool, the tied jacket. Copy them into
`drawable-nodpi/`; `outfitThumbDrawables()` in `OutfitAssets.kt` is the only place that picks them. To add another set,
add it to `WIDE_THUMBS` and to the two `*ThumbDrawable` functions.

## Why the cut-out has extra steps

- **CPU only.** On Apple Silicon onnxruntime picks the CoreML provider, which hangs for minutes compiling the model
  (stuck in `CoreMLExecution loadModel`). `make_session()` pins `CPUExecutionProvider`.
- **`restore_enclosed_whites`.** rembg reads white or pale panels (umbrella canopies) as background and punches holes in
  them. Anything that is not near-white backdrop connected to the frame border is restored. Large pure-white pockets that
  are *not* connected to the border (between an arm and the head, under a hat brim) are carved back out. Edge pixels keep
  rembg's soft alpha, so there is no hard fringe.
- Faint grey contact shadow: alpha at or below `ALPHA_FLOOR` is zeroed.

After changing thresholds, re-check the umbrella sets (`warm_rain`, `cool_rain`, `cold_rain`, `rain_wind`) and
`heat_*` (white pockets) on a dark background.

## Removing the baked card from bg_sunny

```bash
tools/assets/.venv/bin/python tools/assets/remove_baked_card.py   # raw bg_sunny.webp -> bg_sunny_fixed.png
```

The card is a flat white overlay, `observed = orig * (1 - a) + 255 * a`, so the original is recovered exactly, not
painted over. Everything is fitted from the image and printed: overlay strength `a` (~0.435), top edge (~y 1982), edge
softness, and the corner shape. The corners are a circle (radius ~52) whose arc is cut off by the image border, because the
card is ~7 px wider than the image on each side; ignoring that leaves a bright wedge in the outer pixels. The lossy source
also rings around the sharp edge, so a thin band (+-5 px) along the old boundary is re-interpolated from the clean pixels on
both sides, along the edge normal. Checked by: edge-row residual < 1.5 levels, and the old edge zone being no noisier than
untouched grass.

## Softening bg_storm and calming bg_rain

```bash
tools/assets/.venv/bin/python tools/assets/calm_backgrounds.py storm   # -> design/raw_backgrounds/bg_storm_fixed.png
tools/assets/.venv/bin/python tools/assets/calm_backgrounds.py rain    # -> design/raw_backgrounds/bg_rain_fixed.png
```

Both are processed from the raw render (which stays untouched) and print the number they are judged by:

- **storm**: the cloud base was a ruler-straight warm-brown line. It is blended with a vertically blurred copy under a
  noise-warped mask, plus a faint haze that fades in smoothly (a hard haze start leaves its own visible line). Largest
  row-to-row luminance step in the seam window: 4.05 -> 0.81 (target < 1.5); everything outside the window is unchanged.
- **rain**: the bottom third had ~20x the detail of the top (vertical-gradient energy 5.1 vs 0.24), which fights UI placed
  there. Progressive blur (0 -> 16 px from row 1700 down) and a pull toward the local mean colour. Bottom-third energy
  5.12 -> 0.77 (target <= 1.0); rows above 1700 are bit-identical; puddles stay as soft lilac patches.

`bg_mapping.csv` points `storm` and `rain` at these `*_fixed.png` files, like `sunny`.
