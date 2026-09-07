# AuraFX

Standalone Android SDK for a real-time camera → GPU beauty/makeup/filter/scene/AR pipeline, plus Studio capture and a basic editor.

This repository is **not** AuraLive. Do not copy these sources into AuraLive until Step 8.

Roadmap:

1. Core + Camera Pipeline
2. Skin + Beauty + Face Shape
3. Professional Makeup
4. Filters
5. Background + Hair + Body + Lighting
6. AR / Effects / Masks
7. **Studio + Video + Performance + QA** ← this step
8. AuraLive Integration (not started)

## Modules

| Module | Role |
|---|---|
| `aurafx-sdk` | Production SDK library |
| `aurafx-sample` | Debug harness (not AuraLive) |
| `aurafx-studio` | Independent Studio app using **only** public SDK APIs |

## Public API (Studio)

```kotlin
AuraFx.initialize(context)
val session = AuraFx.createSession(context).getOrNull()!!
session.attachPreview(surface, width, height)
session.startCamera(lifecycleOwner, LensFacing.FRONT) // or BACK
session.switchCamera() // refused while recording
session.skin { smoothness = 0.3f }
session.faceShape { vFace = 0.2f }
session.makeup { lipstick { intensity = 0.5f } }
session.setFilter("warm.golden", 0.65f)
session.background { enabled = true; id = "bg.blur.soft"; intensity = 0.7f }
session.setHairStyle("hair.style.bob")
session.hair { enabled = true; color = HairColorId.Auburn; intensity = 0.55f }
session.body { enabled = true; slim = 0.2f }
session.lighting { enabled = true; mode = LightingMode.Soft; intensity = 0.45f }
session.setAREffect("ar.cupid", 0.75f)
session.capturePhoto(file) { /* processed JPEG */ }
session.startRecording(file, recordAudio = true)
session.stopRecording { /* processed MP4 */ }
session.resetAll()
session.performanceSnapshot()
session.release()
```

Photo and video use the **final GPU frame**, not a raw camera dump.

## Pipeline

CameraX (single bind) → vision → background → makeup → beauty → hair (color + grooms) → body → lighting → filter → AR → preview / encoder.

Docs: [`docs/STEP_7_FINAL_SDK_QA.md`](docs/STEP_7_FINAL_SDK_QA.md) · inventory: [`docs/REFERENCE_FEATURE_INVENTORY.md`](docs/REFERENCE_FEATURE_INVENTORY.md)

## Build

Requires JDK 17+ and Android SDK 35.

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :aurafx-sdk:assembleRelease :aurafx-sdk:test :aurafx-studio:assembleDebug
```

Install Studio on a **physical** device for visual QA. Unit tests do not prove realism.

Named options from the supplied inventory: [`docs/REFERENCE_FEATURE_INVENTORY.md`](docs/REFERENCE_FEATURE_INVENTORY.md) · completion: [`docs/FINAL_FEATURE_COMPLETION_REPORT.md`](docs/FINAL_FEATURE_COMPLETION_REPORT.md).

Video audit (six recordings were not in this workspace): [`docs/REFERENCE_VIDEO_INVENTORY.md`](docs/REFERENCE_VIDEO_INVENTORY.md).
PHYSICAL DEVICE = PENDING until a phone is connected.
