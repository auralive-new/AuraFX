# AuraFX Step 6 — AR, Effects, Masks

Standalone SDK. No AuraLive. No Step 7 studio/video/QA.

Physical-device visual QA is **PENDING**. Unit tests do **not** prove on-face realism.

## Architecture

| Type | Role |
|---|---|
| `AREngine` / `ARRig` | Session state (one primary effect + intensity) |
| `AREffectManager` | Switch/query active definition |
| `ARAssetManager` | Procedural GPU sprites/meshes (not PNG stickers) |
| `ARTrackingContext` | Head pose + blink/smile/mouth/brow from MediaPipe landmarks |
| `ARRenderContext` | Per-frame GPU sprite count |
| `ARAnimationEngine` | Fixed 96-particle pool, timeline, easing, bounce |
| `ARPipelineEffect` | GLES compose + sprites after the filter pass |

Order:

```
Camera → Vision → masks → Background → Makeup → Beauty → Hair → Body → Lighting → Filter → AR → Present
```

AR is last so makeup, lighting, and color filters stay on the face; accessories/particles composite on the finished frame. Hair/person masks from Step 5 still drive occlusion.

One CameraX bind. No extra MediaPipe instances.

## Tracking

`ARTrackingContext` uses the **primary** face mesh only (`trackingId` from Face Landmarker `numFaces=1`).

- Head pose: roll from eye line, yaw/pitch from nose vs mid-eye, scale from inter-ocular distance
- Blink: eye aspect (lids 159/145, 386/374)
- Smile: mouth-corner span vs iod
- Mouth open: inner-lip gap
- Brow raise: brow-to-lid distance

Not fake UI triggers. No second-person attach.

## Occlusion

Sprite fragment samples the packed scene mask (G = hair). `hairOcclude` on each definition fades accessories that should sit behind hair. Face paint uses face-skin / person channels so paint stays on skin, not background.

## Catalog (10 original AuraFX looks)

Reference Tango recordings are **not** in this workspace. Extra icon names were **not invented**.

**REFERENCE INVENTORY = NEEDS SOURCE MATERIAL**

Implemented and **GPU-rendered** (procedural, original art):

| ID | Name | Types |
|---|---|---|
| `ar.desert_sun` | Desert Sun | halo accessory, warm atmosphere, dust particles |
| `ar.purrfect_match` | Purrfect Match | cat ears/whiskers (head pose + brow), eye glow |
| `ar.black_cat` | Black Cat | dark ears, gold iris glow, smile sparkles |
| `ar.party_hop` | Party Hop | glasses, confetti, smile bounce |
| `ar.pride_paint` | Pride Paint | tracked rainbow cheeks/forehead, sparkles |
| `ar.summer_vibes` | Summer Vibes | glasses, warm atmosphere, motes |
| `ar.red_hero` | Red Hero | forehead mark, red eye glow, energy particles |
| `ar.blush_pop` | Blush Pop | tracked blush, hearts on smile |
| `ar.cupid` | Cupid | heart halo, heart particles from mouth/smile |
| `ar.moonlit_glow` | Moonlit Glow | moon disc, cool wash, iris glow, stars |

Thumbnails are generated from the same look id, not unrelated PNGs.

## Asset / animation

Assets are SDF sprites (ears, glasses, halo, moon, whiskers, hearts, stars, confetti, dust) transformed by landmark anchors every frame. Particle slots are reused; switching effects resets the pool.

## API

```kotlin
session.setAREffect("ar.cupid", 0.75f)
session.ar { effectId = "ar.purrfect_match"; intensity = 0.8f }
session.setAREffectIntensity(0.5f)
session.clearAREffect()
session.resetAREffects()
```

Unknown ids → `AuraFxError.UnknownEffect`. Intensity 0 skips the GPU pass.

## Step 5 gap (do not mark complete)

12 replacement hairstyles still `RequiresGroomAsset`. See `Step5Outstanding`. Hair **color** and **matte** stay real. Close in Step 7.

## Limitations

- One primary face; semantic (not instance) hair/person mattes
- No blendshape stream (expression is landmark geometry)
- No Tango-complete icon inventory until recordings are in the repo
- Physical device PENDING

## Physical-device QA

**PENDING**
