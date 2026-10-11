package com.example.ui.view

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.*
import com.example.ui.viewmodel.TranslationViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechManager
import com.example.ui.viewmodel.UpdateUiState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnhaApp(viewModel: TranslationViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val updateUiState by viewModel.updateUiState.collectAsState()
    var currentTab by remember { mutableStateOf("home") }
    var showSplash by remember { mutableStateOf(true) }

    val context = LocalContext.current

    // Initialize TTS and STT helpers
    val ttsManager = remember { TextToSpeechManager(context) }
    val sttManager = remember {
        SpeechToTextManager(
            context = context,
            onResult = { text ->
                if (text.isNotBlank()) {
                    viewModel.updateInputText(text)
                    viewModel.translate()
                }
            },
            onPartialResult = { partial ->
                if (partial.isNotBlank()) {
                    viewModel.updateInputText(partial)
                }
            },
            onErrorState = { errorMsg ->
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.setVoiceInputActive(false)
            },
            onListeningState = { listening ->
                viewModel.setVoiceInputActive(listening)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
            sttManager.stopListening()
        }
    }

    MyApplicationTheme(darkTheme = isDarkMode) {
        if (showSplash) {
            SplashScreen(isDarkMode = isDarkMode, onTimeout = { showSplash = false })
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar(
                        containerColor = if (isDarkMode) Color(0xFF05050A) else MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .border(width = 0.5.dp, color = Color(0x0DFFFFFF))
                            .testTag("primary_bottom_navigation")
                    ) {
                        // HOME NAVIGATION BUTTON
                        NavigationBarItem(
                            selected = currentTab == "home",
                            onClick = { currentTab = "home" },
                            icon = { Icon(Icons.Default.Translate, contentDescription = "Home Translate") },
                            label = { Text("Translate") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9333EA),
                                selectedTextColor = Color(0xFF9333EA),
                                indicatorColor = Color(0x1F9333EA),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("tab_home")
                        )

                        // CONVERSATION NAVIGATION BUTTON
                        NavigationBarItem(
                            selected = currentTab == "conversation",
                            onClick = { currentTab = "conversation" },
                            icon = { Icon(Icons.Default.Forum, contentDescription = "Conversation Interpreting") },
                            label = { Text("Interpreter") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9333EA),
                                selectedTextColor = Color(0xFF9333EA),
                                indicatorColor = Color(0x1F9333EA),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("tab_conversation")
                        )

                        // HISTORY NAVIGATION BUTTON
                        NavigationBarItem(
                            selected = currentTab == "history",
                            onClick = { currentTab = "history" },
                            icon = { Icon(Icons.Default.History, contentDescription = "History Log") },
                            label = { Text("History") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9333EA),
                                selectedTextColor = Color(0xFF9333EA),
                                indicatorColor = Color(0x1F9333EA),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("tab_history")
                        )

                        // FAVORITES NAVIGATION BUTTON
                        NavigationBarItem(
                            selected = currentTab == "favorites",
                            onClick = { currentTab = "favorites" },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarked Translations") },
                            label = { Text("Favorites") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9333EA),
                                selectedTextColor = Color(0xFF9333EA),
                                indicatorColor = Color(0x1F9333EA),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("tab_favorites")
                        )

                        // SETTINGS NAVIGATION BUTTON
                        NavigationBarItem(
                            selected = currentTab == "settings",
                            onClick = { currentTab = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "App Settings") },
                            label = { Text("Settings") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF9333EA),
                                selectedTextColor = Color(0xFF9333EA),
                                indicatorColor = Color(0x1F9333EA),
                                unselectedIconColor = Color(0xFF64748B),
                                unselectedTextColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn() with fadeOut()
                        },
                        label = "MainTabsAnimation"
                    ) { targetState ->
                        when (targetState) {
                            "home" -> HomeScreen(viewModel, ttsManager, sttManager)
                            "conversation" -> ConversationScreen(viewModel, ttsManager, sttManager)
                            "history" -> HistoryScreen(viewModel, preSelectedFavoritesOnly = false, onNavigateToHome = { currentTab = it })
                            "favorites" -> HistoryScreen(viewModel, preSelectedFavoritesOnly = true, onNavigateToHome = { currentTab = it })
                            "settings" -> SettingsScreen(viewModel)
                        }
                    }
                }
            }

            when (val state = updateUiState) {
                is UpdateUiState.UpdateAvailable -> {
                    AlertDialog(
                        onDismissRequest = { 
                            if (!state.isMandatory) {
                                viewModel.snoozeUpdateCheck() 
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.dismissUpdateDialog()
                                    val url = state.updateUrl
                                    val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No browser found to open link.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                modifier = Modifier.testTag("update_now_button")
                            ) {
                                Text("Update Now", color = Color.White)
                            }
                        },
                        dismissButton = {
                            if (!state.isMandatory) {
                                TextButton(
                                    onClick = { viewModel.snoozeUpdateCheck() },
                                    modifier = Modifier.testTag("update_later_button")
                                ) {
                                    Text("Later")
                                }
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.img_anha_logo),
                                    contentDescription = "Anha.ai Icon",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(end = 8.dp)
                                )
                                Text(
                                    text = "New Version Available",
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "A newer version of Anha.ai is available. Update now to get the latest improvements, fixes and features.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = "CURRENT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF3B82F6),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = state.currentVersionName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.weight(1.0f)) {
                                        Text(
                                            text = "LATEST",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = state.latestVersionName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                                
                                if (!state.releaseNotes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Release Notes",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 120.dp)
                                            .background(
                                                color = if (isDarkMode) Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.02f),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            text = state.releaseNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = if (isDarkMode) Color(0xFF13172E) else Color.White,
                        titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        textContentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                    )
                }
                is UpdateUiState.UpToDate -> {
                    LaunchedEffect(state) {
                        if (state.isManual) {
                            android.widget.Toast.makeText(context, "You are using the latest version.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        viewModel.dismissUpdateDialog()
                    }
                }
                is UpdateUiState.Downloading -> {
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {},
                        dismissButton = null,
                        title = {
                            Text("Downloading Update", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Please wait while the latest update of Anha.ai is downloaded securely...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = state.progress / 100f,
                                    modifier = Modifier.fillMaxWidth().testTag("update_download_progress"),
                                    color = Color(0xFF9333EA),
                                    trackColor = Color(0xFFE2E8F0)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${state.progress}% completed",
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9333EA)
                                )
                            }
                        },
                        containerColor = if (isDarkMode) Color(0xFF13172E) else Color.White,
                        titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        textContentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                    )
                }
                is UpdateUiState.ReadyToInstall -> {
                    AlertDialog(
                        onDismissRequest = {
                            if (!state.isMandatory) {
                                viewModel.dismissUpdateDialog()
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    triggerApkInstallation(context, state.apkFile)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                modifier = Modifier.testTag("install_now_button")
                            ) {
                                Text("Install Now", color = Color.White)
                            }
                        },
                        dismissButton = {
                            if (!state.isMandatory) {
                                TextButton(
                                    onClick = { viewModel.dismissUpdateDialog() },
                                    modifier = Modifier.testTag("install_later_button")
                                ) {
                                    Text("Later")
                                }
                            }
                        },
                        title = {
                            Text("Install Update", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        },
                        text = {
                            Text("The update package has been downloaded successfully. Click below to install it.")
                        },
                        containerColor = if (isDarkMode) Color(0xFF13172E) else Color.White,
                        titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        textContentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                    )
                }
                is UpdateUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.dismissUpdateDialog() },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.triggerUpdateCheck()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                modifier = Modifier.testTag("update_retry_button")
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { viewModel.dismissUpdateDialog() },
                                modifier = Modifier.testTag("update_error_dismiss")
                            ) {
                                Text("Dismiss")
                            }
                        },
                        title = {
                            Text("Update Failed", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        },
                        text = {
                            Text(state.message)
                        },
                        containerColor = if (isDarkMode) Color(0xFF13172E) else Color.White,
                        titleContentColor = if (isDarkMode) Color.White else Color(0xFF0F172A),
                        textContentColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF475569)
                    )
                }
                else -> {}
            }
        }
    }
}

fun triggerApkInstallation(context: android.content.Context, file: java.io.File) {
    try {
        val apkUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                android.widget.Toast.makeText(context, "Please enable unknown sources settings, then try installing again.", android.widget.Toast.LENGTH_LONG).show()
                return
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Installation error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
