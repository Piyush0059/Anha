package com.example.data.model

object LanguagesList {
    val languages = listOf(
        // Indian Languages
        Language("hi", "Hindi", true, "hi"),
        Language("en-IN", "English (India)", true, "en_IN"),
        Language("bn", "Bengali", true, "bn"),
        Language("mr", "Marathi", true, "mr"),
        Language("te", "Telugu", true, "te"),
        Language("ta", "Tamil", true, "ta"),
        Language("gu", "Gujarati", true, "gu"),
        Language("ur", "Urdu", true, "ur"),
        Language("kn", "Kannada", true, "kn"),
        Language("or", "Odia", true, "or"),
        Language("ml", "Malayalam", true, "ml"),
        Language("pa", "Punjabi", true, "pa"),
        Language("as", "Assamese", true, "as"),
        Language("sa", "Sanskrit", true, "sa"),
        Language("ks", "Kashmiri", true, "ks"),
        Language("kok", "Konkani", true, "kok"),
        Language("mai", "Maithili", true, "mai"),
        Language("brx", "Bodo", true, "brx"),
        Language("doi", "Dogri", true, "doi"),
        Language("mni", "Manipuri", true, "mni"),
        Language("sat", "Santali", true, "sat"),
        Language("sd", "Sindhi", true, "sd"),
        Language("ne", "Nepali", true, "ne"),
        Language("tcy", "Tulu", true, "tcy"),
        Language("bho", "Bhojpuri", true, "bho"),
        Language("mag", "Magahi", true, "mag"),
        Language("hne", "Chhattisgarhi", true, "hne"),
        Language("gbm", "Garhwali", true, "gbm"),
        Language("kfy", "Kumaoni", true, "kfy"),
        Language("raj", "Rajasthani", true, "raj"),
        Language("bgc", "Haryanvi", true, "bgc"),
        Language("bho-bun", "Bundeli", true, "bun"),
        Language("anp", "Angika", true, "anp"),

        // International Languages
        Language("en", "English", false, "en"),
        Language("es", "Spanish", false, "es"),
        Language("fr", "French", false, "fr"),
        Language("de", "German", false, "de"),
        Language("it", "Italian", false, "it"),
        Language("pt", "Portuguese", false, "pt"),
        Language("ru", "Russian", false, "ru"),
        Language("zh-CN", "Chinese (Simplified)", false, "zh_CN"),
        Language("zh-TW", "Chinese (Traditional)", false, "zh_TW"),
        Language("ja", "Japanese", false, "ja"),
        Language("ko", "Korean", false, "ko"),
        Language("ar", "Arabic", false, "ar"),
        Language("tr", "Turkish", false, "tr"),
        Language("nl", "Dutch", false, "nl"),
        Language("pl", "Polish", false, "pl"),
        Language("uk", "Ukrainian", false, "uk"),
        Language("el", "Greek", false, "el"),
        Language("he", "Hebrew", false, "he"),
        Language("th", "Thai", false, "th"),
        Language("vi", "Vietnamese", false, "vi"),
        Language("id", "Indonesian", false, "id"),
        Language("ms", "Malay", false, "ms"),
        Language("fil", "Filipino", false, "fil"),
        Language("sw", "Swahili", false, "sw"),
        Language("fa", "Persian", false, "fa"),
        Language("ro", "Romanian", false, "ro"),
        Language("cs", "Czech", false, "cs"),
        Language("hu", "Hungarian", false, "hu"),
        Language("fi", "Finnish", false, "fi"),
        Language("da", "Danish", false, "da"),
        Language("no", "Norwegian", false, "no"),
        Language("sv", "Swedish", false, "sv"),
        Language("sr", "Serbian", false, "sr"),
        Language("hr", "Croatian", false, "hr"),
        Language("sk", "Slovak", false, "sk"),
        Language("bg", "Bulgarian", false, "bg")
    )

    fun getByCode(code: String): Language? {
        // Match code (fallback to partial match if needed)
        val exact = languages.find { it.code.equals(code, ignoreCase = true) }
        if (exact != null) return exact
        val baseCode = code.split("-").firstOrNull() ?: ""
        return languages.find { it.code.split("-").first().equals(baseCode, ignoreCase = true) }
    }
}
