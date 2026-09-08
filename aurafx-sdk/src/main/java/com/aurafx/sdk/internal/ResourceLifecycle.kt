package com.aurafx.sdk.internal

/**
 * Code-level resource ownership for start/stop/restart and create/release/create.
 * Physical leak numbers remain PENDING without a device.
 */
object ResourceLifecycle {
    val owners: List<String> = listOf(
        "CameraX ProcessCameraProvider unbindAll + executor shutdown on session release",
        "MediaPipeSceneAnalyzer.close on session release; analyzer nulled",
        "EGL context, pbuffer, window surface destroyed on GL teardown",
        "OES + CPU textures deleted; SurfaceTexture released",
        "EffectManager.detachAll releases shaders, FBOs, VAOs, LUTs, meshes",
        "Segmentation upload texture deleted on detach",
        "Hair groom VBO/IBO deleted on HairPipelineEffect.detach",
        "Encoder input Surface + EGL window + MediaCodec + MediaMuxer on stop/release",
        "AudioRecord + AAC encoder + audio thread joined on recorder stop",
        "Photo JPEG writer uses a daemon executor; shutdown on GL teardown",
        "No second CameraX bind; effect switching only mutates rigs",
        "GiftFX FBO/shaders released on GiftPipelineEffect.detach; play/stop does not rebind camera",
    )
}
