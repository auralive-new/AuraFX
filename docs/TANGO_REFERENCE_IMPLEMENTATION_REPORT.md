# Tango reference implementation report

**TANGO COMPLETE = NO**

**Step 8 / AuraLive = not started**

## Why catalogs were not expanded this run

The count gate is **AuraFX ≥ verified Tango**. Verified Tango totals are **UNKNOWN** because no recordings were in the workspace.

Expanding filters to 50, AR to 87, or backgrounds to 30 using the task’s example numbers would:

- invent Tango titles and carousel positions
- count placeholders as features
- claim subset inclusion that cannot be proven

That is forbidden. Original AuraFX GPU catalogs (39 filters, 10 AR, 20 backgrounds) were **left unchanged**.

Cited filter examples (Rainy Street, Neon Clouds, Neon Pattern, City Sunset, Desert, Canopy Bed Interior, Day Light) were **not** implemented under those names: category, motion, occlusion, and whether they are grades vs environments cannot be read from frames that are not here.

Cited lip looks (Glossy Pop, Lacquer, Ombre) were **not** added as fake presets.

## Newly implemented features

None for Tango parity. Added only:

- `docs/TANGO_REFERENCE_INVENTORY.md`
- `docs/TANGO_REFERENCE_IMPLEMENTATION_REPORT.md`
- `TangoReferenceGate` + unit tests that fail if `TANGO_COMPLETE` is flipped while source is still missing

## Features still incomplete (vs Tango)

Everything that requires a Tango tile name + behavior from recordings, including:

- All Tango filter tray items (count unknown)
- All Tango AR / effects / masks (count unknown)
- All Tango backgrounds (count unknown)
- Cited lip looks Glossy Pop / Lacquer / Ombre (names cited, behavior unread)

## Features requiring source material

See inventory §1. Without those files, **no Tango row can be PASS**.

## Exact verified names

**Zero** Tango on-screen names were verified from source in this run.

Cited-but-unverified strings are listed in the inventory; they are not a complete count.

## Automated tests

`:aurafx-sdk:testDebugUnitTest` includes `TangoReferenceGateTest`.

## Build

SDK unit tests + assemble after this change.

## Physical device QA

**PENDING**
