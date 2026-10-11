package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // History
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM translation_history WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity): Long

    @Update
    suspend fun updateHistory(item: HistoryEntity)

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearHistory()

    // Feedback
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Query("SELECT * FROM user_feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    // Downloaded language packs
    @Query("SELECT * FROM downloaded_packs")
    fun getDownloadedPacks(): Flow<List<DownloadPackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedPack(pack: DownloadPackEntity)

    @Query("DELETE FROM downloaded_packs WHERE langCode = :langCode")
    suspend fun deleteDownloadedPack(langCode: String)

    // Cached translations lookup for free tier Gemini usage optimization
    @Query("SELECT * FROM translation_history WHERE LOWER(TRIM(sourceText)) = LOWER(TRIM(:sourceText)) AND (sourceLangCode = :sourceLangCode OR :sourceLangCode = 'auto') AND targetLangCode = :targetLangCode AND category = :category ORDER BY timestamp DESC LIMIT 1")
    suspend fun findCachedTranslation(
        sourceText: String,
        sourceLangCode: String,
        targetLangCode: String,
        category: String
    ): HistoryEntity?

    @Query("SELECT COUNT(*) FROM translation_history")
    suspend fun getHistoryCount(): Int

    // Installation Analytics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallation(installation: InstallationEntity)

    @Query("SELECT * FROM installation_analytics ORDER BY installTimestamp DESC")
    fun getAllInstallations(): Flow<List<InstallationEntity>>

    @Query("SELECT * FROM installation_analytics WHERE installationId = :id LIMIT 1")
    suspend fun getInstallationById(id: String): InstallationEntity?

    @Query("DELETE FROM installation_analytics WHERE installationId = :id")
    suspend fun deleteInstallationById(id: String)

    @Query("DELETE FROM installation_analytics")
    suspend fun clearInstallations()

    // App Diagnostics System & Outbox Database queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnostic(diagnostic: DiagnosticsEntity)

    @Query("SELECT * FROM app_diagnostics ORDER BY timestamp DESC")
    fun getAllDiagnostics(): Flow<List<DiagnosticsEntity>>

    @Query("SELECT * FROM app_diagnostics WHERE isMailNotificationSent = 0")
    suspend fun getPendingDiagnostics(): List<DiagnosticsEntity>

    @Query("UPDATE app_diagnostics SET isMailNotificationSent = 1 WHERE id IN (:ids)")
    suspend fun markDiagnosticsAsNotified(ids: List<Long>)

    @Query("DELETE FROM app_diagnostics")
    suspend fun clearDiagnostics()
}
