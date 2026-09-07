# Reference video implementation report

**Parity: INCOMPLETE**  
**Physical device QA: PENDING**  
**AuraLive / Step 8: not started**

## Inventory findings

The audit brief said six reference videos would be at a `reference/` path. In this workspace:

- Video files found: **0**
- Frame-accurate carousel inventory: **not possible**
- Assumed example totals (70 filters, 87 effects, 35 backgrounds, etc.): **rejected**

Only additional visual input: one user-attached still of host Go Live chrome over an interior scene. That still shows **zero** filter/AR/mask/background carousel tiles.

Therefore no new reference names were taken from video, and **no new catalog entries were invented**.

## Implementation mapping

Existing AuraFX baseline **kept** (not deleted, not downgraded):

| Surface | AuraFX count | Video mapping this pass |
|---|---|---|
| Filters | 51 | not mapped from video files |
| Beauty | 13 listed options | not visible on the still |
| Makeup | 3 presets + listed element styles | not visible on the still |
| AR / effects | 10 original | not mapped from video files |
| Backgrounds | 20 | not mapped from video files |

Existing written-spec names (Rainy Street, Neon Clouds, …) remain implemented from the prior **text** inventory. They are **not** re-certified as the complete video set.

The attached still’s interior look is compatible with existing `rooms.canopy_bed` but is not treated as a newly discovered named tile.

Go Live / Hot / Premium / Party / Audio / 3,000/min are host UI. **Not implemented** in AuraFX.

## Newly implemented features

**None** from this video pass. Inventing GPU looks to “fill” unknown reference counts would violate the exact-name and no-guess rules.

## Remaining incomplete

- Entire six-video inventory
- Filter / AR / mask / background parity vs video
- Visual-behavior copy of each reference tile (tracking, occlusion, particles, intensity)
- Unreadable-tile accounting

## Visual-behavior notes

Not established from video. The still does not show blinking, mouth triggers, or an effects carousel.

## Automated tests

`ReferenceVideoAuditTest`:

- asserts video count 0 ⇒ totals null, status `SOURCE_MISSING`, parity `INCOMPLETE`
- asserts attached still carousel count 0
- asserts baseline 51 / 10 / 20 unique IDs still valid
- asserts production Kotlin sources do not contain the forbidden reference brand

Plus existing `ReferenceFeatureSpecTest` and catalog tests.

## Build results

Recorded in the scorecard after Gradle runs in this session.

## Branding

Production UI, API, IDs, Studio, and these docs use AuraFX names. The reference product brand is not used as an AuraFX feature name.
