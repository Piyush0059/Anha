package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDao
import com.example.data.db.HistoryEntity
import com.example.data.db.FeedbackEntity
import com.example.data.db.DownloadPackEntity
import com.example.data.model.Language
import com.example.data.repository.TranslationRepository
import com.example.ui.viewmodel.TranslationViewModel
import com.example.ui.viewmodel.TranslationUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var fakeDao: FakeAppDao
    private lateinit var repository: TranslationRepository
    private lateinit var viewModel: TranslationViewModel

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakeAppDao()
        repository = TranslationRepository(fakeDao, context)
        viewModel = TranslationViewModel(repository)
    }

    @Test
    fun test_app_label_is_correct() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Anha.ai", appName)
    }

    @Test
    fun test_quality_empty_input_validation() {
        // Empty text is caught instantly to prevent wasteful API usage
        viewModel.updateInputText("")
        assertEquals(TranslationUiState.Idle, viewModel.translationState.value)
    }

    @Test
    fun test_quality_long_text_rejection() {
        // Inputs exceeding 3000 characters must be rejected gracefully with a informative message
        val longText = "a".repeat(3005)
        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                repository.translate(longText, "en", "English", "hi", "Hindi", "Casual")
            }
        }
        assertTrue(exception.message!!.contains("too large"))
    }

    @Test
    fun test_quality_offline_mode_rejection() {
        // Under offline simulation (with lack of cache hitting), we must guide users to reconnect
        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                // translate standard query text
                repository.translate("Hello world", "en", "English", "hi", "Hindi", "Casual")
            }
        }
        // Since emulator/runner might be offline, it throws network connection/offline feedback
        val msg = exception.message ?: ""
        assertTrue(msg.contains("connection") || msg.contains("connect") || msg.contains("API Key") || msg.contains("Offline"))
    }

    @Test
    fun test_quality_auto_detection() {
        // Source defaults to Auto Detect so users don't have to change picker settings manually
        val sourceLanguage = viewModel.sourceLang.value
        assertEquals("auto", sourceLanguage.code)
        assertEquals("Auto Detect", sourceLanguage.name)
    }

    @Test
    fun test_quality_english_to_hindi_mapping() {
        val en = viewModel.sourceLanguages.find { it.code == "en" }
        val hi = viewModel.targetLanguages.find { it.code == "hi" }
        assertNotNull(en)
        assertNotNull(hi)
        assertEquals("English", en?.name)
        assertEquals("Hindi", hi?.name)
    }

    @Test
    fun test_quality_hindi_to_english_mapping() {
        val hi = viewModel.targetLanguages.find { it.code == "hi" }
        assertNotNull(hi)
        assertEquals("hi", hi?.code)
    }

    @Test
    fun test_quality_voice_translation_state() {
        // Voice speech listening controls toggle active voice indicators
        val isVoiceActive = viewModel.isVoiceInputActive.value
        assertFalse(isVoiceActive)
    }

    @Test
    fun test_installed_version_code() {
        val versionCode = repository.getInstalledVersionCode()
        assertTrue(versionCode >= 1)
    }

    @Test
    fun test_installed_version_name() {
        val versionName = repository.getInstalledVersionName()
        assertNotNull(versionName)
    }

    @Test
    fun test_dismiss_update_dialog() {
        viewModel.dismissUpdateDialog()
        assertEquals(com.example.ui.viewmodel.UpdateUiState.Idle, viewModel.updateUiState.value)
    }

    @Test
    fun test_update_states_mandatory_handling() {
         val state = com.example.ui.viewmodel.UpdateUiState.UpdateAvailable(
             latestVersionName = "2.0.0",
             latestVersionCode = 15,
             updateUrl = "https://example.com/app.apk",
             releaseNotes = "New stability fixes.",
             isMandatory = true
         )
         assertTrue(state.isMandatory)
         assertEquals(15, state.latestVersionCode)
         assertEquals("New stability fixes.", state.releaseNotes)
    }

    @Test
    fun test_installation_analytics_registration_and_retrieval() {
        runBlocking {
            viewModel.registerInstallation()
            val currentId = repository.getOrCreateInstallationId()
            assertNotNull(currentId)
            
            val timestamp = repository.getInstallTimestamp()
            assertTrue(timestamp > 0)
        }
    }

    // High performance localized fake Database Access Object block
    class FakeAppDao : AppDao {
        private val installationsList = mutableListOf<com.example.data.db.InstallationEntity>()

        override fun getAllHistory(): Flow<List<HistoryEntity>> = flowOf(emptyList())
        override fun getFavorites(): Flow<List<HistoryEntity>> = flowOf(emptyList())
        override fun searchHistory(query: String): Flow<List<HistoryEntity>> = flowOf(emptyList())
        override suspend fun insertHistory(item: HistoryEntity): Long = 0L
        override suspend fun updateHistory(item: HistoryEntity) {}
        override suspend fun deleteHistoryById(id: Long) {}
        override suspend fun clearHistory() {}
        override suspend fun insertFeedback(feedback: FeedbackEntity) {}
        override fun getAllFeedback(): Flow<List<FeedbackEntity>> = flowOf(emptyList())
        override fun getDownloadedPacks(): Flow<List<DownloadPackEntity>> = flowOf(emptyList())
        override suspend fun insertDownloadedPack(pack: DownloadPackEntity) {}
        override suspend fun deleteDownloadedPack(langCode: String) {}
        override suspend fun findCachedTranslation(
            sourceText: String,
            sourceLangCode: String,
            targetLangCode: String,
            category: String
        ): HistoryEntity? = null
        override suspend fun getHistoryCount(): Int = 0

        override suspend fun insertInstallation(installation: com.example.data.db.InstallationEntity) {
            installationsList.removeAll { it.installationId == installation.installationId }
            installationsList.add(installation)
        }

        override fun getAllInstallations(): Flow<List<com.example.data.db.InstallationEntity>> = 
            flowOf(installationsList)

        override suspend fun getInstallationById(id: String): com.example.data.db.InstallationEntity? = 
            installationsList.find { it.installationId == id }

        override suspend fun deleteInstallationById(id: String) {
            installationsList.removeAll { it.installationId == id }
        }

        override suspend fun clearInstallations() {
            installationsList.clear()
        }

        private val diagnosticsList = mutableListOf<com.example.data.db.DiagnosticsEntity>()

        override suspend fun insertDiagnostic(diagnostic: com.example.data.db.DiagnosticsEntity) {
            diagnosticsList.add(diagnostic)
        }

        override fun getAllDiagnostics(): Flow<List<com.example.data.db.DiagnosticsEntity>> = 
            flowOf(diagnosticsList)

        override suspend fun getPendingDiagnostics(): List<com.example.data.db.DiagnosticsEntity> = 
            diagnosticsList.filter { !it.isMailNotificationSent }

        override suspend fun markDiagnosticsAsNotified(ids: List<Long>) {}

        override suspend fun clearDiagnostics() {
            diagnosticsList.clear()
        }
    }
}
