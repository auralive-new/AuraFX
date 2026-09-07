# AuraFX Step 1A — Architecture Report

**Status:** Inspection complete. This repository was empty (root commit only). There is **no existing Android project**, **no AuraLive application**, and **no prior AuraFX SDK**.

Step 1A therefore **creates** a standalone production SDK from scratch. AuraLive Preview is not present, so it cannot be modified. AuraLive integration remains **Step 8**.

---

## Inspection findings

| Question | Result |
|---|---|
| Current Android project structure | None (empty git repo) |
| Current AuraLive structure | **Not present** in this workspace |
| Existing AuraFX SDK | **Does not exist** |
| Camera implementation | None |
| Rendering implementation | None |
| Dependencies | None |
| Package/module boundaries | None |
| Preview/camera pipeline | None |
| Beauty / Makeup / Filters / AR / Effects | None (correct — those are later steps) |
| Reusable vs isolated | All new SDK code is reusable; sample app is isolated and must never be copied into AuraLive |

**Rule followed:** AuraFX is a standalone library module. A future AuraLive app must depend on `:aurafx-sdk` (AAR / Maven), not copy SDK sources.

---

## Module boundaries

```
AuraFX (Gradle root)
├── aurafx-sdk     ← public SDK (no AuraLive types, no app screens)
└── aurafx-sample  ← independent harness for Step 1A/1B (NOT AuraLive)
```

- `aurafx-sdk` depends on AndroidX Camera, Lifecycle, and the Android SDK only.
- `aurafx-sample` depends on `aurafx-sdk`.
- No reverse dependency. No shared “app business” module.

**Package:** `com.aurafx.sdk` (public API in `com.aurafx.sdk.api`; internals in `internal` packages).

---

## Files to create

### Root / Gradle
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/wrapper/*`, `gradlew*`
- `.gitignore`, `local.properties` (local SDK path, gitignored)
- `README.md`, this document

### `aurafx-sdk` (library)
- Public API: `AuraFx`, `AuraFxSession`, configs, results/errors, `LensFacing`
- Camera: CameraX controller, permission/capability checks, orientation/mirror
- Pipeline: single-active `FramePipeline` (KEEP_ONLY_LATEST)
- GPU: EGL core, window surface, OES program, render thread, GPU timer (optional)
- Vision: interfaces only + empty `TrackingData` (no fake detections)
- Effects: `Effect`, `EffectManager`, `EffectContext`, `FrameContext` (no beauty/makeup/filters)
- Performance: real counters only
- Unit tests for state machine, backpressure, effects, performance math

### `aurafx-sample` (debug app)
- Camera permission UI, SurfaceView preview, front/back, start/stop/release
- Diagnostics overlay reading **SDK-reported** metrics (not invented)

## Files to modify

- None in any AuraLive tree (AuraLive is absent).

## Files that must remain untouched

- Any future AuraLive Preview screen / live feature code — **do not add or change it in this step**.
- No Beauty, Makeup, Filter, Background, Hair, Body, Lighting, AR, or Effects implementations.

---

## Dependencies (SDK)

| Library | Why |
|---|---|
| `androidx.camera:camera-core/camera-camera2/camera-lifecycle` | CameraX capture, lifecycle bind, KEEP_ONLY_LATEST analysis (reserved) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | Lifecycle-safe start/stop |
| `androidx.annotation` | Thread/nullable contracts |

**Not included in Step 1:** ML Kit, MediaPipe, beauty engines, OpenCV, Sceneform, Filament. Vision is interface-only.

Renderer: **OpenGL ES 3.0 + EGL 1.4 + `GL_TEXTURE_EXTERNAL_OES`**. Camera frames stay on GPU (Preview → SurfaceTexture). No CameraX `PreviewView` inside the SDK.

---

## Public API (minimal)

```
AuraFx.initialize(context, config)
AuraFx.isInitialized()
AuraFx.createSession(context, sessionConfig) → AuraFxSession
AuraFx.release()

AuraFxSession.attachPreview(surface, width, height)
AuraFxSession.startCamera(lifecycleOwner, lensFacing)
AuraFxSession.switchCamera()
AuraFxSession.stopCamera()
AuraFxSession.processFrame(input)   // external frames; camera path does not use this
AuraFxSession.performanceSnapshot()
AuraFxSession.release()
```

Apps request the CAMERA permission. The SDK **checks** permission and fails with `PermissionDenied`; it does not own a permission UI.

---

## Frame pipeline (exactly one)

```
CameraX (single ProcessCameraProvider bind)
  → Preview use case Surface (GPU, no YUV copy)
  → SurfaceTexture / OES texture (timestamp preserved)
  → FramePipeline (latest-only; busy renderer ⇒ drop + count)
  → VisionProcessor (no models in Step 1 → TrackingData.UNAVAILABLE)
  → EffectManager (empty list → identity)
  → GLES blit to client Surface
```

- One `ProcessCameraProvider` bind per session; start replaces the same bind (no second camera stack).
- ImageAnalysis is **not** bound in Step 1 (avoids CPU copies). The pipeline has a reserved hook for a future analyzer on a background executor.
- `processFrame` is a separate ingress for host-supplied buffers; it still goes through the same GL output path, never a second CameraX instance.

---

## Renderer architecture

- Dedicated **GL thread** owns EGL display/context/surfaces and all GL objects.
- `EglCore` + `EglWindowSurface` (window Surface from the host).
- `OesBlitProgram`: full-screen triangle, `samplerExternalOES`, CameraX/SurfaceTexture 4×4 UV matrix.
- Front camera: extra horizontal mirror in the blit (in addition to the texture transform).
- Display rotation: applied via the transform + viewport.
- GPU timer: `GL_TIME_ELAPSED` query **if** the driver reports query bits; otherwise snapshot field is `null` (not estimated).

---

## Threading

| Work | Thread |
|---|---|
| Public API / lifecycle | Caller (typically main) — non-blocking; work posted |
| CameraX | CameraX executors |
| GL / present | `AuraFxRenderThread` |
| Future vision | Single-thread `visionExecutor` (idle in Step 1) |

UI thread is not used for `updateTexImage`, EGL, or shader work.

---

## Lifecycle / resources

`start` → bind camera + connect OES surface  
`pause` (Lifecycle) → unbind camera, keep EGL  
`resume` → rebind if session still wanted running  
`stop` → unbind  
`release` → stop, destroy EGL, delete textures, quit GL thread, shutdown executors  

Errors: camera unavailable, permission denied, unsupported device (no GLES3 / no camera), GPU/EGL failure, lifecycle interruption, cleanup failure — all typed `AuraFxError`, with best-effort release on failure paths.

---

## Step 1 non-goals (explicit)

- No Beauty, Makeup, Filters, Background, Hair, Body, Lighting, AR, masks.
- No fake face/iris/segmentation/pose results.
- No AuraLive Preview changes (and no substitute AuraLive app).
- Physical-device pass/fail is **Step 1B**, not claimed here.
