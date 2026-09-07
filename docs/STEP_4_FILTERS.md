# AuraFX Step 4 — Real-Time Filters

Standalone SDK. No AuraLive. No Step 5 background/hair/body/lighting.

Physical-device visual QA is **PENDING**. Unit tests do **not** prove on-face realism.

## Filter architecture

```
CameraX Preview + ImageAnalysis (one bind — unchanged)
  → MediaPipe Face Landmarker
  → MakeupPipelineEffect
  → BeautyPipelineEffect
  → FilterEngine / FilterPipelineEffect
  → GPU present to host Surface
```

Types:

| Type | Role |
|---|---|
| `FilterDefinition` | id, category, displayName, grade, LUT flag, intensity range, capabilities |
| `FilterCatalog` | data-driven preset list |
| `FilterEngine` / `FilterRig` | session state (one primary + optional finish) |
| `FilterRenderer` / `FilterPipelineEffect` | GLES 3 grade + 3D LUT + skin mix |
| `Lut3d` / `LutAssetLoader` | lattice, trilinear sample, `.cube` validation |

Switching filters only swaps the rig snapshot and, when needed, uploads a 32³ LUT. The camera bind, landmarker, makeup, and beauty state are untouched.

## Processing order (intentional)

**Makeup → Beauty / face shape → Filter → present.**

Color filters run **after** cosmetics so lipstick, liner, and lens colors are graded with the face instead of being computed on an already-shifted white balance. Beauty remains after makeup (Step 3). Filters never bind a second CameraX use case.

Intensity `0` skips the filter pass entirely (`processedTextureId` left as makeup/beauty/OES). That is the original path.

## GPU pipeline

1. If makeup/beauty already wrote a 2D texture, copy it. Else resolve `TEXTURE_EXTERNAL_OES` (same front-camera mirror as beauty).
2. Upload reused 144² region mask (skin / teeth / protect: eyes, brows, lips, iris).
3. Apply parametric grade **or** sample a baked 3D LUT (not both stacked).
4. Skin-aware mix: reduced grade on skin; stronger restore on protect + teeth (limit yellow/spill).
5. Optional bloom (highlight neighborhood), vignette, grain.
6. `mix(original, filtered, intensity)`.

Shaders compile once on attach. FBOs, mask texture, and LUT texture are reused. LUT bytes upload only when the filter id changes.

## LUT architecture

- Lattice size 32, `GL_TEXTURE_3D`, linear filtering (GPU interpolation).
- Catalog LUT looks bake the same `ColorGrade` used by the CPU processor (`Lut3d.fromGrade`).
- `LutAssetLoader.parseCube` validates size, count, and finite samples for future host files. No dummy identity `.cube` assets are shipped.
- Intensity still blends source vs LUT result; it is not an on/off toggle.

## Intensity

| Value | Meaning |
|---|---|
| 0.0 | original path (effect skipped) |
| 0.25 | subtle mix |
| 0.50 | medium |
| 0.75 | strong |
| 1.0 | full intended grade |

Optional secondary processing is **not** a second LUT. `FilterFinish` adds grain and/or vignette after the primary look so two color transforms cannot accumulate.

## API

```kotlin
session.setFilter("warm.golden", 0.65f)
session.filter {
    id = "lut.film_warm"
    intensity = 0.5f
    finish = FilterFinish.Grain
    finishIntensity = 0.3f
}
session.clearFilter()
session.resetFilter()
session.filterCatalog()
session.filterParameters()
```

Unknown ids return `AuraFxError.UnknownFilter` from `setFilter` and leave state unchanged. Every public field is consumed by `FilterPipelineEffect`.

## Reference inventory

Inspected this repository and prior step context for Tango recordings, filter icon sheets, and LUT packs.

| Source | Result |
|---|---|
| Repo images/video/LUT files | **none** |
| AuraLive / Tango preview | **not in this project** (locked until Step 8) |
| User-named categories in the Step 4 brief | **used** |

Items that cannot be identified from available material are **not invented**.

**UNREADABLE — NEEDS FRAME REVIEW:** every additional Tango-only category name, tray icon, and per-filter title that would only be visible in the supplied reference recordings/screenshots. Those assets were not present in the workspace.

## Implemented category inventory

| Category | Implemented filters |
|---|---|
| Natural | `natural.true`, `natural.balanced`, `natural.daylight`, `natural.even` |
| Warm | `warm.golden`, `warm.amber`, `warm.honey`, `warm.sunset` |
| Cool | `cool.arctic`, `cool.steel`, `cool.moonlight`, `cool.cyan_shadow` |
| Glow | `glow.pearl`, `glow.halo`, `glow.backlight` |
| Soft | `soft.matte`, `soft.haze`, `soft.pastel` |
| Portrait | `portrait.studio`, `portrait.rembrandt`, `portrait.editorial`, `portrait.key` |
| Vibe | `vibe.punch`, `vibe.teal_orange`, `vibe.neon`, `vibe.urban` |
| Mood | `mood.noir`, `mood.dusk`, `mood.fog`, `mood.ember` |
| Classic | `classic.print`, `classic.chrome`, `classic.fade`, `classic.silver` |
| LUT | `lut.film_warm`, `lut.film_cool`, `lut.cross_process`, `lut.contrast_s`, `lut.split_tone` |

Names describe the grade. They are **not** claimed Tango product names.

## Completeness vs reference

| Metric | Count |
|---|---|
| Reference categories (brief) | 10 |
| Reference individual Tango icons/titles | **UNREADABLE — NEEDS FRAME REVIEW** (assets missing) |
| Implemented categories | 10 |
| Implemented individual filters | 39 |
| Missing specified categories | 0 |
| Placeholders (identity grades / fake thumbs / color overlays) | 0 |
| Functional (GPU grade or real LUT + intensity mix) | 39 |

## Skin-aware filtering

Uses the Step 2 region mask:

- **Skin:** mix toward a milder grade so warmth/cool looks do not orange or gray the face as hard as the background.
- **Eyes / brows / lips / iris (protect):** restore toward the pre-filter face (makeup preserved).
- **Teeth:** limit yellow/red lift.
- **Lashes:** covered by dilated eye protect.
- **Hair:** no neural matte in Step 4; hair outside the face oval receives the full creative grade (background/hair) while skin is protected. Step 5 must not be started here.

## Thumbnails

`FilterThumbnail.render` applies the same `ColorGrade` / baked LUT to a synthetic color-chart + skin strip. Thumbnails are not unrelated PNGs.

## Tests

JVM tests cover catalog validity, unique ids, category membership, ranges, intensity 0, reset, switching, unknown id, LUT validation and sampling bounds, `.cube` loader, shader source presence, thumbnail divergence, finite math. They do **not** claim visual realism.

## Known limitations

- No physical camera QA in this environment (`adb` empty).
- Hair/body/background mattes are out of scope (Step 5).
- Tango icon-accurate names cannot be filled without the recordings.
- Bloom is a same-pass neighborhood highlight, not a multi-resolution glare model.
- Grain is hash noise, not scanned film stock.

## Physical-device QA

**PENDING**
