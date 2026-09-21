package com.nalansitan.chinesepoetry.presentation.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech

object TtsSettingsNavigator {

    fun open(context: Context) {
        val intents = listOf(
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        val packageManager = context.packageManager
        val targetIntent = intents.firstOrNull { intent ->
            intent.resolveActivity(packageManager) != null
        } ?: Intent(Settings.ACTION_SETTINGS)

        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(targetIntent)
    }
}
