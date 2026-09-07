# Step 1B status

## Physical device

`adb devices` in this environment returned an empty list. USB enumeration had no Android phones.

**PHYSICAL DEVICE VERIFICATION = PENDING**

No FPS, process time, drop counts, startup time, GPU time, or memory-leak conclusions are reported from a phone because none were measured on a phone.

## What was done without a device

Core hardening (still a single CameraX → SurfaceTexture → GPU path):

- `processFrame` is rejected while the live camera is wanted or bound (`LiveIngressPolicy`). It cannot run as a second live camera.
- CameraX bind generation ignores stale start callbacks; every bind starts with `unbindAll`.
- ImageAnalysis is still not added.
- SurfaceTexture buffer size is taken from the CameraX `SurfaceRequest` resolution.
- Lifecycle will not rebind until `attachPreview` has a valid host Surface.
- `onFirstFrame` resets on each camera start.
- Tag `AuraFX` logs for initialize, EGL, CameraX bind/unbind, SurfaceRequest, first OES frame, stop, release, and errors (not per-frame).
- Snapshot includes `lastIngress` (`CAMERA_OES` vs `PROCESS_FRAME`) so a later device run can prove which producer presented.

## Builds / tests (this environment)

See the latest Gradle run in the agent log. Device-dependent checks remain PENDING.
