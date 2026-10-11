package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.data.api.GenerationConfig
import com.example.data.api.GeminiClient
import com.example.data.api.TranslationResult
import com.example.data.db.AppDao
import com.example.data.db.HistoryEntity
import com.example.data.db.FeedbackEntity
import com.example.data.db.DownloadPackEntity
import com.example.data.api.AppUpdateResponse
import com.example.data.api.UpdateClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class TranslationRepository(private val appDao: AppDao, val context: Context) {

    val historyFlow: Flow<List<HistoryEntity>> = appDao.getAllHistory()
    val favoritesFlow: Flow<List<HistoryEntity>> = appDao.getFavorites()
    val downloadedPacksFlow: Flow<List<DownloadPackEntity>> = appDao.getDownloadedPacks()
    val feedbackFlow: Flow<List<FeedbackEntity>> = appDao.getAllFeedback()

    companion object {
        private val lastRequestTime = AtomicLong(0L)
        private const val THROTTLE_MS = 2500L // Min 2.5s gap between Gemini API calls to optimize free-tiered usage
        
        // Concurrent map to prevent duplicate requests with identical parameters from running concurrently
        private val activeRequests = ConcurrentHashMap<String, Deferred<TranslationResult>>()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun enforceThrottle() {
        val now = System.currentTimeMillis()
        val last = lastRequestTime.get()
        val diff = now - last
        if (diff < THROTTLE_MS) {
            val wait = THROTTLE_MS - diff
            kotlinx.coroutines.delay(wait)
        }
        lastRequestTime.set(System.currentTimeMillis())
    }

    private fun isHttpRateLimitException(e: Exception): Boolean {
        if (e is retrofit2.HttpException) {
            return e.code() == 429
        }
        val message = e.message?.lowercase() ?: ""
        return message.contains("429") || message.contains("too many requests") || message.contains("quota exceeded")
    }

    private fun isNetworkFailureException(e: Exception): Boolean {
        return e is java.io.IOException || e is java.net.ConnectException || e is java.net.UnknownHostException || e is java.net.SocketTimeoutException
    }

    var lastErrorText: String = "None"
        private set

    fun getLastError(): String = lastErrorText

    suspend fun getCacheCount(): Int = withContext(Dispatchers.IO) {
        try {
            appDao.getHistoryCount()
        } catch (e: Exception) {
            0
        }
    }

    fun isNetworkConnected(): Boolean {
        return isNetworkAvailable()
    }

    private fun toFriendlyErrorMessage(e: Exception): String {
        return when {
            e is IllegalArgumentException -> e.message ?: "Invalid translation input."
            e is IllegalStateException && e.message?.contains("API Key") == true -> e.message ?: "Invalid configured API secret."
            e.message?.contains("Offline Mode") == true -> "No internet connection. Please reconnect and try again."
            isHttpRateLimitException(e) -> "The translation engine is temporarily busy (Quota scale limits). Please wait a few seconds and try again."
            isNetworkFailureException(e) -> "No internet connection. Please reconnect and check your network settings."
            else -> "We encountered an unexpected issue with the AI engine. Please try again."
        }
    }

    fun searchHistory(query: String): Flow<List<HistoryEntity>> {
        return if (query.isBlank()) appDao.getAllHistory() else appDao.searchHistory(query)
    }

    suspend fun translate(
        text: String,
        sourceLangCode: String, // "auto" or language code
        sourceLangName: String,
        targetLangCode: String,
        targetLangName: String,
        category: String // "Casual", "Formal", "Business", "Academic", "Travel"
    ): TranslationResult = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) {
            throw IllegalArgumentException("Translation text cannot be empty.")
        }

        // Safeguard very large inputs for free-tier Gemini limits
        if (cleanText.length > 3000) {
            throw IllegalArgumentException("The input text size is too large (maximum 3000 characters). Please simplify your text.")
        }

        // 1. API KEY VALIDATION
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.trim().isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.length < 10) {
            throw IllegalStateException("Your Gemini API Key is missing or invalid. Please configure your key in Google AI Studio secrets panel.")
        }

        // 2. CACHE LOOKUP (Optimizes free tier quota, offline-safe!)
        val cached = appDao.findCachedTranslation(
            sourceText = cleanText,
            sourceLangCode = sourceLangCode,
            targetLangCode = targetLangCode,
            category = category
        )
        if (cached != null) {
            return@withContext TranslationResult(
                detected_language_code = cached.sourceLangCode,
                detected_language_name = cached.sourceLangName,
                confidence_score = 1.0,
                translated_text = cached.translatedText
            )
        }

        // 3. OFFLINE DETECTION & ERROR HANDLING
        if (!isNetworkAvailable()) {
            throw Exception("Offline Mode: No active internet connection found, and this input combination wasn't translated before.")
        }

        // 4. DUPLICATE REQUEST PREVENTION & QUEUE MANAGEMENT (Single Flight Pattern)
        val requestKey = "${cleanText.lowercase()}_${sourceLangCode.lowercase()}_${targetLangCode.lowercase()}_${category.lowercase()}"
        
        coroutineScope {
            val deferred = activeRequests.getOrPut(requestKey) {
                async(Dispatchers.IO) {
                    val systemPrompt = """
                        You are Anha.ai, a modern, premium, enterprise-grade translation engine.
                        Your purpose is to break language barriers instantly, naturally, and securely.
                        Translate the input text exactly, keeping the nuance, tone, and cultural context.
                        You MUST respond with a single, valid JSON block ONLY. Do not write markdown blocks or backticks like ```json ...```. Output Raw JSON.
                        Response JSON Schema:
                        {
                          "detected_language_code": "ISO 639-1 language code (e.g. 'en', 'hi', 'fr')",
                          "detected_language_name": "Full English name of language (e.g. 'English', 'Hindi', 'French')",
                          "confidence_score": 0.95,
                          "translated_text": "The translated text string"
                        }
                        Do NOT include explanations or notes. Always return a perfectly formed JSON.
                        Use the translation category style: '$category'.
                        - Casual: everyday, friendly, colloquial language.
                        - Formal: polite, grammatically perfect, respectful.
                        - Business: professional, executive, corporate terms.
                        - Academic: scholarly, exact, formal prose.
                        - Travel: simple, practical, clear.
                    """.trimIndent()

                    val prompt = if (sourceLangCode.lowercase() == "auto") {
                        "Translate the following text to '$targetLangName'. Text: $cleanText"
                    } else {
                        "Translate the text from '$sourceLangName' (original language code: $sourceLangCode) into '$targetLangName'. Text: $cleanText"
                    }

                    val request = GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(Part(text = prompt)))
                        ),
                        generationConfig = GenerationConfig(
                            responseMimeType = "application/json",
                            temperature = 0.3
                        ),
                        systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                    )

                    // 5. RETRY MECHANISM WITH EXPONENTIAL BACKOFF & REQUEST THROTTLING
                    var attempt = 0
                    val maxAttempts = 3
                    var delayMs = 2000L
                    var responseText: String? = null
                    var latestException: Exception? = null

                    while (attempt < maxAttempts) {
                        try {
                            enforceThrottle()

                            val response = GeminiClient.apiService.generateContent(
                                model = "gemini-3.5-flash",
                                apiKey = apiKey,
                                request = request
                            )
                            responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            if (responseText != null) {
                                break // Success!
                            } else {
                                throw IllegalStateException("Empty response from AI engine")
                            }
                        } catch (e: Exception) {
                            latestException = e
                            attempt++
                            if (attempt >= maxAttempts) {
                                break
                            }

                            if (isHttpRateLimitException(e)) {
                                // For scale limits (429), back off longer
                                delayMs = 4000L * attempt
                                kotlinx.coroutines.delay(delayMs)
                            } else if (isNetworkFailureException(e)) {
                                kotlinx.coroutines.delay(delayMs)
                                delayMs *= 2
                            } else {
                                // Non-transient errors (like JSON format issues or auth failures) shouldn't be retried
                                throw e
                            }
                        }
                    }

                    val finalResponseText = responseText ?: throw latestException ?: IllegalStateException("Translation engine is temporarily busy. Please wait a moment.")

                    val cleanedText = finalResponseText.trim().removeSurrounding("```json", "```").trim()
                    val result = GeminiClient.translationResultAdapter.fromJson(cleanedText)
                        ?: throw IllegalStateException("Failed to parse translation JSON: $finalResponseText")

                    val resolvedSourceLangCode = if (sourceLangCode.lowercase() == "auto") result.detected_language_code else sourceLangCode
                    val resolvedSourceLangName = if (sourceLangCode.lowercase() == "auto") result.detected_language_name else sourceLangName

                    // 6. PERSIST SUCCESSFUL TRANSLATION TO HISTORY (Saves to Cache)
                    val historyEntity = HistoryEntity(
                        sourceText = cleanText,
                        translatedText = result.translated_text,
                        sourceLangCode = resolvedSourceLangCode,
                        sourceLangName = resolvedSourceLangName,
                        targetLangCode = targetLangCode,
                        targetLangName = targetLangName,
                        category = category,
                        isFavorite = false,
                        timestamp = System.currentTimeMillis()
                    )
                    appDao.insertHistory(historyEntity)

                    result
                }
            }

            try {
                deferred.await()
            } catch (e: Exception) {
                val friendlyMessage = toFriendlyErrorMessage(e)
                lastErrorText = "${e.javaClass.simpleName}: ${e.message}"
                try {
                    val failureDetails = "Source Lang: $sourceLangName, Target Lang: $targetLangName, Style: $category, Input: '${text.take(60)}...', Error: ${e.message ?: "Unknown issue"}"
                    insertDiagnostic("TRANSLATION_FAILURE", failureDetails)
                } catch (idx: Exception) {
                    idx.printStackTrace()
                }
                throw Exception(friendlyMessage)
            } finally {
                activeRequests.remove(requestKey)
            }
        }
    }

    suspend fun toggleFavorite(historyItem: HistoryEntity) {
        appDao.updateHistory(historyItem.copy(isFavorite = !historyItem.isFavorite))
    }

    suspend fun deleteHistory(id: Long) {
        appDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        appDao.clearHistory()
    }

    suspend fun submitFeedback(
        comment: String,
        languages: String,
        contextText: String,
        appVersion: String,
        androidVersion: String,
        deviceModel: String,
        deviceManufacturer: String,
        errorDetails: String
    ) {
        val feedback = FeedbackEntity(
            comment = comment,
            languages = languages,
            contextText = contextText,
            timestamp = System.currentTimeMillis(),
            appVersion = appVersion,
            androidVersion = androidVersion,
            deviceModel = deviceModel,
            deviceManufacturer = deviceManufacturer,
            errorDetails = errorDetails
        )
        appDao.insertFeedback(feedback)
        
        // Securely log user feedback as a local diagnostic telemetry event
        try {
            insertDiagnostic(
                type = "USER_FEEDBACK",
                details = "User feedback remarks: '$comment'. Languages involved: $languages. Reference Context: '$contextText'. Errors captured: $errorDetails"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun downloadPack(langCode: String, langName: String) {
        appDao.insertDownloadedPack(DownloadPackEntity(langCode = langCode, langName = langName))
    }

    suspend fun removePack(langCode: String) {
        appDao.deleteDownloadedPack(langCode)
    }

    fun getInstalledVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    fun getInstalledVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(): AppUpdateResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val remoteUrl = "https://raw.githubusercontent.com/piyush09360/anha-model/main/app_version.json"
                UpdateClient.apiService.getLatestVersion(remoteUrl)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun checkForGitHubRelease(): com.example.data.api.GitHubReleaseResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val remoteUrl = "https://api.github.com/repos/Vixid-exe/anha.ai_1/releases/latest"
                UpdateClient.apiService.getLatestGitHubRelease(remoteUrl)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun getUpdateLaterTimestamp(): Long {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("update_later_timestamp", 0L)
    }

    fun saveUpdateLaterTimestamp() {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("update_later_timestamp", System.currentTimeMillis()).apply()
    }

    suspend fun downloadApkFile(urlStr: String, file: java.io.File, onProgress: (Int) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }
                
                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = java.io.FileOutputStream(file)
                
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (inputStream.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        onProgress(((total * 100) / fileLength).toInt())
                    }
                    outputStream.write(data, 0, count)
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // --- INSTALLATION ANALYTICS ENGINE ---

    val installationsFlow: Flow<List<com.example.data.db.InstallationEntity>> = appDao.getAllInstallations()

    fun getOrCreateInstallationId(): String {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        var installId = prefs.getString("installation_id", null)
        if (installId == null) {
            installId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("installation_id", installId).also {
                it.putLong("install_timestamp", System.currentTimeMillis())
            }.apply()
        }
        return installId
    }

    fun getInstallTimestamp(): Long {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("install_timestamp", System.currentTimeMillis())
    }

    suspend fun registerCurrentInstallation() {
        val installId = getOrCreateInstallationId()
        val timestamp = getInstallTimestamp()
        val appVer = getInstalledVersionName()
        val deviceMod = android.os.Build.MODEL
        val deviceMfg = android.os.Build.MANUFACTURER
        val osVer = android.os.Build.VERSION.RELEASE
        
        val installation = com.example.data.db.InstallationEntity(
            installationId = installId,
            installTimestamp = timestamp,
            appVersion = appVer,
            deviceModel = deviceMod,
            deviceManufacturer = deviceMfg,
            androidVersion = osVer,
            lastActiveTimestamp = System.currentTimeMillis()
        )
        
        withContext(Dispatchers.IO) {
            appDao.insertInstallation(installation)
            
            val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
            
            // Check for First Launch Event
            val firstLaunchNotified = prefs.getBoolean("first_launch_notified", false)
            if (!firstLaunchNotified) {
                insertDiagnostic(
                    type = "FIRST_LAUNCH",
                    details = "Secure Setup completed. Client Onboarded successfully. Unique ID: $installId"
                )
                prefs.edit().putBoolean("first_launch_notified", true).apply()
            }

            // Check for App Update Event
            val lastCode = prefs.getInt("last_notified_version_code", -1)
            val currentCode = getInstalledVersionCode()
            
            // If we have an existing code record, and it has updated
            if (lastCode != -1 && currentCode > lastCode) {
                val prevVerStr = prefs.getString("last_notified_version_name", "1.0.0") ?: "1.0.0"
                insertDiagnostic(
                    type = "APP_UPDATE",
                    details = "Platform migration verified. v$prevVerStr (code $lastCode) -> v$appVer (code $currentCode)."
                )
            }
            
            // Persist codes for subsequent launches
            prefs.edit()
                .putInt("last_notified_version_code", currentCode)
                .putString("last_notified_version_name", appVer)
                .apply()
            
            // Check if we should populate demo records (only if we only have 1 item i.e. ours, and haven't populated it before)
            val demoPopulated = prefs.getBoolean("demo_analytics_populated", false)
            if (!demoPopulated) {
                populateDemoInstallations()
                prefs.edit().putBoolean("demo_analytics_populated", true).apply()
            }
        }
    }

    private suspend fun populateDemoInstallations() {
        val demoDevices = listOf(
            Triple("Samsung", "Galaxy S24 Ultra", "14"),
            Triple("Google", "Pixel 8 Pro", "14"),
            Triple("OnePlus", "OnePlus 12", "14"),
            Triple("Samsung", "Galaxy A54", "13"),
            Triple("Xiaomi", "Redmi Note 13", "13"),
            Triple("Google", "Pixel 7a", "13"),
            Triple("Sony", "Xperia 1 V", "13"),
            Triple("Samsung", "Galaxy S23", "14"),
            Triple("Nothing", "Phone (2)", "13"),
            Triple("Asus", "ROG Phone 8", "14"),
            Triple("Motorola", "Edge 40", "13"),
            Triple("Google", "Pixel 6", "12")
        )
        val appVersions = listOf("1.5.0", "1.4.2", "1.4.0", "1.3.0", "1.2.5")
        val baseTime = System.currentTimeMillis()
        
        for (i in demoDevices.indices) {
            val dev = demoDevices[i]
            val version = appVersions[i % appVersions.size]
            val installId = "demo_uuid_7a8${i}d84e9"
            val hoursAgo = (i + 1) * 36 + (i * 3)
            val installTime = baseTime - (hoursAgo * 3600000L)
            // Some remain active recently, some are inactive (e.g. activeTime old)
            val activeTime = if (i % 4 != 0) {
                baseTime - ((i % 5) * 4500000L)
            } else {
                baseTime - (i * 2 * 24 * 3600000L)
            }
            
            appDao.insertInstallation(
                com.example.data.db.InstallationEntity(
                    installationId = installId,
                    installTimestamp = installTime,
                    appVersion = version,
                    deviceModel = dev.second,
                    deviceManufacturer = dev.first,
                    androidVersion = dev.third,
                    lastActiveTimestamp = activeTime
                )
            )
        }

        // Seed 5 Demo Translation History items so translation dashboard visualizations are alive
        val demoTranslations = listOf(
            HistoryEntity(
                sourceText = "Have a safe flight home!",
                translatedText = "अपनी सुरक्षित घर वापसी उड़ान की कामना करता हूँ!",
                sourceLangCode = "en",
                sourceLangName = "English",
                targetLangCode = "hi",
                targetLangName = "Hindi",
                category = "Casual",
                isFavorite = true,
                timestamp = baseTime - 45000000L
            ),
            HistoryEntity(
                sourceText = "Where is the nearest metropolitan railway hub?",
                translatedText = "निकटतम मेट्रोपॉलिटन रेलवे हब कहाँ है?",
                sourceLangCode = "en",
                sourceLangName = "English",
                targetLangCode = "hi",
                targetLangName = "Hindi",
                category = "Travel",
                isFavorite = false,
                timestamp = baseTime - 36000000L
            ),
            HistoryEntity(
                sourceText = "I would appreciate your feedback on this proposal.",
                translatedText = "मैं इस प्रस्ताव पर आपकी प्रतिक्रिया की सराहना करूँगा।",
                sourceLangCode = "en",
                sourceLangName = "English",
                targetLangCode = "hi",
                targetLangName = "Hindi",
                category = "Business",
                isFavorite = true,
                timestamp = baseTime - 18000000L
            ),
            HistoryEntity(
                sourceText = "Amazing sunset scene by the river.",
                translatedText = "नदी के किनारे अद्भुत सूर्यास्त का दृश्य।",
                sourceLangCode = "en",
                sourceLangName = "English",
                targetLangCode = "hi",
                targetLangName = "Hindi",
                category = "Casual",
                isFavorite = false,
                timestamp = baseTime - 9000000L
            ),
            HistoryEntity(
                sourceText = "La reunión de negocios está programada para mañana por la mañana.",
                translatedText = "The business meeting is scheduled for tomorrow morning.",
                sourceLangCode = "es",
                sourceLangName = "Spanish",
                targetLangCode = "en",
                targetLangName = "English",
                category = "Business",
                isFavorite = false,
                timestamp = baseTime - 5000000L
            )
        )
        for (item in demoTranslations) {
            appDao.insertHistory(item)
        }

        // Seed 3 Demo Feedbacks so feedback tab has material
        val demoFeedbacks = listOf(
            FeedbackEntity(
                comment = "Translating speech locally was extremely quick on my OnePlus. High accuracy with conversation style!",
                languages = "English -> Hindi",
                contextText = "Could you please bring some water?",
                timestamp = baseTime - 24 * 3600000L,
                appVersion = "1.5.0",
                androidVersion = "14",
                deviceModel = "OnePlus 12",
                deviceManufacturer = "OnePlus"
            ),
            FeedbackEntity(
                comment = "Awesome app! Love the design and offline translation packs are fantastic for travel.",
                languages = "English -> Spanish",
                contextText = "Where can I buy tickets?",
                timestamp = baseTime - 12 * 3600000L,
                appVersion = "1.5.0",
                androidVersion = "14",
                deviceModel = "Pixel 8 Pro",
                deviceManufacturer = "Google"
            ),
            FeedbackEntity(
                comment = "Found a small grammatical discrepancy when using formal style selector for business queries.",
                languages = "Auto Detect -> Hindi",
                contextText = "We are pleased to inform you that your registration is confirmed.",
                timestamp = baseTime - 6 * 3600000L,
                appVersion = "1.4.2",
                androidVersion = "13",
                deviceModel = "Galaxy S23",
                deviceManufacturer = "Samsung",
                errorDetails = "Dialect mismatches for business formal honorifics"
            )
        )
        for (f in demoFeedbacks) {
            appDao.insertFeedback(f)
        }

        // Seed 2 Demo Telemetry Events
        val demoDiagnostics = listOf(
            com.example.data.db.DiagnosticsEntity(
                type = "TRANSLATION_FAILURE",
                appVersion = "1.5.0",
                androidVersion = "14",
                deviceModel = "Pixel 8 Pro",
                deviceManufacturer = "Google",
                details = "Gemini server timed out response block after 5000ms. Host: api.gemini.google.com, Endpoint: /v1/models/gemini",
                timestamp = baseTime - 15 * 3600000L,
                isMailNotificationSent = true
            ),
            com.example.data.db.DiagnosticsEntity(
                type = "SYSTEM_ERROR",
                appVersion = "1.4.2",
                androidVersion = "13",
                deviceModel = "Galaxy S23",
                deviceManufacturer = "Samsung",
                details = "Local translation engine failed: Invalid model packaging descriptor.",
                timestamp = baseTime - 8 * 3600000L,
                isMailNotificationSent = false
            )
        )
        for (d in demoDiagnostics) {
            appDao.insertDiagnostic(d)
        }
    }

    // --- APP DIAGNOSTICS & TELEMETRY OUTBOX ---

    val diagnosticsFlow: Flow<List<com.example.data.db.DiagnosticsEntity>> = appDao.getAllDiagnostics()

    suspend fun insertDiagnostic(type: String, details: String) {
        val appVer = getInstalledVersionName()
        val deviceMod = android.os.Build.MODEL
        val deviceMfg = android.os.Build.MANUFACTURER
        val osVer = android.os.Build.VERSION.RELEASE
        
        val diag = com.example.data.db.DiagnosticsEntity(
            type = type,
            appVersion = appVer,
            androidVersion = osVer,
            deviceModel = deviceMod,
            deviceManufacturer = deviceMfg,
            details = details,
            isMailNotificationSent = false,
            timestamp = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            appDao.insertDiagnostic(diag)
            
            // Trigger Owner Notification for critical telemetry events
            val notificationTitle = when (type) {
                "CRASH_REPORT" -> "Anha.ai Warning: App Crash \uD83D\uDED1"
                "TRANSLATION_FAILURE" -> "Anha.ai Alert: Translation Issue \u26A0\uFE0F"
                "USER_FEEDBACK" -> "Anha.ai Info: New Feedback Received \u2709\uFE0F"
                "FIRST_LAUNCH" -> "Anha.ai Alert: New Host Onboarded \uD83C\uDF89"
                else -> "Anha.ai System Event logged: $type"
            }
            triggerOwnerNotification(notificationTitle, details)
        }
    }

    suspend fun markDiagnosticsAsSent(ids: List<Long>) {
        withContext(Dispatchers.IO) {
            appDao.markDiagnosticsAsNotified(ids)
        }
    }

    suspend fun clearDiagnostics() {
        withContext(Dispatchers.IO) {
            appDao.clearDiagnostics()
        }
    }

    suspend fun simulateCrashReport(errorMessage: String) {
        val stackTrace = """
            java.lang.RuntimeException: Simulated Crash: $errorMessage
                at com.example.ui.screens.DiagnosticsDashboard.simulateCrash(DiagnosticsDashboard.kt:42)
                at com.example.ui.screens.DiagnosticsDashboard.onClick(DiagnosticsDashboard.kt:18)
                at androidx.compose.foundation.ClickableKt.handlePress(Clickable.kt:205)
                at androidx.compose.foundation.CheckableKt.access${'$'}handlePress(Checkable.kt:1)
                at androidx.compose.foundation.ClickableNode.onClick(Clickable.kt:812)
        """.trimIndent()
        insertDiagnostic("CRASH_REPORT", stackTrace)
    }

    // --- OWNER NOTIFICATION & SYSTEM UTILITIES ---

    fun triggerOwnerNotification(title: String, message: String) {
        val channelId = "anha_owner_alerts"
        val channelName = "Anha.ai Owner Alerts"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts for new feedback, installs, or crashes of Anha.ai"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
            
        try {
            notificationManager.notify((System.currentTimeMillis() % 1000000).toInt(), notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- USAGE ANALYTICS HELPERS ---

    fun getUsageFirstOpen(): Long {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("first_app_open", 0L)
    }

    fun getUsageLastOpen(): Long {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_app_open", 0L)
    }

    fun getUsageSessionCount(): Int {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("session_count", 0)
    }

    fun getUsageTotalTime(): Long {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("total_usage_time", 0L)
    }

    fun recordUsageSessionStart() {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        
        if (!prefs.contains("first_app_open")) {
            editor.putLong("first_app_open", now)
        }
        editor.putLong("last_app_open", now)
        
        val sessions = prefs.getInt("session_count", 0)
        editor.putInt("session_count", sessions + 1)
        editor.apply()
    }

    fun accumulateUsageTime(ms: Long) {
        val prefs = context.getSharedPreferences("anha_analytics_prefs", Context.MODE_PRIVATE)
        val currentTotal = prefs.getLong("total_usage_time", 0L)
        prefs.edit().putLong("total_usage_time", currentTotal + ms).apply()
    }
}
