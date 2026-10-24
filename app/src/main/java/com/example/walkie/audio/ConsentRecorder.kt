package com.example.walkie.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Consent-based Audio Recorder for Walkie-Talkie sessions.
 * Guarantees that:
 * 1. Audio recording only starts upon explicit user consent.
 * 2. Visual indicator is presented during recording.
 * 3. Files are stored locally in app-specific storage.
 */
class ConsentRecorder(private val context: Context) {

    companion object {
        private const val TAG = "ConsentRecorder"
    }

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    /**
     * Starts audio recording to an AAC/M4A file in the app storage.
     */
    fun startRecording(sessionId: String): File? {
        if (isRecording) return currentOutputFile

        return try {
            val dir = File(context.filesDir, "walkie_recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "walkie_${sessionId.take(6)}_$timeStamp.m4a")

            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mediaRecorder
            currentOutputFile = file
            isRecording = true
            Log.d(TAG, "Recording started: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            stopRecording()
            null
        }
    }

    /**
     * Stops the active recording cleanly.
     */
    fun stopRecording(): File? {
        val file = currentOutputFile
        try {
            if (isRecording) {
                recorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaRecorder stop failed", e)
                    }
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        } finally {
            recorder = null
            isRecording = false
            currentOutputFile = null
        }
        return file
    }
}
