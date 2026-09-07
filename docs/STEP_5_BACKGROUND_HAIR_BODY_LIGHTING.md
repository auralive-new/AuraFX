# AuraFX Step 5 — Background, Hair, Body, Lighting

Standalone SDK. No AuraLive. Step 6 AR/effects is a later GPU pass.

Physical-device visual QA is **PENDING**. Unit tests do **not** prove on-device edge quality.

## Architecture

One CameraX bind (Preview GPU + ImageAnalysis KEEP_ONLY_LATEST). The analysis callback runs three MediaPipe tasks on the **same** RGBA frame:

| Provider | Model | Output |
|---|---|---|
| Face | `models/face_landmarker.task` | 468/478 landmarks (unchanged) |
| `BackgroundMaskProvider` / `HairMaskProvider` / `BodySegmentationProvider` | `models/selfie_multiclass_256x256.tflite` | Semantic classes: background, **hair**, body-skin, face-skin, clothes, other |
| `PoseProvider` | `models/pose_landmarker_lite.task` | 33 pose landmarks, `numPoses = 1` |

Packed GPU mask (uploaded once per frame):

- R = person (non-background)
- G = hair
- B = body-skin + clothes
- A = face-skin (protects hair dye and body warp)

This is **not** a face-oval approximation.

## Processing order

```
CameraX
  → Vision (face + multiclass + pose)
  → SegmentationUploadEffect
  → BackgroundPipelineEffect
  → MakeupPipelineEffect          (now chains prior 2D if present)
  → BeautyPipelineEffect
  → HairPipelineEffect            (recolor only)
  → BodyPipelineEffect            (localized pose warp)
  → LightingPipelineEffect
  → FilterPipelineEffect
  → ARPipelineEffect           (Step 6)
  → Present
```

Background runs **before** makeup so cosmetics are graded on the composited person, not thrown away. Makeup was updated to copy an existing processed texture instead of always resolving OES. Lighting stays after beauty/makeup so fill light hits finished skin and cosmetics, then the Step 4 filter grades the whole frame.

## GPU

- Background: edge-weighted blur (samples inverse person weight) **or** procedural solid/gradient/environment shaders. No full-frame translucent rectangle. No photo PNG placeholders. `BackgroundType.Image` is in the type system but **no image assets are shipped**; catalog entries are procedural.
- Hair color: luminance-preserving dye on the hair channel; face-skin excluded. Highlights are partially restored from source luma.
- Hair **styles**: catalog + head anchors (`crown`, `hairline`, `temples`, pose). Only `hair.style.natural` is production-ready (own hair). The other 12 require a real groom/strand asset bundle and **do not draw a PNG sticker**.
- Body: pose-centered UV warp with safe clamps; fragment mix uses body mask and zeros warp on face, hair, and background. Hands down-weighted via wrist Gaussians. Not a uniform image scale.
- Lighting: subject-masked fill / directional / warm / cool / natural. Shadow lift and highlight compression. Not a white overlay.

Shaders compile once. Mask/LUT/FBOs reused. Switching backgrounds or colors does not rebind CameraX.

## API

```kotlin
session.background { enabled = true; id = "bg.blur.soft"; intensity = 0.7f }
session.hair { enabled = true; color = HairColorId.Auburn; intensity = 0.55f }
session.body { enabled = true; slim = 0.25f; waist = 0.2f }
session.lighting { enabled = true; mode = LightingMode.Soft; intensity = 0.45f }
session.resetBackground(); session.resetHair(); session.resetBody(); session.resetLighting()
```

Every field is consumed by the matching GPU effect.

## Catalogs

- **20** background definitions (blur, solid, gradient, procedural environments).
- **13** hairstyle definitions (1 real identity, 12 `RequiresGroomAsset`).
- **12** hair colors including Custom.
- **6** body transforms: slim, waist, shoulders, legs, arms, torso.
- **5** lighting modes: Soft, Directional, Warm, Cool, Natural.

## Multi-person / edges

- Multiclass segmentation is **semantic**, not instance IDs. All people share one person/hair/body matte.
- Pose is tracked for **one** primary body. A second person is not warped independently.
- No person (`personCoverage` too low): background/lighting skip (do not replace the whole frame).
- No hair / no pose: those effects skip.
- Front camera: landmarks, pose, and masks are mirrored with the existing preview convention.
- Fast motion: category masks are temporally mixed (EMA). Still model-limited.

## Known limitations (unsupported as production-real)

- **12 replacement hairstyles** (`Step5Outstanding`) require real groom/strand assets. They are **not** complete. Close in **Step 7** QA. Hair matte + hair color remain real.
- Image-file backgrounds (architecture only; no placeholder bitmaps).
- Instance-aware multi-person body warp.
- Dedicated neural hair-strand simulation.
- Physical-device halo/ear/hand QA (PENDING).

## Physical-device QA

**PENDING**
