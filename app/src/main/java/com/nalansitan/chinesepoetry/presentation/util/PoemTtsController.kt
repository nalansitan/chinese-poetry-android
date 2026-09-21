package com.nalansitan.chinesepoetry.presentation.util

import android.content.Context
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nalansitan.chinesepoetry.domain.model.Poem
import java.util.Locale
import java.util.UUID

class PoemTtsController(
    context: Context
) {
    private enum class InitMode {
        DEFAULT_ENGINE,
        FALLBACK_SYSTEM
    }

    private val appContext: Context = context.applicationContext
    private var currentContext: Context = context
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isReleased = false
    private var isInitializing = false
    private var pendingPoem: Poem? = null
    private var initMode = InitMode.DEFAULT_ENGINE

    var isReady by mutableStateOf(false)
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var currentEngineName by mutableStateOf<String?>(null)
        private set

    init {
    }

    fun toggle(context: Context, poem: Poem) {
        currentContext = context
        if (isSpeaking) {
            stop()
        } else if (isReady) {
            speak(poem)
        } else {
            pendingPoem = poem
            initialize()
        }
    }

    fun stop() {
        if (isInitialized) {
            textToSpeech?.stop()
        }
        isSpeaking = false
    }

    fun consumeError() {
        errorMessage = null
    }

    fun shutdown() {
        isReleased = true
        stop()
        if (isInitialized) {
            textToSpeech?.shutdown()
        }
        textToSpeech = null
        isInitialized = false
        isInitializing = false
        isReady = false
        pendingPoem = null
    }

    private fun speak(poem: Poem) {
        val tts = textToSpeech
        if (tts == null || !isReady) {
            errorMessage = "系统朗读尚未准备好"
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        val result = tts.speak(
            buildSpeechText(poem),
            TextToSpeech.QUEUE_FLUSH,
            null,
            utteranceId
        )

        if (result != TextToSpeech.SUCCESS) {
            errorMessage = "朗读启动失败"
        }
    }

    private fun initialize() {
        if (isReleased || isInitializing || isInitialized) return

        isInitializing = true
        isReady = false
        if (isInitialized) {
            textToSpeech?.shutdown()
        }
        textToSpeech = null

        var engineRef: TextToSpeech? = null
        val defaultEngine = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.TTS_DEFAULT_SYNTH
        )?.takeIf { it.isNotBlank() }
        currentEngineName = defaultEngine

        val onInit: (Int) -> Unit = onInit@{ status ->
            if (isReleased) return@onInit

            isInitializing = false

            if (status != TextToSpeech.SUCCESS) {
                if (initMode == InitMode.DEFAULT_ENGINE) {
                    textToSpeech?.shutdown()
                    textToSpeech = null
                    initMode = InitMode.FALLBACK_SYSTEM
                    initialize()
                } else {
                    errorMessage = "系统朗读初始化失败"
                }
                return@onInit
            }

            val currentEngine = engineRef ?: return@onInit
            val languageResult = currentEngine.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                errorMessage = "当前设备不支持中文朗读"
                return@onInit
            }

            currentEngine.setSpeechRate(0.9f)
            currentEngine.setPitch(1.0f)
            currentEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    errorMessage = "朗读失败，请稍后重试"
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                    errorMessage = "朗读失败，请稍后重试"
                }
            })

            textToSpeech = currentEngine
            isInitialized = true
            isReady = true

            pendingPoem?.let {
                pendingPoem = null
                speak(it)
            }
        }

        val engine = when {
            initMode == InitMode.DEFAULT_ENGINE && defaultEngine != null -> {
                TextToSpeech(currentContext, onInit, defaultEngine)
            }
            else -> {
                TextToSpeech(currentContext, onInit)
            }
        }
        engineRef = engine
        textToSpeech = engine
    }

    private fun buildSpeechText(poem: Poem): String {
        return buildString {
            append(poem.title)
            append("。")
            append(poem.authorName)
            append("。")
            poem.content.forEach { line ->
                append(line)
                append("。")
            }
        }
    }
}
