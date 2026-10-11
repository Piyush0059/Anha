package com.example.data.api

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class AppUpdateResponse(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val releaseNotes: String? = null,
    val isMandatory: Boolean = false,
    val minSupportedVersionCode: Int? = null
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseResponse(
    val tag_name: String,
    val name: String? = null,
    val html_url: String,
    val body: String? = null,
    val assets: List<GitHubAsset>? = null
)

@JsonClass(generateAdapter = true)
data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

interface UpdateApiService {
    @GET
    suspend fun getLatestVersion(@Url url: String): AppUpdateResponse

    @GET
    suspend fun getLatestGitHubRelease(
        @Url url: String,
        @Header("User-Agent") userAgent: String = "Anha-App"
    ): GitHubReleaseResponse
}
