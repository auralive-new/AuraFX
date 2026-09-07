# AuraFX reference feature inventory

Standalone SDK inventory against the **supplied specification** (externally reviewed). Videos are not required in this repo.

AuraFX branding only. Do not treat this document as a copy of any third-party product.

**Do not start Step 8 / AuraLive.**

Physical-device visual QA is **PENDING**. Automated tests prove registration and non-placeholder catalog data, not on-device realism.

## Count rule

Only counts established by this repo plus the supplied spec:

| Surface | Count | Notes |
|---|---|---|
| Filters | **51** | Original 39 kept + 12 added |
| Named required filters from spec | **7** | All present |
| Required filter trays from spec | **11** | All present (`trayLabel`) |
| AR effects | **10** | Original AuraFX set kept. Spec does **not** establish a larger verified AR name list |
| Backgrounds | **20** | All original GPU backgrounds kept |
| Beauty options from spec | **13** | All present |
| Makeup makeover presets | **3** | All present |
| Makeup element styles | blush 4, lip looks 3, brow 7, eyeliner 11, lash 6, lens 6 | All present |

Unknown third-party totals are **not** stated here.

---

## FILTERS

Every catalog entry uses the GPU grade / LUT / scene path (`FilterPipelineEffect`). Identity grades are rejected by `FilterCatalog.validate()`.

Required trays from spec (AuraFX labels):

| Spec tray | AuraFX category | `trayLabel()` |
|---|---|---|
| 360° | `Orbit360` | 360° |
| LIVE | `Live` | LIVE |
| GLOW | `Glow` | GLOW |
| PATTERNS | `Patterns` | PATTERNS |
| BLUR | `Blur` | BLUR |
| (original AuraFX name for the sixth tray) | `Signature` | SIGNATURE |
| ANIME | `Anime` | ANIME |
| ANIMAL PRINT | `AnimalPrint` | ANIMAL PRINT |
| NATURE | `Nature` | NATURE |
| SCENERY | `Scenery` | SCENERY |
| ROOMS | `Rooms` | ROOMS |

Additional original AuraFX trays (kept from the 39): Natural, Warm, Cool, Soft, Portrait, Vibe, Mood, Classic, Lut.

### Required named filters from spec

| Display name | AuraFX ID | Category | GPU | Status |
|---|---|---|---|---|
| Rainy Street | `scenery.rainy_street` | Scenery | parametric + scene mode 1 | implemented |
| Neon Clouds | `live.neon_clouds` | Live | parametric + scene mode 2 | implemented |
| Neon Pattern | `patterns.neon_pattern` | Patterns | parametric + scene mode 3 | implemented |
| City Sunset | `scenery.city_sunset` | Scenery | parametric grade | implemented |
| Desert | `nature.desert` | Nature | parametric + scene mode 4 | implemented |
| Canopy Bed Interior | `rooms.canopy_bed` | Rooms | parametric + scene mode 5 | implemented |
| Day Light | `live.day_light` | Live | parametric grade | implemented |

### Original extras added to fill required trays (not labeled as spec tiles)

| Display name | AuraFX ID | Category | GPU | Status |
|---|---|---|---|---|
| Orbit Wrap | `orbit.wrap` | Orbit360 | parametric + scene mode 6 | implemented |
| Dream Blur | `blur.dream` | Blur | parametric bloom/soft | implemented |
| Prime | `signature.prime` | Signature | parametric grade | implemented |
| Cel Shade | `anime.cel` | Anime | parametric + scene mode 7 | implemented |
| Leopard | `animal.leopard` | AnimalPrint | parametric + scene mode 8 | implemented |

### Original 39 (kept)

| Display name | AuraFX ID | Category | Status |
|---|---|---|---|
| True | `natural.true` | Natural | implemented |
| Balanced | `natural.balanced` | Natural | implemented |
| Daylight | `natural.daylight` | Natural | implemented |
| Even | `natural.even` | Natural | implemented |
| Golden | `warm.golden` | Warm | implemented |
| Amber | `warm.amber` | Warm | implemented |
| Honey | `warm.honey` | Warm | implemented |
| Sunset | `warm.sunset` | Warm | implemented |
| Arctic | `cool.arctic` | Cool | implemented |
| Steel | `cool.steel` | Cool | implemented |
| Moonlight | `cool.moonlight` | Cool | implemented |
| Cyan Shadow | `cool.cyan_shadow` | Cool | implemented |
| Pearl | `glow.pearl` | Glow | implemented |
| Halo | `glow.halo` | Glow | implemented |
| Backlight | `glow.backlight` | Glow | implemented |
| Matte | `soft.matte` | Soft | implemented |
| Haze | `soft.haze` | Soft | implemented |
| Pastel | `soft.pastel` | Soft | implemented |
| Studio | `portrait.studio` | Portrait | implemented |
| Rembrandt | `portrait.rembrandt` | Portrait | implemented |
| Editorial | `portrait.editorial` | Portrait | implemented |
| Key Light | `portrait.key` | Portrait | implemented |
| Punch | `vibe.punch` | Vibe | implemented |
| Teal Orange | `vibe.teal_orange` | Vibe | implemented |
| Neon | `vibe.neon` | Vibe | implemented |
| Urban | `vibe.urban` | Vibe | implemented |
| Noir | `mood.noir` | Mood | implemented |
| Dusk | `mood.dusk` | Mood | implemented |
| Fog | `mood.fog` | Mood | implemented |
| Ember | `mood.ember` | Mood | implemented |
| Print | `classic.print` | Classic | implemented |
| Chrome | `classic.chrome` | Classic | implemented |
| Fade | `classic.fade` | Classic | implemented |
| Silver | `classic.silver` | Classic | implemented |
| Film Warm | `lut.film_warm` | Lut | implemented (3D LUT) |
| Film Cool | `lut.film_cool` | Lut | implemented (3D LUT) |
| Cross Process | `lut.cross_process` | Lut | implemented (3D LUT) |
| Contrast S | `lut.contrast_s` | Lut | implemented (3D LUT) |
| Split Tone | `lut.split_tone` | Lut | implemented (3D LUT) |

Intensity 0 skips the GPU pass. `reset` / `clearFilter` restores identity.

---

## AR / EFFECTS / MASKS

The spec requires the existing original ten to remain. It does **not** list additional verified third-party effect names. Extra names were **not invented** as “reference” items.

This ten-item set is **not** claimed to be a complete third-party AR tray.

| Display name | AuraFX ID | Categories | Tracking | Status |
|---|---|---|---|---|
| Desert Sun | `ar.desert_sun` | FaceAccessory, Particle, Environment, Animated | face mesh, head pose, hair occlusion, particles | implemented |
| Purrfect Match | `ar.purrfect_match` | FaceMask, FaceAccessory, EyeEffect, Animated | face mesh, brow expression, iris, hair occlusion | implemented |
| Black Cat | `ar.black_cat` | FaceMask, EyeEffect, Particle, Animated | face mesh, smile, iris, hair occlusion | implemented |
| Party Hop | `ar.party_hop` | FaceAccessory, Particle, Animated, Environment | face mesh, smile bounce, hair occlusion, particles | implemented |
| Pride Paint | `ar.pride_paint` | FacePaint, Particle, Animated | face mesh, person matte, smile | implemented |
| Summer Vibes | `ar.summer_vibes` | FaceAccessory, Environment, Particle | face mesh, head pose, hair occlusion | implemented |
| Red Hero | `ar.red_hero` | FacePaint, EyeEffect, Particle, Animated | face mesh, mouth open, iris, hair occlusion | implemented |
| Blush Pop | `ar.blush_pop` | FacePaint, Particle, Animated | face mesh, smile, person matte | implemented |
| Cupid | `ar.cupid` | FaceAccessory, Particle, Animated, EyeEffect | face mesh, smile, iris, hair occlusion | implemented |
| Moonlit Glow | `ar.moonlit_glow` | FaceAccessory, Environment, Particle, EyeEffect | face mesh, iris, hair occlusion | implemented |

All entries are procedural GPU sprites/particles (`productionRendered`, `procedural` assets). No static sticker catalog.

---

## BACKGROUNDS

All **20** original GPU backgrounds preserved. Person segmentation + GPU composite (`BackgroundPipelineEffect`). Spec did not supply additional verified background display names.

| Display name | AuraFX ID | Status |
|---|---|---|
| Soft Blur | `bg.blur.soft` | implemented |
| Strong Blur | `bg.blur.strong` | implemented |
| Black | `bg.solid.black` | implemented |
| White | `bg.solid.white` | implemented |
| Studio Gray | `bg.gradient.studio_gray` | implemented |
| Studio Warm | `bg.gradient.studio_warm` | implemented |
| Studio Cool | `bg.gradient.studio_cool` | implemented |
| Sunset | `bg.gradient.sunset` | implemented |
| Cyclorama | `bg.env.cyc_white` | implemented |
| Warm Bokeh | `bg.env.bokeh_warm` | implemented |
| Cool Bokeh | `bg.env.bokeh_cool` | implemented |
| Dusk Sky | `bg.env.sky_dusk` | implemented |
| Noon Sky | `bg.env.sky_noon` | implemented |
| Office Window | `bg.env.office_window` | implemented |
| Night City | `bg.env.night_city` | implemented |
| Forest Bokeh | `bg.env.forest_bokeh` | implemented |
| Beach Haze | `bg.env.beach_haze` | implemented |
| Studio Blue | `bg.env.studio_blue` | implemented |
| Spotlight Falloff | `bg.env.spotlight` | implemented |
| Warm Paper | `bg.env.paper_warm` | implemented |

---

## BEAUTY

Basic / Advanced in Studio: skin/tone on Basic, face-shape on Advanced. Intensity sliders + Reset. Values drive `BeautyParameters` / `FaceShapeParameters` into `BeautyPipelineEffect` (GPU skin + warp).

| Spec option | AuraFX field | Pipeline | Status |
|---|---|---|---|
| Fine Smooth | `fineSmooth` | skin GPU | implemented |
| Tooth Whiten | `toothWhiten` | teeth mask GPU | implemented |
| Whiten | `whiten` | skin tone GPU | implemented |
| Ruddy | `ruddy` | skin tone GPU | implemented |
| V Face | `vFace` | face warp | implemented |
| Cheek Thin | `cheekThin` | face warp | implemented |
| Cheek Small | `cheekSmall` | face warp | implemented |
| Cheek Narrow | `cheekNarrow` | face warp | implemented |
| Nose | `nose` | face warp | implemented |
| Eye Enlarge | `eyeEnlarge` | face warp | implemented |
| Eye Distance | `eyeDistance` | signed face warp | implemented |
| Mouth | `mouth` | face warp | implemented |
| Circles | `circles` | under-eye GPU | implemented |

---

## MAKEUP

Tracked landmark makeup (`MakeupPipelineEffect`). No PNG lash strips. Lip looks are GPU (`uLipLook`).

### Makeover

| Spec name | AuraFX ID | Status |
|---|---|---|
| Classic | `MakeupPreset.Classic` | implemented |
| Bright | `MakeupPreset.Bright` | implemented |
| Extravagant | `MakeupPreset.Extravagant` | implemented |

### Elements

| Group | Spec name | AuraFX ID | Status |
|---|---|---|---|
| BLUSH | Soft Touch | `BlushStyle.SoftTouch` | implemented |
| BLUSH | Airbrush | `BlushStyle.Airbrush` | implemented |
| BLUSH | Blush Bomb | `BlushStyle.BlushBomb` | implemented |
| BLUSH | Sun-Kissed | `BlushStyle.SunKissed` | implemented |
| LIP | Glossy Pop | `LipLook.GlossyPop` | implemented |
| LIP | Lacquer | `LipLook.Lacquer` | implemented |
| LIP | Ombre | `LipLook.Ombre` | implemented |
| EYEBROW | Bold Arch | `BrowStyle.BoldArch` | implemented |
| EYEBROW | Natural | `BrowStyle.Natural` | implemented |
| EYEBROW | Feathered | `BrowStyle.Feathered` | implemented |
| EYEBROW | Flat | `BrowStyle.Flat` | implemented |
| EYEBROW | Soft Curve | `BrowStyle.SoftCurve` | implemented |
| EYEBROW | Angled | `BrowStyle.Angled` | implemented |
| EYEBROW | Full | `BrowStyle.Full` | implemented |
| EYELINER | Cat Eye | `EyelinerStyle.CatEye` | implemented |
| EYELINER | Classic | `EyelinerStyle.Classic` | implemented |
| EYELINER | Glam | `EyelinerStyle.Glam` | implemented |
| EYELINER | Smokey | `EyelinerStyle.Smokey` | implemented |
| EYELINER | Goldie | `EyelinerStyle.Goldie` | implemented |
| EYELINER | Flick | `EyelinerStyle.Flick` | implemented |
| EYELINER | None | `EyelinerStyle.None` | implemented |
| EYELINER | Bold | `EyelinerStyle.Bold` | implemented |
| EYELINER | Retro | `EyelinerStyle.Retro` | implemented |
| EYELINER | Graphic | `EyelinerStyle.Graphic` | implemented |
| EYELINER | Winged | `EyelinerStyle.Winged` | implemented |
| EYELASH | Natural Curl | `LashStyle.NaturalCurl` | implemented |
| EYELASH | Soft Volume | `LashStyle.SoftVolume` | implemented |
| EYELASH | Lifted | `LashStyle.Lifted` | implemented |
| EYELASH | Defined | `LashStyle.Defined` | implemented |
| EYELASH | Doll Eyes | `LashStyle.DollEyes` | implemented |
| EYELASH | Full Fan | `LashStyle.FullFan` | implemented |
| LENS | Pure Tone | `LensStyle.PureTone` | implemented |
| LENS | Golden Glint | `LensStyle.GoldenGlint` | implemented |
| LENS | Sapphire Ink | `LensStyle.SapphireInk` | implemented |
| LENS | Warm Glint | `LensStyle.WarmGlint` | implemented |
| LENS | Kiwi Pop | `LensStyle.KiwiPop` | implemented |
| LENS | Silver Mist | `LensStyle.SilverMist` | implemented |

---

## Missing vs this specification

None of the **named** required options are missing from the SDK catalog/API.

## Clarification

- Additional third-party **AR effect names** were not in the supplied list; AuraFX keeps the original 10 and does not invent “reference” AR titles.
- Additional third-party **background names** were not in the supplied list; AuraFX keeps the original 20.
- Visual pass on a physical camera is still required before claiming on-face realism.
