# Reference video inventory

**Status: SOURCE_MISSING — INCOMPLETE**

Step 7 is closed. AuraLive / Step 8 was **not** started.

This document records what could be established from files actually present in this workspace. Invented carousel counts (for example 50, 70, 87, 30, 35) are **not** used.

## Source files

Expected: six reference videos under `reference/` (any subdirectory).

| Check | Result |
|---|---|
| `reference/` video files (mp4/mov/webm/mkv/m4v) | **0** |
| Frame-by-frame video review | **not performed** (no files) |
| User-attached still | **1** JPEG (host Go Live preview) |

Without the six recordings, reference **totals are not established**. Distinct selectable options cannot be counted from carousels that were not inspected.

`ReferenceVideoAudit.videoCount() == 0`  
`ReferenceVideoAudit.sourceStatus() == SOURCE_MISSING`

## Count gate (from this workspace)

| Metric | Value |
|---|---|
| REFERENCE FILTERS | **not established** (no video files) |
| AURAFX FILTERS | **51** |
| MISSING FILTERS | **not established** |
| REFERENCE AR/EFFECTS | **not established** |
| AURAFX AR/EFFECTS | **10** |
| MISSING AR/EFFECTS | **not established** |
| REFERENCE MASKS | **not established** |
| AURAFX MASKS | **10** AR looks cover mask/paint/accessory categories; no separate mask catalog |
| MISSING MASKS | **not established** |
| REFERENCE BACKGROUNDS | **not established** |
| AURAFX BACKGROUNDS | **20** |
| MISSING BACKGROUNDS | **not established** |
| REFERENCE ITEMS MATCHED | **0** from video files (none available) |
| REFERENCE ITEMS UNREADABLE | **not established** from video |
| REFERENCE ITEMS STILL INCOMPLETE | **entire video inventory** |

AuraFX count ≥ reference count **cannot** be proven until the six videos are in the tree.

## FILTERS

| Field | Value |
|---|---|
| reference total | not established |
| AuraFX total | 51 |
| matched (from video) | 0 |
| missing (from video) | not established |
| unreadable | not established |
| newly implemented this pass | **0** (no video-derived names) |

Previous written inventory (not re-verified from video files) remains in [`REFERENCE_FEATURE_INVENTORY.md`](REFERENCE_FEATURE_INVENTORY.md). Those names are **not** re-certified as a complete video carousel.

## AR / EFFECTS

| Field | Value |
|---|---|
| reference total | not established |
| AuraFX total | 10 original looks |
| matched (from video) | 0 |
| missing | not established |
| unreadable | not established |
| newly implemented this pass | **0** |

The ten AuraFX looks stay as **original** features. They are not labeled as a complete reference tray.

## MASKS

No separate mask catalog exists. Mask/paint behavior is part of AR looks. Video mask tray **not inspected**.

| Field | Value |
|---|---|
| reference total | not established |
| AuraFX total | 10 AR entries (includes FaceMask / FacePaint categories) |
| matched / missing / unreadable | not established |
| newly implemented | **0** |

## BACKGROUNDS

| Field | Value |
|---|---|
| reference total | not established |
| AuraFX total | 20 |
| matched (from video) | 0 |
| missing | not established |
| unreadable | not established |
| newly implemented this pass | **0** |

## Attached still (not a video)

Source: user-attached JPEG of a portrait **Go Live** host preview.

| Readable label | Timestamp | Carousel item? | SDK action |
|---|---|---|---|
| Go Live | n/a (still) | no | out of scope (Step 8 / host) |
| Live Description | n/a | no | out of scope |
| Live singing | n/a | no | out of scope |
| Hot | n/a | no | out of scope |
| Premium | n/a | no | out of scope |
| Party | n/a | no | out of scope |
| Audio | n/a | no | out of scope |
| 3,000/min | n/a | no | out of scope |

**Carousel items visible on this still: 0.**

Visual (not a named tray tile): interior with arched canopy, circular ceiling light, night city through curtains, pink/purple wash. That is consistent with the existing AuraFX filter `rooms.canopy_bed` (Canopy Bed Interior) plus glow, but this still **does not** show a labeled carousel cell, so it is **not** counted as extra reference titles.

No AR mesh, lashes, beauty sliders, or background picker cells are readable on this still.

## Categories / subcategories / presets from video

**UNREADABLE — FRAME REVIEW REQUIRED** (video files absent). Do not invent tray names from this pass.

## What to do when videos are added

1. Place the six recordings under `reference/`.
2. Re-run this audit: seek every tray and carousel, record filename + timestamp per tile.
3. Readable names map 1:1 into AuraFX GPU catalogs.
4. Unreadable tiles: count them, mark `UNREADABLE — FRAME REVIEW REQUIRED`, implement with original AuraFX names only.
5. Raise AuraFX counts until reference ⊆ AuraFX with real GPU paths.
