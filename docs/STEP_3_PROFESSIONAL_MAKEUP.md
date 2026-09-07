# AuraFX Step 3 — Professional Makeup

Standalone SDK. No AuraLive. Filters run **after** this makeup pass (Step 4).

## Architecture

```
CameraX Preview + ImageAnalysis (one bind)
  → MediaPipe Face Landmarker
  → MakeupPipelineEffect  (resolve OES → region masks + stamps + liner/lash → GPU composite)
  → BeautyPipelineEffect  (skin / tone / face warp; copies makeup 2D if present)
  → FilterPipelineEffect  (Step 4 color / LUT; skipped at intensity 0)
  → Host Surface
```

`MakeupPipelineEffect` is registered **before** beauty so the requested order is Makeup → Beauty/Face.

## API

```kotlin
session.applyMakeupPreset(MakeupPreset.Classic) // Bright, Extravagant
session.makeup {
    foundation { enabled = true; intensity = 0.4f; coverage = 0.5f }
    blush { enabled = true; style = BlushStyle.SoftTouch; intensity = 0.35f }
    eyeliner { style = EyelinerStyle.CatEye; intensity = 0.7f }
    lipstick { color.set(0.7f, 0.15f, 0.2f); intensity = 0.6f }
}
session.resetMakeup()
```

Presets set many modules at once and remain editable afterward.

## Categories

| Module | Mechanism |
|---|---|
| Foundation | Skin-mask overlay/soft mix, pores via luminance detail |
| Concealer | Under-eye polygon + gaussian stamp |
| Blush | Cheek stamps: Soft Touch, Airbrush, Blush Bomb, Sun-Kissed |
| Contour | Jaw, cheek, nose ala, forehead stamps (multiply) |
| Highlight | Cheekbone, nose bridge, forehead, cupid's bow |
| Eyebrow | Brow polygon + 7 styles (width via `browWidth`) |
| Eyeshadow | Lid mask, lid/crease colors, iris excluded |
| Eyeliner | Landmark polylines, 11 styles including None; tapered width + optional wing |
| Lashes | Procedural quadratic strands (6 styles), no PNG strips |
| Lipstick | Outer−inner lip mask, overlay, texture from luminance |
| Lip liner | Lip-mask edge |
| Lip gloss | Specular from luminance on lips |
| Lens | Iris ring (pupil hole), catchlight preserved, 6 styles |

## Tracking / masks

Landmarks are the same MediaPipe mesh as Step 2 (EMA). Masks rasterize at 160². Blush/contour/highlight are oriented gaussians in UV, not boxes.

## GPU

Shaders compiled once. FBOs and mask textures reused. No per-frame Bitmap for cosmetics (ByteBuffers reused).

## Limitations

- Physical look is **unverified** (no phone).
- Brows are filled/feathered via the brow polygon, not individual 3D hairs.
- Lashes are procedural curves on the lid, not detected real cilia.
- Closed eyelids shrink the lash-line polyline (tracking-dependent).
- No neural hair matting; foundation already excludes oval exterior.

## Physical QA

PENDING
