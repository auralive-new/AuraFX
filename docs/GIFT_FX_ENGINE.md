# GiftFX Engine

Standalone GPU gift overlays inside AuraFX. **Not** AuraLive. Physical-device visual QA is **PENDING**.

Catalog size: **exactly 50** unique real-time GLES3 gifts (original 10 preserved + 40 new). No PNG stickers, no prerecorded MP4/WebM.

Play/replay/stop/reset mutate gift instances only. The CameraX bind is unchanged. Multiple gifts can run at once.

## Public API

```kotlin
session.giftCatalog()
session.playGift("gift.time_freeze")
session.replayGift()
session.stopAllGifts()
session.resetGifts()
```

Studio **Gifts** tray lists all 50 with Play / Replay / Stop on the live preview.

Full catalog: [`GIFT_FX_50_CATALOG.md`](GIFT_FX_50_CATALOG.md)
