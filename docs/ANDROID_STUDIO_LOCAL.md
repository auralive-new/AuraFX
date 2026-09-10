# Open this project in Android Studio

This is the existing AuraFX Gradle project. Do not create a new project. Do not import AuraLive.

## Folder to open

Open **this repository root** in Android Studio (File → Open). It is the directory that contains `settings.gradle.kts`, `gradlew`, `aurafx-sdk`, and `aurafx-studio`.

In the current workspace that path is:

```
/workspace
```

After a local clone, it is whatever folder you cloned into (still the folder with `settings.gradle.kts`).

## Run on a device

1. Install **Android Studio** with **JDK 17** and **Android SDK 35**.
2. Open `/workspace` (or your clone of that root). Let Gradle sync.
3. Select the **aurafx-sample** or **aurafx-studio** run configuration.
4. Connect a **physical** phone (GLES 3, camera). Emulators are a poor match for this GPU + MediaPipe pipeline.
5. Run. Grant **CAMERA**. The Studio screen auto-starts the **front camera** and shows a draggable Beauty sheet above the system navigation bar.
6. Long-press the FPS chip (debug builds) to open the QA panel. From there you can open the legacy diagnostic harness if needed.

`local.properties` is machine-specific and gitignored. Android Studio writes `sdk.dir` when you open the project.

## Already in the tree (do not replace)

- `:aurafx-sdk` library, including GiftFX (50 gifts) and MediaPipe models:
  - `aurafx-sdk/src/main/assets/models/face_landmarker.task`
  - `aurafx-sdk/src/main/assets/models/selfie_multiclass_256x256.tflite`
  - `aurafx-sdk/src/main/assets/models/pose_landmarker_lite.task`
- `:aurafx-studio` app (live camera + Gifts tray)
- `:aurafx-sample` debug harness (not required for Studio testing)
- Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`)

AuraLive / Step 8 is not part of this project.
