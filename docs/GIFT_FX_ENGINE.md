# GiftFX Engine (experimental, task 1/2)

Standalone GPU gift overlays inside AuraFX. **Not** AuraLive. Physical-device visual QA is **PENDING**.

Play/replay/stop/reset mutate gift instances only. The CameraX bind is unchanged.

## Public API

```kotlin
session.giftCatalog()
session.playGift("gift.time_freeze")
session.replayGift()
session.stopAllGifts()
session.resetGifts()
```

## Sample IDs

`gift.time_freeze` · `gift.portal_door` · `gift.meteor_creature` · `gift.hologram_clone` · `gift.magic_paint` · `gift.giant_shadow` · `gift.mini_world` · `gift.gravity_flip` · `gift.mirror_dimension` · `gift.ink_universe`
