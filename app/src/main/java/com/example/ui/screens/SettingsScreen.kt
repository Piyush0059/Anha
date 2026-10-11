package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.data.model.Language
import com.example.data.model.LanguagesList
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.TranslationViewModel
import com.example.ui.viewmodel.TranslationUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: TranslationViewModel) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isLargeText by viewModel.isLargeText.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val ttsGender by viewModel.ttsGender.collectAsState()
    val downloadedPacks by viewModel.downloadedPacksList.collectAsState()

    val scope = rememberCoroutineScope()

    val currentSourceLang by viewModel.sourceLang.collectAsState()
    val currentTargetLang by viewModel.targetLang.collectAsState()

    var feedbackComment by remember { mutableStateOf("") }
    var feedbackLanguage by remember { mutableStateOf("English -> Hindi") }
    var feedbackContext by remember { mutableStateOf("Translation mismatch context sample.") }

    LaunchedEffect(currentSourceLang, currentTargetLang) {
        feedbackLanguage = "${currentSourceLang.name} -> ${currentTargetLang.name}"
    }

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAdminDashboard by remember { mutableStateOf(false) }

    var selectedLangToDownload by remember { mutableStateOf<Language?>(null) }
    var isDownloadingPack by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Engine Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor
        )
        Text(
            text = "Accessibility, speech configs, and feedback",
            fontSize = 11.sp,
            color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ACCESSIBILITY & STYLING
            item {
                Text(
                    "ACCESSIBILITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9333EA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = secondaryTextColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Force Dark Mode Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text("Premium high-contrast slate aesthetics", fontSize = 11.sp, color = secondaryTextColor)
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF9333EA), checkedTrackColor = Color(0xFF9333EA).copy(alpha = 0.5f))
                            )
                        }

                        Divider(color = dividerColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, tint = secondaryTextColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Enlarged UI Text Scale", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text("Improves general reading and scanability", fontSize = 11.sp, color = secondaryTextColor)
                                }
                            }
                            Switch(
                                checked = isLargeText,
                                onCheckedChange = { viewModel.toggleLargeText() },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF9333EA), checkedTrackColor = Color(0xFF9333EA).copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // ADVANCED SPEECH TEXT TO SPEECH SETTINGS
            item {
                Text(
                    "ADVANCED VOICE & SPEECH CONFIGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Gender Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = secondaryTextColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Synthetic Speaker Gender", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text("Female or Male pronunciation", fontSize = 11.sp, color = secondaryTextColor)
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                listOf("Female", "Male").forEach { gender ->
                                    val isSelected = ttsGender == gender
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF3B82F6) else Color.Transparent)
                                            .clickable { viewModel.setTtsGender(gender) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(gender, color = if (isSelected || isDarkMode) Color.White else Color(0xFF334155), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Divider(color = dividerColor)

                        // Speech Speed rate slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = secondaryTextColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Speech Playback Velocity", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                }
                                Text("${ttsSpeed}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = ttsSpeed,
                                onValueChange = { viewModel.setTtsSpeed(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF3B82F6),
                                    activeTrackColor = Color(0xFF3B82F6)
                                )
                            )
                        }
                    }
                }
            }

            // OFFLINE SUPPORT PACKS
            item {
                Text(
                    "OFFLINE ACCESS SYSTEM PACKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column {
                        Text(
                            text = "Download localized models for offline translations and TTS backups.",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Offline downloaded status
                        if (downloadedPacks.isEmpty()) {
                            Text(
                                "No language packs on device.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        } else {
                            Text(
                                "Installed packs: ${downloadedPacks.joinToString { it.langName }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Download trigger
                        OutlinedButton(
                            onClick = {
                                selectedLangToDownload = LanguagesList.languages.random()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth().testTag("add_pack_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Install Offline Language Pack")
                        }

                        if (selectedLangToDownload != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ready: ${selectedLangToDownload!!.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Button(
                                    onClick = {
                                        isDownloadingPack = true
                                        downloadProgress = 0f
                                        scope.launch {
                                            while (downloadProgress < 1.0f) {
                                                delay(100)
                                                downloadProgress += 0.05f
                                            }
                                            viewModel.downloadLanguagePack(selectedLangToDownload!!)
                                            Toast.makeText(context, "${selectedLangToDownload!!.name} pack installed!", Toast.LENGTH_SHORT).show()
                                            selectedLangToDownload = null
                                            isDownloadingPack = false
                                        }
                                    },
                                    enabled = !isDownloadingPack,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Download", color = Color.White)
                                }
                            }
                        }

                        if (isDownloadingPack) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // FEEDBACK MECHANISM SECTION PANEL
            item {
                Text(
                    "REPORT MISTAKES & IMPROVEMENTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Help perfect Anha.ai. Report translations issues or suggest localized styling improvements.",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )

                        OutlinedTextField(
                            value = feedbackLanguage,
                            onValueChange = { feedbackLanguage = it },
                            label = { Text("Languages involved") },
                            modifier = Modifier.fillMaxWidth().testTag("feedback_langs"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                unfocusedLabelColor = secondaryTextColor,
                                focusedBorderColor = Color(0xFFF43F5E)
                            )
                        )

                        OutlinedTextField(
                            value = feedbackContext,
                            onValueChange = { feedbackContext = it },
                            label = { Text("Context or Original Text") },
                            modifier = Modifier.fillMaxWidth().testTag("feedback_context"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                unfocusedLabelColor = secondaryTextColor,
                                focusedBorderColor = Color(0xFFF43F5E)
                            )
                        )

                        OutlinedTextField(
                            value = feedbackComment,
                            onValueChange = { feedbackComment = it },
                            label = { Text("Mistake description / Remarks") },
                            modifier = Modifier.fillMaxWidth().height(80.dp).testTag("feedback_comment"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                unfocusedLabelColor = secondaryTextColor,
                                focusedBorderColor = Color(0xFFF43F5E)
                            ),
                            maxLines = 3
                        )

                        Button(
                            onClick = {
                                if (feedbackComment.isNotBlank()) {
                                    showFeedbackDialog = true
                                } else {
                                    Toast.makeText(context, "Please enter your remarks before submitting.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                            modifier = Modifier.fillMaxWidth().testTag("submit_feedback_button")
                        ) {
                            Text("Submit Secure Feedback", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // PRIVACY & ANLYTICS TRANSPARENCY NOTICE
            item {
                Text(
                    "PRIVACY & TRUST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure Diagnostics Disclosure", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                        }
                        Text(
                            text = "To maintain performance and track service uptime, Anha.ai generates a unique installation ID. We record installation timestamps, app version, and standard device specifications (OS version, manufacturer, and model).\n\n" +
                                   "We strictly do NOT collect personal files, contacts, messages, passwords, precise location, IMEIs, or other sensitive personal identifiers. Your data remains stored securely inside the sandboxed local database, fully respecting your privacy.",
                            fontSize = 11.sp,
                            color = secondaryTextColor,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // UPDATE SYSTEM
            item {
                Text(
                    "SYSTEM UPDATES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9333EA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = secondaryTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Check for Updates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Text("Verify if a newer release exists on GitHub", fontSize = 11.sp, color = secondaryTextColor)
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.triggerUpdateCheck(isManual = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                modifier = Modifier.testTag("check_updates_settings_button")
                            ) {
                                Text("Check Now", color = Color.White)
                            }
                        }
                    }
                }
            }

            // APP VERSION & DEVELOPER DIAGNOSTICS FOOTER
            item {
                var developerClicks by remember { mutableStateOf(0) }
                val isDeveloperMode by viewModel.isDeveloperMode.collectAsState()
                var cachedCount by remember { mutableStateOf(0) }
                
                LaunchedEffect(isDeveloperMode) {
                    if (isDeveloperMode) {
                        cachedCount = viewModel.getCacheCount()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Anha.ai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Version ${viewModel.getAppVersionName()} (Production Release)",
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        modifier = Modifier
                            .clickable {
                                developerClicks++
                                if (developerClicks >= 7) {
                                    viewModel.toggleDeveloperMode()
                                    if (isDeveloperMode) {
                                        Toast.makeText(context, "Developer Mode Disabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Developer Mode Enabled! Advanced diagnostics unlocked.", Toast.LENGTH_SHORT).show()
                                    }
                                    developerClicks = 0
                                } else if (developerClicks >= 3) {
                                    Toast.makeText(context, "You are ${7 - developerClicks} steps away from a developer.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(8.dp)
                            .testTag("app_version_label")
                    )

                    AnimatedVisibility(
                        visible = isDeveloperMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DEVELOPER DIAGNOSTICS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                                
                                Divider(color = dividerColor)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Internet Status:", fontSize = 12.sp, color = secondaryTextColor)
                                    val isConnected = viewModel.isNetworkConnected()
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444)))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isConnected) "Connected" else "No Connection", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("API Client Status:", fontSize = 12.sp, color = secondaryTextColor)
                                    val isKeyConfigured = com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                                    Text(
                                        text = if (isKeyConfigured) "Active & Validated" else "Misconfigured Secrets",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKeyConfigured) Color(0xFF10B981) else Color(0xFFFBBF24)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Local Cache Entries:", fontSize = 12.sp, color = secondaryTextColor)
                                    Text("$cachedCount translation pairs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Persistent Errors logged:", fontSize = 12.sp, color = secondaryTextColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = viewModel.getLastError(),
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = if (viewModel.getLastError() == "None") Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { showAdminDashboard = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    modifier = Modifier.fillMaxWidth().testTag("launch_admin_dashboard_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Launch Admin Analytics Dashboard", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFeedbackDialog) {
            val appVersion = viewModel.getAppVersionName()
            val androidVersion = android.os.Build.VERSION.RELEASE
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            val lastError = viewModel.getLastError()

            AlertDialog(
                onDismissRequest = { showFeedbackDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showFeedbackDialog = false
                            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            
                            // 1. Submit feedback to local secure Database (Room)
                            viewModel.submitFeedback(
                                comment = feedbackComment,
                                languages = feedbackLanguage,
                                contextText = feedbackContext,
                                appVersion = appVersion,
                                androidVersion = androidVersion,
                                deviceModel = deviceModel,
                                deviceManufacturer = deviceManufacturer,
                                errorDetails = lastError
                            )
                            
                            // 2. Trigger feedback email notification
                            sendFeedbackEmail(
                                context = context,
                                targetEmail = "indo00059@gmail.com",
                                comment = feedbackComment,
                                languages = feedbackLanguage,
                                contextText = feedbackContext,
                                appVersion = appVersion,
                                androidVersion = androidVersion,
                                deviceModel = deviceModel,
                                deviceManufacturer = deviceManufacturer,
                                errorDetails = lastError,
                                timestamp = timestamp,
                                sourceLang = currentSourceLang.name,
                                targetLang = currentTargetLang.name
                            )
                            
                            // 3. Inform user of success
                            Toast.makeText(context, "Feedback sent successfully.", Toast.LENGTH_SHORT).show()
                            feedbackComment = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                        modifier = Modifier.testTag("confirm_feedback_send_button")
                    ) {
                        Text("Agree & Send", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFeedbackDialog = false },
                        modifier = Modifier.testTag("cancel_feedback_send_button")
                    ) {
                        Text("Cancel")
                    }
                },
                title = {
                    Text("Diagnostic Privacy Consent", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "To help debug and perfect Anha.ai, your feedback report can include secure diagnostic info:\n\n" +
                        "• App Version: $appVersion\n" +
                        "• Android OS: $androidVersion\n" +
                        "• Device Model: $deviceModel\n" +
                        "• Device Manufacturer: $deviceManufacturer\n" +
                        "• Selected Source Lang: ${currentSourceLang.name}\n" +
                        "• Selected Target Lang: ${currentTargetLang.name}\n" +
                        "• Last system error recorded: $lastError\n\n" +
                        "We strictly do NOT collect personal files, contacts, messages, passwords, or location data."
                    )
                },
                containerColor = if (isDarkMode) Color(0xFF13172E) else Color.White,
                titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                textContentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
            )
        }

        if (showAdminDashboard) {
            com.example.ui.screens.AdminDashboardDialog(
                viewModel = viewModel,
                onDismiss = { showAdminDashboard = false }
            )
        }
    }
}

fun sendFeedbackEmail(
    context: android.content.Context,
    targetEmail: String,
    comment: String,
    languages: String,
    contextText: String,
    appVersion: String,
    androidVersion: String,
    deviceModel: String,
    deviceManufacturer: String,
    errorDetails: String,
    timestamp: String,
    sourceLang: String,
    targetLang: String
) {
    val emailBody = """
        Anha.ai User Feedback
        --------------------
        Feedback Message:
        $comment
        
        Languages Involved: $languages
        Selected Source Language: $sourceLang
        Selected Target Language: $targetLang
        Context / Original Text: $contextText
        
        System & Diagnostic Information:
        - Timestamp: $timestamp
        - App Version: $appVersion
        - Android OS Version: $androidVersion
        - Device Model: $deviceModel
        - Device Manufacturer: $deviceManufacturer
        - Error Details (if available): $errorDetails
        
        ---
        Privacy Note: Privacy is fully respected. Personal files, contacts, messages, passwords, or precise location data were not collected.
    """.trimIndent()

    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
        data = android.net.Uri.parse("mailto:") // only email apps should handle this
        putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(targetEmail))
        putExtra(android.content.Intent.EXTRA_SUBJECT, "Anha.ai Feedback - Diagnostic Report")
        putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
    }
    
    try {
        context.startActivity(android.content.Intent.createChooser(intent, "Send feedback email using..."))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No email client found. Feedback compiled & copied to clipboard.", android.widget.Toast.LENGTH_LONG).show()
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Anha.ai Feedback", emailBody)
        clipboard.setPrimaryClip(clip)
    }
}
