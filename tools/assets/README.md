# Asset processing

`process_assets.py` turns the raw AI illustrations into the app's drawables (stage 4, Prompt A.1).

| Input | Output |
|---|---|
| `design/raw_characters/outfit_{set}_{gender}.webp` (24, white backdrop, 1792x2400) | `outfit_{set}_{gender}.webp`: transparent, trimmed, 24 px padding, 1600 px tall, WebP q90 |
| `design/raw_backgrounds/*` (8) + `tools/assets/bg_mapping.csv` | `bg_{weather}.webp`: 1080x2400 cover crop, WebP q85 |

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
```

It fails loudly if `mapping.csv` is missing one of the 24 set/gender combinations or a listed source file is absent,
and prints a warning for any figure shorter than 1600 px after trimming (never upscaled: regenerate those).

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
