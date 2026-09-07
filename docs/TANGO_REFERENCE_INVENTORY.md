# Tango reference inventory

**Status: NEEDS SOURCE MATERIAL**

**TANGO COMPLETE = NO**

This document is **not** a finished Tango count. No Tango recording, screenshot, icon sheet, LUT pack, or AuraLive/Tango APK was present in this Cursor workspace on 2026-09-07. Names were **not** invented to fill carousels. Count-gate arithmetic that needs a verified Tango total is **unknown**.

AuraFX original catalogs remain (filters, AR, backgrounds). Those are **not** claimed as Tango-named 1:1 matches.

Step 8 / AuraLive was not started.

## 1. Source files actually inspected

| Location / pattern | Result |
|---|---|
| `/workspace` (`*.mp4`, `*.mov`, `*.webm`, `*.mkv`, `*tango*`) | **none** |
| `/workspace/docs` | Architecture/status markdown only; no frames |
| `/workspace/aurafx-sdk/src/main/assets` | MediaPipe models + hair groom JSON; no Tango UI |
| `/opt/cursor/artifacts` | unused for this task |
| Git history of this repo | New empty project; no binary reference commits |
| AuraLive / Tango app | **not in this repository** (Step 8 locked) |

**Missing source material (required before any TANGO COMPLETE claim):**

- Tango screen recordings of Filters, AR/Effects/Masks, Backgrounds (full carousels, including brief tiles)
- Readable icon text / accessibility labels
- Any official inventory export

Until those files are added to the workspace, **do not treat the cited lists below as X.**

## 2. Count gate (verified Tango totals)

| Metric | Value |
|---|---|
| Tango Filters total | **UNKNOWN** |
| AuraFX Filters total | **39** |
| Missing Filters (verified Tango − matching AuraFX) | **UNKNOWN** |
| Tango AR / Effects / Masks total | **UNKNOWN** |
| AuraFX AR / Effects / Masks total | **10** |
| Missing AR / Effects / Masks | **UNKNOWN** |
| Tango Backgrounds total | **UNKNOWN** |
| AuraFX Backgrounds total | **20** |
| Missing Backgrounds | **UNKNOWN** |
| AuraFX ≥ Tango (every category) | **UNDECIDABLE** |

Example thresholds in the task (“if Tango has 50 / 87 / 30”) are **illustrations of the rule**, not measured Tango counts. They were **not** used to pad AuraFX.

Code gate: `com.aurafx.sdk.reference.TangoReferenceGate` (`SOURCE_AVAILABLE = false`, `TANGO_COMPLETE = false`).

## 3. FILTERS

### 3a. Cited Tango categories (starting evidence — not frame-verified)

Reference Verified = **NO** for every row. AuraFX categories are original (`Natural`, `Warm`, `Cool`, `Glow`, `Soft`, `Portrait`, `Vibe`, `Mood`, `Classic`, `Lut`) and are **not** claimed as these Tango tray labels.

| Category | Subcategory | Exact Tango Name | AuraFX ID | AuraFX Status | Rendering Type | Reference Verified |
|---|---|---|---|---|---|---|
| 360° | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| LIVE | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| GLOW | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| PATTERNS | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| BLUR | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| TANGO STYLE | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| ANIME | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| ANIMAL PRINT | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| NATURE | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| SCENERY | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |
| ROOMS | UNREADABLE | UNREADABLE — NEEDS FRAME REVIEW | — | NEEDS SOURCE MATERIAL | — | NO |

Carousel tiles under each category: **UNREADABLE — NEEDS FRAME REVIEW** (count unknown).

### 3b. Cited Tango filter examples (starting evidence — not frame-verified)

These strings were listed in the task as previously observed. They were **not** re-read from frames in this run. None exist as AuraFX `displayName` values.

| Category | Subcategory | Exact Tango Name (cited) | AuraFX ID | AuraFX Status | Rendering Type | Reference Verified |
|---|---|---|---|---|---|---|
| UNREADABLE | UNREADABLE | Rainy Street | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | Neon Clouds | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | Neon Pattern | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | City Sunset | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | Desert | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | Canopy Bed Interior | — | NEEDS SOURCE MATERIAL | — | NO |
| UNREADABLE | UNREADABLE | Day Light | — | NEEDS SOURCE MATERIAL | — | NO |

**Not implemented** under those names: adding GPU grades with guessed Tango titles would invent a 1:1 mapping without frames (behavior, intensity, skin protect, whether they are scene replacements vs color grades).

### 3c. AuraFX filter catalog (original — not Tango-verified)

All 39 are GPU `ColorGrade` / 32³ LUT + intensity + skin protect. Status vs Tango: **NEEDS SOURCE MATERIAL** (cannot PASS a Tango row without a Tango name).

| AuraFX category | Count | Example IDs |
|---|---|---|
| Natural | 4 | `natural.true`, `natural.balanced`, `natural.daylight`, `natural.even` |
| Warm | 4 | `warm.golden`, `warm.amber`, `warm.honey`, `warm.sunset` |
| Cool | 4 | `cool.arctic`, `cool.steel`, `cool.moonlight`, `cool.cyan_shadow` |
| Glow | 3 | `glow.pearl`, `glow.halo`, `glow.backlight` |
| Soft | 3 | `soft.matte`, `soft.haze`, `soft.pastel` |
| Portrait | 4 | `portrait.studio`, `portrait.rembrandt`, `portrait.editorial`, `portrait.key` |
| Vibe | 4 | `vibe.punch`, `vibe.teal_orange`, `vibe.neon`, `vibe.urban` |
| Mood | 4 | `mood.noir`, `mood.dusk`, `mood.fog`, `mood.ember` |
| Classic | 4 | `classic.print`, `classic.chrome`, `classic.fade`, `classic.silver` |
| Lut | 5 | `lut.film_warm`, `lut.film_cool`, `lut.cross_process`, `lut.contrast_s`, `lut.split_tone` |

## 4. AR / EFFECTS / MASKS

### 4a. Tango AR tray

**UNREADABLE — NEEDS FRAME REVIEW.** No Tango AR/mask names were supplied as a verified recording inventory. The ten names below were listed in the task as **currently implemented AuraFX examples**, not as Tango-verified titles.

| Category | Subcategory | Exact Tango Name | AuraFX ID | AuraFX Status | Tracking | Animation | Occlusion | Reference Verified |
|---|---|---|---|---|---|---|---|---|
| UNREADABLE | UNREADABLE | UNREADABLE (full tray) | — | NEEDS SOURCE MATERIAL | — | — | — | NO |

### 4b. AuraFX AR catalog (original GPU looks)

Implementation exists (landmarks, expression hooks, hair-channel occlusion, particles). **Tango PASS is not claimed.** Physical visual QA **PENDING**.

| Category (AuraFX) | Subcategory | Exact Tango Name | AuraFX ID | AuraFX Status | Tracking Required | Animation Required | Occlusion Required | Reference Verified |
|---|---|---|---|---|---|---|---|---|
| FaceAccessory / Particle | — | (not Tango-verified) | `ar.desert_sun` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceMask | — | (not Tango-verified) | `ar.purrfect_match` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceMask | — | (not Tango-verified) | `ar.black_cat` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceAccessory / Particle | — | (not Tango-verified) | `ar.party_hop` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FacePaint | — | (not Tango-verified) | `ar.pride_paint` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceAccessory | — | (not Tango-verified) | `ar.summer_vibes` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FacePaint | — | (not Tango-verified) | `ar.red_hero` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FacePaint | — | (not Tango-verified) | `ar.blush_pop` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceAccessory / Particle | — | (not Tango-verified) | `ar.cupid` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |
| FaceAccessory | — | (not Tango-verified) | `ar.moonlit_glow` | REAL BUT VISUALLY DIFFERENT | YES | YES | YES | NO |

Missing Tango AR tiles beyond these 10: **UNKNOWN**.

## 5. BACKGROUNDS

No Tango background **names** were cited. Full Tango background tray: **UNREADABLE — NEEDS FRAME REVIEW**.

AuraFX has **20** procedural/blur GPU composites with person segmentation. They are **not** Tango-labeled.

| Category | Exact Tango Name | AuraFX ID | AuraFX Status | Segmentation | GPU | Reference Verified |
|---|---|---|---|---|---|---|
| UNREADABLE | UNREADABLE (full tray) | — | NEEDS SOURCE MATERIAL | — | — | NO |
| (AuraFX original) | (not Tango-verified) | `bg.blur.soft` … `bg.env.paper_warm` (20) | REAL BUT VISUALLY DIFFERENT | YES | YES | NO |

## 6. Other cited Tango UI (beauty / makeup)

These strings were listed as previously observed **controls**, not as a complete Tango screenshot dump. AuraFX already has matching **parameters / enums** from Steps 2–3. Visual identity vs Tango: **not frame-verified**. Physical QA **PENDING**.

| Tango-cited label | AuraFX | AuraFX Status | Reference Verified |
|---|---|---|---|
| Fine Smooth | `beauty.fineSmooth` | REAL BUT VISUALLY DIFFERENT | NO |
| Tooth Whiten | `beauty.toothWhiten` | REAL BUT VISUALLY DIFFERENT | NO |
| Whiten | `beauty.whiten` | REAL BUT VISUALLY DIFFERENT | NO |
| Ruddy | `beauty.ruddy` | REAL BUT VISUALLY DIFFERENT | NO |
| V Face | `faceShape.vFace` | REAL BUT VISUALLY DIFFERENT | NO |
| Cheek Thin / Small / Narrow | `cheekThin` / `cheekSmall` / `cheekNarrow` | REAL BUT VISUALLY DIFFERENT | NO |
| Nose | `faceShape.nose` | REAL BUT VISUALLY DIFFERENT | NO |
| Eye Enlarge / Distance | `eyeEnlarge` / `eyeDistance` | REAL BUT VISUALLY DIFFERENT | NO |
| Mouth | `faceShape.mouth` | REAL BUT VISUALLY DIFFERENT | NO |
| Circles | `beauty.circles` | REAL BUT VISUALLY DIFFERENT | NO |
| Makeover Classic / Bright / Extravagant | `MakeupPreset` | REAL BUT VISUALLY DIFFERENT | NO |
| Blush Soft Touch, Airbrush, Blush Bomb, Sun-Kissed | `BlushStyle` | REAL BUT VISUALLY DIFFERENT | NO |
| Lip Glossy Pop / Lacquer / Ombre | lipstick + gloss modules; **named lip looks not a 3-preset enum** | INCOMPLETE vs cited names | NO |
| Eyebrow 7 styles | `BrowStyle` | REAL BUT VISUALLY DIFFERENT | NO |
| Eyeliner 11 styles | `EyelinerStyle` | REAL BUT VISUALLY DIFFERENT | NO |
| Eyelash 6 styles | `LashStyle` | REAL BUT VISUALLY DIFFERENT | NO |
| Lens 6 styles | `LensStyle` | REAL BUT VISUALLY DIFFERENT | NO |

Cited lip look names (Glossy Pop, Lacquer, Ombre) were **not** added as Tango-named presets without recordings (intensity/color/gradient behavior unread).

## 7. What must happen when source arrives

1. Place recordings under something like `reference/tango/`.
2. Inventory every selectable tile (pause brief frames).
3. Fill the tables with **exact on-screen names**.
4. Implement missing GPU features using original AuraFX assets (no Tango binaries).
5. Recompute count gate. Only then can any category be **PASS**.

## 8. Status summary

| Claim | Result |
|---|---|
| TANGO INVENTORY ⊆ AURAFX INVENTORY | **NOT PROVEN** |
| Filter / AR / background Tango PASS | **NO** |
| Reference inventory | **NEEDS SOURCE MATERIAL** |
| Invented Tango names this run | **0** |
| Placeholders counted as Tango features | **0** |
