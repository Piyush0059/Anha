package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.TranslationResult
import com.example.data.db.HistoryEntity
import com.example.data.db.DownloadPackEntity
import com.example.data.model.Language
import com.example.data.model.LanguagesList
import com.example.data.repository.TranslationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TranslationUiState {
    object Idle : TranslationUiState
    object Loading : TranslationUiState
    data class Success(val result: TranslationResult) : TranslationUiState
    data class Error(val message: String) : TranslationUiState
}

sealed interface UpdateUiState {
    object Idle : UpdateUiState
    object Checking : UpdateUiState
    data class UpdateAvailable(
        val currentVersionName: String = "1.0.0",
        val latestVersionName: String,
        val latestVersionCode: Int = 100,
        val updateUrl: String,
        val releaseNotes: String?,
        val isMandatory: Boolean
    ) : UpdateUiState
    data class Downloading(val progress: Int) : UpdateUiState
    data class ReadyToInstall(val apkFile: java.io.File, val isMandatory: Boolean) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
    object NoUpdate : UpdateUiState
    data class UpToDate(val isManual: Boolean) : UpdateUiState
}

data class ConversationTurn(
    val id: Long = System.currentTimeMillis() + (1..1000).random(),
    val speaker: String, // "Speaker A" or "Speaker B"
    val originalText: String,
    val translatedText: String,
    val sourceLangCode: String = "",
    val targetLangCode: String = "",
    val isError: Boolean = false,
    val isTranslating: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class TranslationViewModel(private val repository: TranslationRepository) : ViewModel() {

    // Language list configuration
    val autoLanguage = Language("auto", "Auto Detect", false, "en")
    val sourceLanguages = listOf(autoLanguage) + LanguagesList.languages
    val targetLanguages = LanguagesList.languages

    // Primary UI States
    private val _sourceLang = MutableStateFlow(autoLanguage)
    val sourceLang: StateFlow<Language> = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow(LanguagesList.languages.first()) // Hindi is first
    val targetLang: StateFlow<Language> = _targetLang.asStateFlow()

    private val _categoryStyle = MutableStateFlow("Casual") // Casual, Formal, Business, Academic, Travel
    val categoryStyle: StateFlow<String> = _categoryStyle.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translationState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val translationState: StateFlow<TranslationUiState> = _translationState.asStateFlow()

    // History and Favorites from Database
    val historyList: StateFlow<List<HistoryEntity>> = repository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteList: StateFlow<List<HistoryEntity>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedPacksList: StateFlow<List<DownloadPackEntity>> = repository.downloadedPacksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History search
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    val filteredHistory: StateFlow<List<HistoryEntity>> = _historySearchQuery
        .debounce(300)
        .flatMapLatest { query -> repository.searchHistory(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Speech controls / settings
    private val _isVoiceInputActive = MutableStateFlow(false)
    val isVoiceInputActive: StateFlow<Boolean> = _isVoiceInputActive.asStateFlow()

    fun setVoiceInputActive(active: Boolean) {
        _isVoiceInputActive.value = active
    }

    private val _ttsSpeed = MutableStateFlow(1.0f) // 0.75f, 1.0f, 1.25f, 1.5f
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsGender = MutableStateFlow("Female") // Male, Female
    val ttsGender: StateFlow<String> = _ttsGender.asStateFlow()

    // Conversation Mode state
    private val _isConversationActive = MutableStateFlow(false)
    val isConversationActive: StateFlow<Boolean> = _isConversationActive.asStateFlow()

    private val _conversationHistory = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationHistory: StateFlow<List<ConversationTurn>> = _conversationHistory.asStateFlow()

    // Conversational speakers configured languages
    private val _speakerALang = MutableStateFlow(LanguagesList.languages.find { it.code == "en" } ?: LanguagesList.languages.first())
    val speakerALang: StateFlow<Language> = _speakerALang.asStateFlow()

    private val _speakerBLang = MutableStateFlow(LanguagesList.languages.find { it.code == "hi" } ?: LanguagesList.languages.first())
    val speakerBLang: StateFlow<Language> = _speakerBLang.asStateFlow()

    private val _activeSpeaker = MutableStateFlow("Speaker A") // "Speaker A" or "Speaker B"
    val activeSpeaker: StateFlow<String> = _activeSpeaker.asStateFlow()

    // App customization preferences
    private val _isDarkMode = MutableStateFlow(true) // Premium slate-glow visual looks phenomenal in dark mode
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isLargeText = MutableStateFlow(false)
    val isLargeText: StateFlow<Boolean> = _isLargeText.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    fun toggleDeveloperMode() {
        _isDeveloperMode.value = !_isDeveloperMode.value
    }

    fun isNetworkConnected(): Boolean {
        return repository.isNetworkConnected()
    }

    fun getLastError(): String {
        return repository.getLastError()
    }

    suspend fun getCacheCount(): Int {
        return repository.getCacheCount()
    }

    fun updateInputText(text: String) {
        _inputText.value = text
        if (text.isBlank()) {
            _translationState.value = TranslationUiState.Idle
            currentTranslationJob?.cancel()
        }
    }

    fun selectSourceLanguage(language: Language) {
        _sourceLang.value = language
    }

    fun selectTargetLanguage(language: Language) {
        _targetLang.value = language
    }

    fun setCategoryStyle(style: String) {
        _categoryStyle.value = style
    }

    fun setSearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleLargeText() {
        _isLargeText.value = !_isLargeText.value
    }

    fun setTtsSpeed(speed: Float) {
        _ttsSpeed.value = speed
    }

    fun setTtsGender(gender: String) {
        _ttsGender.value = gender
    }

    fun clearInput() {
        _inputText.value = ""
        _translationState.value = TranslationUiState.Idle
        currentTranslationJob?.cancel()
    }

    fun swapLanguages() {
        val currentSource = _sourceLang.value
        val currentTarget = _targetLang.value
        // If current source is "Auto Detect", swap with target default or target current
        if (currentSource.code == "auto") {
            _sourceLang.value = currentTarget
            _targetLang.value = LanguagesList.languages.find { it.code == "en" } ?: LanguagesList.languages.first()
        } else {
            _sourceLang.value = currentTarget
            _targetLang.value = currentSource
        }
    }

    private var currentTranslationJob: kotlinx.coroutines.Job? = null
    private var lastActiveRequestKey: String? = null

    // Trigger translation
    fun translate() {
        val queryText = _inputText.value
        if (queryText.isBlank()) return

        // 1. Click throttle guard: if it is already loading for the EXACT same query parameters, do not submit/re-trigger
        val currentKey = "${queryText.trim()}_${_sourceLang.value.code}_${_targetLang.value.code}_${_categoryStyle.value}"
        if (_translationState.value is TranslationUiState.Loading && currentKey == lastActiveRequestKey) {
            return // Ignore duplicated button clicks while querying
        }

        lastActiveRequestKey = currentKey

        // Cancel previous active job (e.g. if we changed text or parameters dynamically to prevent backlog spam)
        currentTranslationJob?.cancel()

        currentTranslationJob = viewModelScope.launch {
            _translationState.value = TranslationUiState.Loading
            try {
                // Ensure a small debounce delay before executing network calls if clicked in rapid succession
                kotlinx.coroutines.delay(200)

                val result = repository.translate(
                    text = queryText,
                    sourceLangCode = _sourceLang.value.code,
                    sourceLangName = _sourceLang.value.name,
                    targetLangCode = _targetLang.value.code,
                    targetLangName = _targetLang.value.name,
                    category = _categoryStyle.value
                )
                _translationState.value = TranslationUiState.Success(result)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellation exceptions when the job was canceled by a newer request
            } catch (e: Exception) {
                _translationState.value = TranslationUiState.Error(e.message ?: "Translation unsuccessful")
            }
        }
    }

    // Toggle favorite
    fun toggleFavorite(item: HistoryEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    // Delete a historic translation
    fun deleteHistoryId(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    // Clear entire history
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Submit Feedback
    fun submitFeedback(
        comment: String,
        languages: String,
        contextText: String,
        appVersion: String,
        androidVersion: String,
        deviceModel: String,
        deviceManufacturer: String,
        errorDetails: String
    ) {
        viewModelScope.launch {
            repository.submitFeedback(
                comment = comment,
                languages = languages,
                contextText = contextText,
                appVersion = appVersion,
                androidVersion = androidVersion,
                deviceModel = deviceModel,
                deviceManufacturer = deviceManufacturer,
                errorDetails = errorDetails
            )
        }
    }

    fun getAppVersionName(): String = repository.getInstalledVersionName()

    // Download offline language pack
    fun downloadLanguagePack(lang: Language) {
        viewModelScope.launch {
            repository.downloadPack(lang.code, lang.name)
        }
    }

    // Delete language pack
    fun removeLanguagePack(langCode: String) {
        viewModelScope.launch {
            repository.removePack(langCode)
        }
    }

    // --- Conversation Mode ---
    fun selectSpeakerALang(lang: Language) {
        _speakerALang.value = lang
    }

    fun selectSpeakerBLang(lang: Language) {
        _speakerBLang.value = lang
    }

    fun swapSpeakerLanguages() {
        val temp = _speakerALang.value
        _speakerALang.value = _speakerBLang.value
        _speakerBLang.value = temp
        _activeSpeaker.value = if (_activeSpeaker.value == "Speaker A") "Speaker B" else "Speaker A"
    }

    fun setActiveSpeaker(speaker: String) {
        _activeSpeaker.value = speaker
    }

    fun setConversationActive(active: Boolean) {
        _isConversationActive.value = active
    }

    fun addToConversation(speaker: String, text: String, onResultSpoken: (String, String) -> Unit) {
        if (text.isBlank()) return
        
        val localSource = if (speaker == "Speaker A") _speakerALang.value else _speakerBLang.value
        val localTarget = if (speaker == "Speaker A") _speakerBLang.value else _speakerALang.value
        
        val turnId = System.currentTimeMillis() + (1..1000).random()
        val inFlightTurn = ConversationTurn(
            id = turnId,
            speaker = speaker,
            originalText = text,
            translatedText = "Translating...",
            sourceLangCode = localSource.code,
            targetLangCode = localTarget.code,
            isTranslating = true,
            isError = false
        )
        _conversationHistory.value = _conversationHistory.value + inFlightTurn

        viewModelScope.launch {
            try {
                val result = repository.translate(
                    text = text,
                    sourceLangCode = localSource.code,
                    sourceLangName = localSource.name,
                    targetLangCode = localTarget.code,
                    targetLangName = localTarget.name,
                    category = _categoryStyle.value
                )
                
                val completedTurn = ConversationTurn(
                    id = turnId,
                    speaker = speaker,
                    originalText = text,
                    translatedText = result.translated_text,
                    sourceLangCode = localSource.code,
                    targetLangCode = localTarget.code,
                    isTranslating = false,
                    isError = false
                )
                _conversationHistory.value = _conversationHistory.value.map {
                    if (it.id == turnId) completedTurn else it
                }
                
                // Triggers outloud text to speech automatically for translation matching target code!
                onResultSpoken(result.translated_text, localTarget.code)
            } catch (e: Exception) {
                val failedTurn = ConversationTurn(
                    id = turnId,
                    speaker = speaker,
                    originalText = text,
                    translatedText = "Translation failed: ${e.message ?: "Network error"}",
                    sourceLangCode = localSource.code,
                    targetLangCode = localTarget.code,
                    isTranslating = false,
                    isError = true
                )
                _conversationHistory.value = _conversationHistory.value.map {
                    if (it.id == turnId) failedTurn else it
                }
            }
        }
    }

    fun retryConversationTurn(turnId: Long, onResultSpoken: (String, String) -> Unit) {
        val turn = _conversationHistory.value.find { it.id == turnId } ?: return
        val localSource = if (turn.speaker == "Speaker A") _speakerALang.value else _speakerBLang.value
        val localTarget = if (turn.speaker == "Speaker A") _speakerBLang.value else _speakerALang.value

        _conversationHistory.value = _conversationHistory.value.map {
            if (it.id == turnId) it.copy(translatedText = "Retrying...", isTranslating = true, isError = false) else it
        }

        viewModelScope.launch {
            try {
                val result = repository.translate(
                    text = turn.originalText,
                    sourceLangCode = localSource.code,
                    sourceLangName = localSource.name,
                    targetLangCode = localTarget.code,
                    targetLangName = localTarget.name,
                    category = _categoryStyle.value
                )
                val completedTurn = turn.copy(
                    translatedText = result.translated_text,
                    targetLangCode = localTarget.code,
                    isTranslating = false,
                    isError = false
                )
                _conversationHistory.value = _conversationHistory.value.map {
                    if (it.id == turnId) completedTurn else it
                }
                onResultSpoken(result.translated_text, localTarget.code)
            } catch (e: Exception) {
                val failedTurn = turn.copy(
                    translatedText = "Retry failed: ${e.message ?: "Network error"}",
                    isTranslating = false,
                    isError = true
                )
                _conversationHistory.value = _conversationHistory.value.map {
                    if (it.id == turnId) failedTurn else it
                }
            }
        }
    }

    fun clearConversation() {
        _conversationHistory.value = emptyList()
    }

    // App Update State
    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    val feedbacks: StateFlow<List<com.example.data.db.FeedbackEntity>> = repository.feedbackFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks running session duration (seconds)
    private val _currentSessionDurationSec = MutableStateFlow(0L)
    val currentSessionDurationSec: StateFlow<Long> = _currentSessionDurationSec.asStateFlow()

    fun getUsageFirstOpen(): Long = repository.getUsageFirstOpen()
    fun getUsageLastOpen(): Long = repository.getUsageLastOpen()
    fun getUsageSessionCount(): Int = repository.getUsageSessionCount()
    fun getUsageTotalTime(): Long = repository.getUsageTotalTime()

    init {
        triggerUpdateCheck()
        registerInstallation()
        
        // Record active usage session launch
        viewModelScope.launch {
            try {
                repository.recordUsageSessionStart()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Live timer tick and periodic persistence logic to sum total usage
        viewModelScope.launch {
            var secs = 0L
            while (true) {
                kotlinx.coroutines.delay(1000)
                secs += 1
                _currentSessionDurationSec.value = secs
                if (secs % 10 == 0L) {
                    try {
                        repository.accumulateUsageTime(10000L)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateUiState.value = UpdateUiState.Idle
    }

    fun snoozeUpdateCheck() {
        repository.saveUpdateLaterTimestamp()
        _updateUiState.value = UpdateUiState.Idle
    }

    fun isNewerVersion(installed: String, latest: String): Boolean {
        return try {
            val cleanInstalled = installed.trim().removePrefix("v").removePrefix("V")
            val cleanLatest = latest.trim().removePrefix("v").removePrefix("V")
            
            val installedParts = cleanInstalled.split(".")
            val latestParts = cleanLatest.split(".")
            
            val length = maxOf(installedParts.size, latestParts.size)
            for (i in 0 until length) {
                val installedPart = installedParts.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                val latestPart = latestParts.getOrNull(i)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0
                
                if (latestPart > installedPart) return true
                if (installedPart > latestPart) return false
            }
            false
        } catch (e: Exception) {
            e.printStackTrace()
            latest != installed
        }
    }

    fun triggerUpdateCheck(isManual: Boolean = false) {
        viewModelScope.launch {
            if (!isManual) {
                val lastLaterTime = repository.getUpdateLaterTimestamp()
                val oneDayMs = 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastLaterTime < oneDayMs) {
                    _updateUiState.value = UpdateUiState.Idle
                    return@launch
                }
            }
            
            _updateUiState.value = UpdateUiState.Checking
            val currentVersionName = repository.getInstalledVersionName()
            val remoteRelease = repository.checkForGitHubRelease()
            if (remoteRelease != null) {
                val latestVersionName = remoteRelease.tag_name.trim().removePrefix("v").removePrefix("V")
                if (isNewerVersion(currentVersionName, latestVersionName)) {
                    var apkUrl = remoteRelease.html_url
                    remoteRelease.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.let {
                        apkUrl = it.browser_download_url
                    }
                    _updateUiState.value = UpdateUiState.UpdateAvailable(
                        currentVersionName = currentVersionName,
                        latestVersionName = latestVersionName,
                        updateUrl = apkUrl,
                        releaseNotes = remoteRelease.body,
                        isMandatory = false
                    )
                } else {
                    _updateUiState.value = UpdateUiState.UpToDate(isManual)
                }
            } else {
                if (isManual) {
                    _updateUiState.value = UpdateUiState.Error("Failed to fetch release information. Please check your internet connection.")
                } else {
                    _updateUiState.value = UpdateUiState.NoUpdate
                }
            }
        }
    }

    fun startApkDownload(url: String, isMandatory: Boolean) {
        viewModelScope.launch {
            _updateUiState.value = UpdateUiState.Downloading(0)
            
            val targetDir = java.io.File(repository.context.cacheDir, "updates")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val apkFile = java.io.File(targetDir, "AnhaUpdate.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }
            
            val success = repository.downloadApkFile(url, apkFile) { progress ->
                _updateUiState.value = UpdateUiState.Downloading(progress)
            }
            
            if (success && apkFile.exists()) {
                _updateUiState.value = UpdateUiState.ReadyToInstall(apkFile, isMandatory)
            } else {
                _updateUiState.value = UpdateUiState.Error("Failed to download update APK. Please try again.")
            }
        }
    }

    // --- INSTALLATION ANALYTICS & DIAGNOSTICS APIs ---

    val installations: StateFlow<List<com.example.data.db.InstallationEntity>> = repository.installationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diagnostics: StateFlow<List<com.example.data.db.DiagnosticsEntity>> = repository.diagnosticsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun registerInstallation() {
        viewModelScope.launch {
            try {
                repository.registerCurrentInstallation()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllInstallations() {
        viewModelScope.launch {
            try {
                repository.context.getSharedPreferences("anha_analytics_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("demo_analytics_populated", false)
                    .apply()
                repository.registerCurrentInstallation() // will re-register only ours and re-populate fresh demo if needed or we can clear
                _updateUiState.value = UpdateUiState.Idle
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun simulateCrash(message: String) {
        viewModelScope.launch {
            try {
                repository.simulateCrashReport(message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markDiagnosticsAsSent(ids: List<Long>) {
        viewModelScope.launch {
            try {
                repository.markDiagnosticsAsSent(ids)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearDiagnostics() {
        viewModelScope.launch {
            try {
                repository.clearDiagnostics()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerSimulatedUpdateLog(prevName: String, prevCode: Int, newName: String, newCode: Int) {
        viewModelScope.launch {
            try {
                repository.insertDiagnostic(
                    type = "APP_UPDATE",
                    details = "Simulated platform update: v$prevName (code $prevCode) -> v$newName (code $newCode)."
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class TranslationViewModelFactory(private val repository: TranslationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TranslationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TranslationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
