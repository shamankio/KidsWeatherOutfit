# Launcher icon

Source art: `icon_master.jpeg` (2142×2016, gradient background baked in, no alpha).

## How the adaptive icon is built

An adaptive icon is two 108 dp layers that the launcher masks (circle, squircle, rounded square, …) and moves slightly for parallax:

```
108 dp canvas ┌──────────────────────┐
              │  18 dp margin        │  masks and parallax eat into this
              │  ┌────────────────┐  │
              │  │ 72 dp visible  │  │  what the launcher shows (before masking)
              │  │  ┌──────────┐  │  │
              │  │  │ 66 dp    │  │  │  safe zone: a circle, nothing important may leave it
              │  │  └──────────┘  │  │
              │  └────────────────┘  │
              └──────────────────────┘
```

| Layer | File | Notes |
|---|---|---|
| Background | `@color/ic_launcher_background` (`values/ic_launcher_colors.xml`) | solid sky blue `#4FC3F7`, the theme seed colour in `ui/theme/Color.kt` |
| Foreground | `drawable-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png` | shirt cut out of the master, scaled so its enclosing circle is r = 32 dp (safe zone is r = 33 dp) |
| Monochrome | `drawable/ic_launcher_monochrome.xml` | flat silhouette (Android 13+ themed icons use only its alpha) |

`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` tie them together. minSdk is 26, so there are no legacy
`mipmap-*dpi` PNGs: every supported device resolves the adaptive icon. (`-v26` is redundant at minSdk 26, so don't also
keep a `mipmap-anydpi` copy; AAPT2 treats them as the same configuration and fails with a duplicate resource.)

Foreground sizes: 108 / 162 / 216 / 324 / 432 px for mdpi / hdpi / xhdpi / xxhdpi / xxxhdpi.

## Regenerating (script)

```bash
swift tools/icon/make_icons.swift     # from the repo root, macOS only
```

It cuts the shirt out with Vision, then writes:

- `design/icon/foreground_master.png` — full-resolution transparent cut-out
- `app/src/main/res/drawable-*dpi/ic_launcher_foreground.png` — the five foreground layers
- `app/src/main/ic_launcher-playstore.png` — 512×512 Play Store icon (solid background + art, no baked-in mask or shadow)
- `design/icon/preview.png` — the canvas under circle / squircle / rounded-square masks, with the 66 dp safe circle (red) and 72 dp visible box (blue)

To change the background or the art size, edit the constants at the top of the script. The monochrome vector is hand-authored
and is not touched by the script.

## Regenerating (Android Studio Image Asset)

Equivalent manual route: right-click `res` → **New → Image Asset** → *Launcher Icons (Adaptive and Legacy)*.

1. **Foreground Layer**: Source Asset = Image, path `design/icon/foreground_master.png`, Trim = Yes, drag Resize until the art sits inside the safe-zone circle overlay.
2. **Background Layer**: Asset Type = Color, `4FC3F7`.
3. **Options**: Legacy Icon = No, Round Icon = No, Google Play Store Icon = Yes. (Studio's *Monochrome* tab can reuse the foreground; ours is a hand-drawn silhouette instead.)
4. Studio generates the Play Store PNG at `src/main/ic_launcher-playstore.png` and adaptive XMLs under `mipmap-anydpi-v26`.

Note that Studio's density slicing writes the foreground as WebP in `mipmap-*` folders; this project uses PNG in `drawable-*dpi`. Either works.
