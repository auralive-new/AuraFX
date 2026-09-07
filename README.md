# AuraFX

Standalone Android SDK for a real-time camera → GPU frame pipeline. This repository is **not** AuraLive. A future AuraLive app should depend on the `aurafx-sdk` module (AAR), not copy these sources.

Roadmap (locked):

1. **Core + Camera Pipeline** ← this repo / this step
2. Skin + Beauty + Face Shape
3. Professional Makeup
4. Filters
5. Background + Hair + Body + Lighting
6. Complete AR / Effects / Masks
7. Studio + Video + Performance + QA
8. AuraLive Integration + Production Release

Step 1 does **not** implement beauty, makeup, filters, background, hair, body, lighting, AR, or effects. Vision types are interfaces only.

## Modules

| Module | Role |
|---|---|
| `aurafx-sdk` | Production SDK library |
| `aurafx-sample` | Independent debug harness (not AuraLive Preview) |

Architecture: [`docs/STEP_1A_ARCHITECTURE.md`](docs/STEP_1A_ARCHITECTURE.md)

## Public API

```kotlin
AuraFx.initialize(context)
val session = AuraFx.createSession(context).getOrNull()!!
session.attachPreview(surface, width, height)
session.startCamera(lifecycleOwner, LensFacing.FRONT)
session.switchCamera()
session.stopCamera()
session.processFrame(input) // optional CPU ingress; camera path does not use this
session.performanceSnapshot()
session.release()
AuraFx.release()
```

The host app requests `CAMERA`. The SDK checks permission and returns `AuraFxError.PermissionDenied` if it is missing.

## Pipeline

CameraX (single bind) → GPU `SurfaceTexture` / `TEXTURE_EXTERNAL_OES` → latest-only frame gate → vision interfaces (none registered) → empty effect graph → GLES 3 blit to the host `Surface`.

Front-camera mirroring is applied in the blit shader. Frame timestamps come from `SurfaceTexture.timestamp`.

## Build

Requires JDK 17+ and Android SDK 35.

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :aurafx-sdk:assembleRelease :aurafx-sdk:test :aurafx-sample:assembleDebug
```

Install the harness on a device:

```bash
./gradlew :aurafx-sample:installDebug
```

Physical-device verification is Step 1B. Compilation and JVM unit tests do **not** constitute a Step 1 pass.

## Metrics

`PerformanceSnapshot` reports only observed values. `gpuTimeMs` is null when `GL_TIME_ELAPSED` queries are unsupported. `cameraStartupMs` is null until the first presented frame after `startCamera`.
