# AuraFX Step 7 — Final SDK QA

Standalone SDK. **No AuraLive. Do not start Step 8.**

Physical-device visual QA is **PENDING**. Automated tests are not visual realism proof.

REFERENCE SOURCE = **NOT AVAILABLE** (no Tango recordings in this workspace). Completeness audit vs Tango remains pending. See [`TANGO_REFERENCE_INVENTORY.md`](TANGO_REFERENCE_INVENTORY.md).

## Final architecture

One CameraX bind (Preview GPU SurfaceTexture + ImageAnalysis `KEEP_ONLY_LATEST` on the same bind):

```
CameraX Preview (OES) + ImageAnalysis
  → MediaPipe face + selfie multiclass + pose
  → SegmentationUploadEffect
  → BackgroundPipelineEffect
  → MakeupPipelineEffect
  → BeautyPipelineEffect
  → HairPipelineEffect          (dye + procedural grooms)
  → BodyPipelineEffect
  → LightingPipelineEffect
  → FilterPipelineEffect
  → ARPipelineEffect
  → Present (preview EGL window)
  → Encoder surface (while recording; same processed texture, no extra camera)
```

Effect switching mutates rigs only. It does not rebind CameraX.

Camera switch **while recording** is **refused** (`CapturePolicy.Refuse`). Stop recording first.

## Modules

| Module | Role |
|---|---|
| `aurafx-sdk` | Production SDK |
| `aurafx-sample` | Debug harness |
| `aurafx-studio` | Independent Studio app; public SDK API only |

## Complete feature inventory (implementation)

- **Skin / beauty / face shape** — GPU skin-aware blur + localized warp (Step 2).
- **Makeup** — 13 landmark GPU modules, 3 presets (Step 3).
- **Filters** — 10 categories, 39 grades + 32³ LUT (Step 4).
- **Background** — 20 procedural/blur entries; no image PNG catalog.
- **Hair colors** — 12 dye colors on hair-class matte.
- **Hair styles** — 13 rendered (see table).
- **Body** — 6 localized pose warps.
- **Lighting** — 5 subject-masked modes.
- **AR** — 10 procedural tracked looks (Step 6).
- **Photo** — `session.capturePhoto` reads the processed FBO (or identity blit), JPEG, Y-flipped.
- **Video** — `startRecording` / `stopRecording` draws the processed texture to a MediaCodec input surface; optional `AudioRecord` AAC; PTS from camera timestamp deltas + monotonic bump.
- **Editor** — import, still preview, video `VideoView` preview, trim, still crop, rotate (pixel still / orientation hint video), speed, music-as-replacement-track, export. **Unavailable:** video crop, video effect reprocess (would be a second live producer).
- **PerformanceManager** — observed FPS, process ms, GPU query ms when supported, drops, camera startup, effect load, heaps, encoder frame/drop/time, last photo/export durations. Never invents FPS.

## Hairstyle status

Groom recipes live in Kotlin (`HairGroomRecipes`) and as JSON pointers under `aurafx-sdk/src/main/assets/hair/groom/*.json`. Rendering is GPU strand ribbons anchored to MediaPipe hairline/temple/crown/jaw, with hair-channel occlusion and simple azimuth lighting. **Not PNG stickers.**

| Id | REAL RENDERING | HAIR MATTE | HEAD TRACKING | OCCLUSION | LIGHTING RESPONSE | PLACEHOLDER | PHYSICAL QA |
|---|---|---|---|---|---|---|---|
| hair.style.natural | YES (own hair) | YES (dye) | YES | YES (face protect on dye) | YES (luma keep) | NO | PENDING |
| hair.style.bob | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.pixie | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.long_layers | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.bangs | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.ponytail | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.bun | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.braid | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.curtain | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.wolf | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.shag | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.volume | YES | YES | YES | YES | YES | NO | PENDING |
| hair.style.asymmetric | YES | YES | YES | YES | YES | NO | PENDING |

Natural draws **no replacement mesh** (identity silhouette). The other 12 draw tracked ribbons. Without face landmarks the style pass skips (does not fake a sticker). Without a hair matte, dye skips; grooms still draw with weaker occlusion.

**Honesty:** these grooms are real GPU geometry driven by tracking, not photographic strand scans. On-device hairline/ear quality is **PENDING** until a phone is tested. Do not treat unit tests as visual pass.

## Photo pipeline

Camera → Vision → AuraFX GPU graph → processed 2D texture (or OES identity) → dedicated capture FBO → `glReadPixels` once → JPEG.

Not a raw camera still. Not a screenshot of Studio chrome (`SurfaceView` has no Android widgets).

Orientation: front-camera preview mirroring is applied when reading identity OES; processed 2D textures are already in present space. GL Y is flipped for JPEG.

## Video pipeline

Same processed texture is drawn to a **recordable EGL window** on the encoder `Surface` after the preview blit. No per-frame CPU readback.

Audio: camcorder `AudioRecord` when `RECORD_AUDIO` is granted and the recorder initializes; otherwise video-only.

A/V: video PTS from `SurfaceTexture.timestamp` relative to first recorded frame; audio PTS from elapsed `System.nanoTime` with monotonic bump. Clocks can still drift on device — physical A/V QA is PENDING.

Black/corrupt frames: encoder draw failures increment `encoderDroppedFrames`; they are not filled with invented pixels.

Effect switch during record: rig mutation only; camera stays bound.

Restart: `stopRecording` fully releases codec/muxer/audio; `startRecording` creates a new encoder.

## Editor status

See `EditorCapabilities`. Unavailable actions are labeled in Studio, not drawn as silent no-ops that look successful.

## Performance metrics architecture

`PerformanceManager` records only observed events. Snapshot fields that were never measured stay `null` (GPU, camera startup, encode time, photo/export).

## Resource lifecycle

`ResourceLifecycle.owners` lists CameraX, MediaPipe, EGL, GLES, LUTs, meshes, models, encoder surfaces, audio. Session `release()` closes analyzer, camera executors, GL thread, recorder. Effects `onDetach` delete FBOs/shaders/buffers. Repeated start/stop is the existing CameraX unbind/bind path.

Leak magnitudes on a phone: **PENDING**.

## Realism audit (implementation-level, not physical)

| Category | Implementation claim | Physical |
|---|---|---|
| Skin | Texture-preserve mix, no oval-only smooth | PENDING |
| Face shape | Localized warp + oval falloff | PENDING |
| Makeup | Landmark masks, not PNG stickers | PENDING |
| Filter | Grade + LUT, not a flat multiply | PENDING |
| Background | Person-matte composite, not a box | PENDING |
| Hair color | Hair-channel dye, face protected | PENDING |
| Hair style | Tracked ribbons + matte occlusion | PENDING |
| Body | Pose-local warp, face/hair/bg protected | PENDING |
| Lighting | Subject-masked, not a white rectangle | PENDING |
| AR | Landmarks, expression, hair occlude | PENDING |

## Known limitations

- No Tango reference inventory (source missing).
- Groom assets are procedural meshes, not scanned hair grooms.
- Semantic (not instance) segmentation; one primary pose.
- Video crop and live effect reprocess of imported video are unavailable.
- Music replaces the original audio track; it does not duck-mix.
- Photo capture waits for the next presented frame.
- Encoder requires even dimensions; viewport is masked to even.
- Physical FPS, memory growth, and visual quality are unmeasured here.

## Reference inventory status

**NEEDS SOURCE MATERIAL**

## Physical-device QA status

**PHYSICAL DEVICE = PENDING** (`adb` / phone not assumed).
