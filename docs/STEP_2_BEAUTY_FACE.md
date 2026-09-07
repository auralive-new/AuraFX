# AuraFX Step 2 — Skin, Beauty, Face Shape

Standalone SDK only. No AuraLive integration.

## Architecture

```
CameraX Preview (GPU OES) + ImageAnalysis KEEP_ONLY_LATEST (same bind)
        │                         │
        │                         └─ MediaPipe Face Landmarker (VIDEO)
        │                              468/478 landmarks, EMA smoother
        ▼
 GLES resolve OES → 2D (display space, front-mirrored)
        ▼
 Region masks (skin / teeth / under-eye / protect)
        ▼
 Skin + color + teeth + circles shader (skin-aware, texture-preserving)
        ▼
 Localized UV warp mesh (V-face, cheeks, nose, eyes, mouth)
        ▼
 Host Surface
```

Still **one** camera pipeline. Analysis is a second use-case on the same `ProcessCameraProvider` bind, not a second camera.

## Public API

```kotlin
session.skin { smoothness = 0.35f; texturePreserve = 0.7f; whiten = 0.2f }
session.beauty { fineSmooth = 0.3f; toothWhiten = 0.4f; circles = 0.35f }
session.faceShape { vFace = 0.25f; eyeEnlarge = 0.2f; eyeDistance = -0.1f }
session.resetBeauty()
```

Overlapping fields (whiten, vFace, …) share one [BeautyRig] so groups cannot drift.

## Parameter ranges

| Control | Range | Default | Identity |
|---|---|---|---|
| Fine Smooth, Smoothness, Blemish, Evenness, Brightness, Whiten, Ruddy, Tooth, Circles, V Face, Cheeks, Nose, Eye Enlarge, Mouth | 0..1 | 0 | 0 |
| Eye Distance | -1..1 | 0 | 0 |
| Texture preserve | 0..1 | 0.7 | — |
| Natural skin | 0..1 | 0.4 | — |
| Skin tone | 0..1 (0.5 neutral) | 0.5 | 0.5 |

At all identity values the GPU path skips beauty and presents the original OES blit.

## Vision provider

- **Implementation:** MediaPipe Tasks Vision `FaceLandmarker`, model `assets/models/face_landmarker.task`
- **Mode:** `RunningMode.VIDEO` on the analysis executor
- **Output:** detection, 468 mesh points, iris 468–477 when present, tracking id 1
- **Smoothing:** landmark EMA; large jumps treated as re-acquire
- **Hair/background:** geometric — face oval + protect (eyes, brows, lips, nostrils). **Not** a learned hair matte (no selfie-segmenter in this step)
- If the model fails to load, tracking stays `UNAVAILABLE` and beauty is skipped. No fake faces.

This environment cannot execute MediaPipe on a phone GPU/NPU. Load/detect is real code; runtime success is device-side.

## GPU processing

- Range-weighted blur on skin only; high-frequency detail mixed back via `texturePreserve`
- Protect mask blocks eyes / brows / lips / lashes / nostrils from smooth/whiten
- Teeth channel only drives tooth whitening
- Under-eye channel only drives Circles
- Warp uses a 36×36 grid with oval falloff so background/hair/shoulders are not globally scaled

## Known limitations

- Physical visual quality is **unverified** (no phone on ADB)
- Hair strand matting is not a neural matte
- Inner-mouth teeth mask is landmark-based; closed mouth has little/no teeth area (correct)
- MediaPipe + CameraX rotation/mirror must be confirmed on device (`lastIngress`, `vision=mediapipe-face-landmarker`, `lm=468/478`)

## Tests

JVM: parameter clamp/reset/identity, warp bounds/indices, polygon mask, smoother, shader uniform presence. Not a substitute for a face on camera.
