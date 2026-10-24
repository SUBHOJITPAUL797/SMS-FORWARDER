package com.example.walkie.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Manages Android AudioFocus and Audio Routing for the Walkie-Talkie feature.
 * Automatically switches to MODE_IN_COMMUNICATION and speakerphone during calls,
 * and restores normal audio mode upon hangup.
 */
class AudioFocusManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioFocusManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * Request audio focus and configure VoIP audio mode for Walkie-Talkie.
     */
    fun acquireFocusAndRouteToSpeaker(onFocusLost: () -> Unit = {}) {
        try {
            // Set VoIP communication mode for acoustic echo cancellation and hardware routing
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            routeToSpeaker(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            Log.w(TAG, "Audio focus lost: $focusChange")
                            onFocusLost()
                        }
                    }
                    .build()

                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) onFocusLost()
                    },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            Log.d(TAG, "Audio focus acquired, speakerphone active")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring audio focus", e)
        }
    }

    /**
     * Release audio focus and restore previous audio routing.
     */
    fun releaseFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
            routeToSpeaker(false)
            audioManager.mode = AudioManager.MODE_NORMAL
            Log.d(TAG, "Audio focus released, mode restored to NORMAL")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio focus", e)
        }
    }

    /**
     * Routes audio output to speaker or clears routing.
     */
    fun routeToSpeaker(on: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (on) {
                    val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    }
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = on
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: $on", e)
        }
    }
}
