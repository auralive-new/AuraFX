package com.aurafx.studio

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.aurafx.sdk.api.AuraFxResult
import com.aurafx.sdk.editor.AuraFxEditor
import com.aurafx.sdk.editor.EditorCapabilities
import com.aurafx.studio.databinding.ActivityEditorBinding
import java.io.File

class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private lateinit var editor: AuraFxEditor

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { import(it) }
    }
    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { import(it) }
    }
    private val musicPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val clip = editor.clip
        if (uri == null || clip == null) return@registerForActivityResult
        if (!EditorCapabilities.available("audio.music")) {
            binding.editorStatus.text = "Music is unavailable"
            return@registerForActivityResult
        }
        editor.applyTimeline { musicUri = uri }
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        editor = AuraFxEditor(this)
        binding.importPhoto.setOnClickListener { photoPicker.launch(arrayOf("image/*")) }
        binding.importVideo.setOnClickListener { videoPicker.launch(arrayOf("video/*")) }
        binding.trimBtn.setOnClickListener {
            val clip = editor.clip ?: return@setOnClickListener
            if (!clip.isVideo) {
                binding.editorStatus.text = "Trim is for video"
                return@setOnClickListener
            }
            editor.applyTimeline {
                trimStartUs = 1_000_000L
                trimEndUs = maxOf(2_000_000L, clip.durationUs)
            }
            refresh()
        }
        binding.cropBtn.setOnClickListener {
            val clip = editor.clip ?: return@setOnClickListener
            if (clip.isVideo && !EditorCapabilities.available("video.crop")) {
                binding.editorStatus.text = "Video crop unavailable"
                return@setOnClickListener
            }
            editor.applyTimeline {
                cropLeft = 0.1f
                cropTop = 0.1f
                cropRight = 0.9f
                cropBottom = 0.9f
            }
            refreshPreview()
        }
        binding.rotateBtn.setOnClickListener {
            editor.applyTimeline { rotateDegrees += 90 }
            refreshPreview()
        }
        binding.speedBtn.setOnClickListener {
            val clip = editor.clip ?: return@setOnClickListener
            if (!clip.isVideo) {
                binding.editorStatus.text = "Speed is for video"
                return@setOnClickListener
            }
            editor.applyTimeline { speed = if (speed < 1.5f) 2f else 1f }
            refresh()
        }
        binding.musicBtn.setOnClickListener {
            if (editor.clip?.isVideo != true) {
                binding.editorStatus.text = "Music replacement is for video export"
                return@setOnClickListener
            }
            musicPicker.launch(arrayOf("audio/*"))
        }
        binding.reprocessBtn.setOnClickListener {
            binding.editorStatus.text = if (EditorCapabilities.available("video.effectReprocess")) {
                "Reprocess"
            } else {
                "Effect reprocess unavailable for video (would be a second live producer). Photos can be sent through session.processFrame by a host."
            }
        }
        binding.exportBtn.setOnClickListener {
            val dest = File(cacheDir, "export_${System.currentTimeMillis()}.${if (editor.clip?.isVideo == true) "mp4" else "jpg"}")
            when (val r = editor.export(dest)) {
                is AuraFxResult.Ok -> {
                    binding.editorStatus.text = "Exported ${r.value.absolutePath}"
                    Toast.makeText(this, "Exported", Toast.LENGTH_SHORT).show()
                }
                is AuraFxResult.Err -> binding.editorStatus.text = r.error.message
            }
        }
    }

    private fun import(uri: Uri) {
        when (val r = editor.import(uri)) {
            is AuraFxResult.Ok -> {
                binding.editorStatus.text = "Imported ${r.value.mime} ${r.value.width}x${r.value.height}"
                refreshPreview()
            }
            is AuraFxResult.Err -> binding.editorStatus.text = r.error.message
        }
    }

    private fun refresh() {
        val clip = editor.clip ?: return
        val t = clip.timeline
        binding.timelineText.text =
            "trim=${t.trimStartUs}-${t.trimEndUs}us speed=${t.speed} rot=${t.rotateDegrees} crop=${t.cropLeft}-${t.cropRight} music=${t.musicUri != null}"
    }

    private fun refreshPreview() {
        refresh()
        val clip = editor.clip ?: return
        if (clip.isVideo) {
            binding.previewImage.isVisible = false
            binding.previewVideo.isVisible = true
            binding.previewVideo.setVideoURI(clip.uri)
            binding.previewVideo.start()
        } else {
            binding.previewVideo.isVisible = false
            binding.previewImage.isVisible = true
            when (val bmp = editor.previewBitmap()) {
                is AuraFxResult.Ok -> binding.previewImage.setImageBitmap(bmp.value)
                is AuraFxResult.Err -> binding.editorStatus.text = bmp.error.message
            }
        }
    }
}
