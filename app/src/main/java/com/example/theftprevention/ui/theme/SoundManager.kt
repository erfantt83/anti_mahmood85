package com.example.theftprevention.ui.theme

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.preference.PreferenceManager
import com.example.theftprevention.R

object SoundManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playAlarm(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val selectedSound = prefs.getString("selected_alarm_sound", "alarm1")

        val soundResId = when (selectedSound) {
            "alarm2" -> R.raw.alarm_sound2
            "alarm3" -> R.raw.alarm_sound3
            else -> R.raw.alarm_sound
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, soundResId)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } else if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun stopAlarm(context: Context) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
}