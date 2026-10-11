package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLangCode: String,
    val sourceLangName: String,
    val targetLangCode: String,
    val targetLangName: String,
    val category: String, // Casual, Formal, Business, Academic, Travel
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_feedback")
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comment: String,
    val languages: String, // e.g. "English (India) -> Hindi"
    val contextText: String, // context text of translation
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val androidVersion: String = "",
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    val errorDetails: String = ""
)

@Entity(tableName = "downloaded_packs")
data class DownloadPackEntity(
    @PrimaryKey val langCode: String,
    val langName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "installation_analytics")
data class InstallationEntity(
    @PrimaryKey val installationId: String,
    val installTimestamp: Long,
    val appVersion: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidVersion: String,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_diagnostics")
data class DiagnosticsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // FIRST_LAUNCH, APP_UPDATE, TRANSLATION_FAILURE, SYSTEM_ERROR, CRASH_REPORT
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "",
    val androidVersion: String = "",
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    val details: String = "",
    val isMailNotificationSent: Boolean = false
)

