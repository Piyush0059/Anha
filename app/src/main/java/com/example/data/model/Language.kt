package com.example.data.model

data class Language(
    val code: String,
    val name: String,
    val isIndian: Boolean,
    val ttsLocaleCode: String // Used to match Android TextToSpeech locales if available
)
