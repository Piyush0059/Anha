package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d("AnhaTTS", "TextToSpeech successfully initialized.")
        } else {
            Log.e("AnhaTTS", "TextToSpeech initialization failed.")
        }
    }

    fun speak(text: String, languageCode: String, speed: Float = 1.0f, gender: String = "Female") {
        if (!isInitialized || tts == null) {
            Log.w("AnhaTTS", "TTS is not ready yet.")
            return
        }

        tts?.stop()

        val cleanCode = languageCode.lowercase().replace("_", "-").trim()
        val locale = when {
            cleanCode == "hi" -> Locale("hi", "IN")
            cleanCode == "en-in" -> Locale("en", "IN")
            cleanCode == "en" -> Locale.US
            cleanCode == "bn" -> Locale("bn", "IN")
            cleanCode == "mr" -> Locale("mr", "IN")
            cleanCode == "te" -> Locale("te", "IN")
            cleanCode == "ta" -> Locale("ta", "IN")
            cleanCode == "gu" -> Locale("gu", "IN")
            cleanCode == "ur" -> Locale("ur", "IN")
            cleanCode == "kn" -> Locale("kn", "IN")
            cleanCode == "or" -> Locale("or", "IN")
            cleanCode == "ml" -> Locale("ml", "IN")
            cleanCode == "pa" -> Locale("pa", "IN")
            cleanCode == "as" -> Locale("as", "IN")
            cleanCode == "sa" -> Locale("sa", "IN")
            cleanCode == "ne" -> Locale("ne", "NP")
            cleanCode == "es" -> Locale("es", "ES")
            cleanCode == "fr" -> Locale.FRANCE
            cleanCode == "de" -> Locale.GERMANY
            cleanCode == "it" -> Locale.ITALY
            cleanCode == "pt" -> Locale("pt", "PT")
            cleanCode in listOf("zh-cn", "zh") -> Locale.SIMPLIFIED_CHINESE
            cleanCode == "zh-tw" -> Locale.TRADITIONAL_CHINESE
            cleanCode == "ja" -> Locale.JAPAN
            cleanCode == "ko" -> Locale.KOREA
            cleanCode == "ru" -> Locale("ru", "RU")
            cleanCode == "ar" -> Locale("ar")
            cleanCode == "tr" -> Locale("tr", "TR")
            cleanCode == "nl" -> Locale("nl", "NL")
            cleanCode == "pl" -> Locale("pl", "PL")
            cleanCode == "uk" -> Locale("uk", "UA")
            cleanCode == "el" -> Locale("el", "GR")
            cleanCode == "he" -> Locale("he", "IL")
            cleanCode == "th" -> Locale("th", "TH")
            cleanCode == "vi" -> Locale("vi", "VN")
            cleanCode == "id" -> Locale("id", "ID")
            cleanCode == "ms" -> Locale("ms", "MY")
            cleanCode == "fil" -> Locale("fil", "PH")
            cleanCode == "sw" -> Locale("sw")
            cleanCode == "fa" -> Locale("fa", "IR")
            cleanCode == "ro" -> Locale("ro", "RO")
            cleanCode == "cs" -> Locale("cs", "CZ")
            cleanCode == "hu" -> Locale("hu", "HU")
            cleanCode == "fi" -> Locale("fi", "FI")
            cleanCode == "da" -> Locale("da", "DK")
            cleanCode == "no" -> Locale("no", "NO")
            cleanCode == "sv" -> Locale("sv", "SE")
            cleanCode.contains("-") -> {
                val parts = cleanCode.split("-")
                Locale(parts[0], parts.getOrElse(1) { "" }.uppercase())
            }
            else -> Locale(cleanCode)
        }

        try {
            tts?.language = locale
            tts?.setSpeechRate(speed)
            
            // To simulate gender or different accent modes where engine supports it, 
            // we let TTS handle primary speaker selection. Older APIs fallback standardly.
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AnhaTTSId")
        } catch (e: Exception) {
            Log.e("AnhaTTS", "Error setting up TTS locale or speaking: ${e.message}")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
