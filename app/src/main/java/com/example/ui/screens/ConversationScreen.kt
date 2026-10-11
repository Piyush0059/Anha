package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Language
import com.example.data.model.LanguagesList
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.ConversationTurn
import com.example.ui.viewmodel.TranslationViewModel
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

@Composable
fun ConversationScreen(
    viewModel: TranslationViewModel,
    ttsManager: TextToSpeechManager,
    sttManager: SpeechToTextManager
) {
    val context = LocalContext.current
    val history by viewModel.conversationHistory.collectAsState()
    val speakerALang by viewModel.speakerALang.collectAsState()
    val speakerBLang by viewModel.speakerBLang.collectAsState()
    val activeSpeaker by viewModel.activeSpeaker.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isListening by viewModel.isVoiceInputActive.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val ttsGender by viewModel.ttsGender.collectAsState()

    var activeDialogSelection by remember { mutableStateOf<String?>(null) } // "A" or "B" for lang picking
    var customTextEntry by remember { mutableStateOf("") }
    var showDirectTexter by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    // Automatically scroll to the latest conversation turn
    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) {
            listState.animateScrollToItem(history.size - 1)
        }
    }

    // Capture speech action listener
    val sttCoordinator = remember {
        SpeechToTextManager(
            context = context,
            onResult = { text ->
                if (text.isNotBlank()) {
                    viewModel.addToConversation(
                        speaker = activeSpeaker,
                        text = text,
                        onResultSpoken = { translation, code ->
                            ttsManager.speak(translation, code, ttsSpeed, ttsGender)
                        }
                    )
                }
            },
            onListeningState = { listening ->
                viewModel.setVoiceInputActive(listening)
            },
            onErrorState = { errorMsg ->
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                viewModel.setVoiceInputActive(false)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!viewModel.isNetworkConnected()) {
                Toast.makeText(context, "Network unavailable. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val targetCode = if (activeSpeaker == "Speaker A") speakerALang.code else speakerBLang.code
            sttCoordinator.startListening(targetCode)
            viewModel.setConversationActive(true)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice translation.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            sttCoordinator.stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Conversation Mode Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Conversation Interpreter",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = "Real-time natural communication flow",
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFF06B6D4) else Color(0xFF475569)
                )
            }

            Row {
                IconButton(onClick = { showDirectTexter = !showDirectTexter }) {
                    Icon(
                        imageVector = if (showDirectTexter) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = "Manual text input",
                        tint = if (isDarkMode) Color.White else Color(0xFF475569)
                    )
                }
                IconButton(onClick = { viewModel.clearConversation() }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear dialogue",
                        tint = Color(0xFFF43F5E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Split Speakers Language Panels Configuration with Swap Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speaker A Settings
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeDialogSelection = "A" }
                    .testTag("speaker_a_lang_card"),
                isDarkTheme = isDarkMode,
                cornerRadius = 12.dp,
                elevation = 2.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPEAKER A", fontSize = 10.sp, color = Color(0xFF9333EA), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(speakerALang.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 1)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF9333EA))
                    }
                }
            }

            // Swap Languages Button
            IconButton(
                onClick = { viewModel.swapSpeakerLanguages() },
                modifier = Modifier
                    .size(38.dp)
                    .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0), shape = CircleShape)
                    .testTag("swap_interpreter_languages_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap speaker languages",
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Speaker B Settings
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { activeDialogSelection = "B" }
                    .testTag("speaker_b_lang_card"),
                isDarkTheme = isDarkMode,
                cornerRadius = 12.dp,
                elevation = 2.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SPEAKER B", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(speakerBLang.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 1)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF3B82F6))
                    }
                }
            }
        }

        // TEXT OVERLAY FOR EMULATOR IN CASE SpeechToText LACKS CORE SERVICES
        AnimatedVisibility(visible = showDirectTexter) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customTextEntry,
                onValueChange = { customTextEntry = it },
                placeholder = { Text("Or type manually as Speaker A/B...", color = Color(0xFF64748B), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().testTag("manual_conversation_entry"),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            if (customTextEntry.isNotBlank()) {
                                viewModel.setActiveSpeaker("Speaker A")
                                viewModel.addToConversation(
                                    speaker = "Speaker A",
                                    text = customTextEntry,
                                    onResultSpoken = { translation, code ->
                                        ttsManager.speak(translation, code, ttsSpeed, ttsGender)
                                    }
                                )
                                customTextEntry = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Speaker A send", tint = Color(0xFF8B5CF6))
                        }
                        IconButton(onClick = {
                            if (customTextEntry.isNotBlank()) {
                                viewModel.setActiveSpeaker("Speaker B")
                                viewModel.addToConversation(
                                    speaker = "Speaker B",
                                    text = customTextEntry,
                                    onResultSpoken = { translation, code ->
                                        ttsManager.speak(translation, code, ttsSpeed, ttsGender)
                                    }
                                )
                                customTextEntry = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Speaker B send", tint = Color(0xFF06B6D4))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor,
                    unfocusedBorderColor = dividerColor,
                    focusedBorderColor = Color(0xFF8B5CF6)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONVERSATION DIALOGUE LIST
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    if (isDarkMode) Color(0xFF0F1122).copy(alpha = 0.3f) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = if (isDarkMode) Color(0xFF262C4D) else Color(0xFF94A3B8),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No dialogue active",
                        color = if (isDarkMode) Color(0xFF475569) else Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select speakers, tap the Mic below, and begin communicating back-and-forth.",
                        color = if (isDarkMode) Color(0xFF475569) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { turn ->
                        val isSpeakerA = turn.speaker == "Speaker A"
                        val senderLabel = if (isSpeakerA) "Speaker A (${speakerALang.name})" else "Speaker B (${speakerBLang.name})"
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = if (isSpeakerA) Alignment.Start else Alignment.End
                        ) {
                            Text(
                                text = senderLabel,
                                fontSize = 10.sp,
                                color = if (isSpeakerA) Color(0xFFC084FC) else Color(0xFF60A5FA),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp),
                                letterSpacing = 0.5.sp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isSpeakerA) 4.dp else 16.dp,
                                            bottomEnd = if (isSpeakerA) 16.dp else 4.dp
                                        )
                                    )
                                    .background(
                                        if (turn.isError) {
                                            Brush.linearGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.25f), Color(0xFFEF4444).copy(alpha = 0.10f)))
                                        } else if (isSpeakerA) {
                                            Brush.linearGradient(listOf(Color(0xFF9333EA).copy(alpha = 0.20f), Color(0xFF9333EA).copy(alpha = 0.08f)))
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.20f), Color(0xFF3B82F6).copy(alpha = 0.08f)))
                                        }
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (turn.isError) Color(0xFFEF4444).copy(alpha = 0.4f) else if (isSpeakerA) Color(0xFF9333EA).copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isSpeakerA) 4.dp else 16.dp,
                                            bottomEnd = if (isSpeakerA) 16.dp else 4.dp
                                        )
                                    )
                                    .padding(14.dp)
                                    .widthIn(max = 290.dp)
                            ) {
                                Column {
                                    Text(
                                        text = turn.originalText,
                                        fontSize = 14.sp,
                                        color = primaryTextColor,
                                        fontWeight = FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Divider(color = primaryTextColor.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    if (turn.isTranslating) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = if (isSpeakerA) Color(0xFFC084FC) else Color(0xFF60A5FA)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = turn.translatedText,
                                                fontSize = 13.sp,
                                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = turn.translatedText,
                                            fontSize = 15.sp,
                                            color = if (turn.isError) Color(0xFFFCA5A5) else if (isSpeakerA) Color(0xFFE9D5FF) else Color(0xFFDBEAFE),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Action bar for turn: playback & retry
                                    if (!turn.isTranslating) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (turn.isError) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.retryConversationTurn(turn.id) { spokenText, code ->
                                                            ttsManager.speak(spokenText, code, ttsSpeed, ttsGender)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("retry_turn_${turn.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Retry translation",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        val targetLangCode = if (turn.targetLangCode.isNotBlank()) {
                                                            turn.targetLangCode
                                                        } else {
                                                            if (isSpeakerA) speakerBLang.code else speakerALang.code
                                                        }
                                                        ttsManager.speak(turn.translatedText, targetLangCode, ttsSpeed, ttsGender)
                                                    },
                                                    modifier = Modifier.size(28.dp).testTag("speak_turn_${turn.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = "Speak translation",
                                                        tint = if (isSpeakerA) Color(0xFFC084FC) else Color(0xFF60A5FA),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isListening,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val pulsingColor = if (activeSpeaker == "Speaker A") Color(0xFF9333EA) else Color(0xFF3B82F6)
            val currentLangName = if (activeSpeaker == "Speaker A") speakerALang.name else speakerBLang.name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(pulsingColor.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp))
                    .border(0.5.dp, pulsingColor.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = pulsingColor, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$activeSpeaker is speaking in $currentLangName...", 
                    color = pulsingColor, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INTERPRET CONTROLS: split microphone triggers
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Microphone Speaker A
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val listening = isListening && activeSpeaker == "Speaker A"
                        if (listening) {
                            sttCoordinator.stopListening()
                            viewModel.setConversationActive(false)
                        } else {
                            viewModel.setActiveSpeaker("Speaker A")
                            val isPermissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isPermissionGranted) {
                                if (!viewModel.isNetworkConnected()) {
                                    Toast.makeText(context, "Network unavailable. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                                } else {
                                    sttCoordinator.startListening(speakerALang.code)
                                    viewModel.setConversationActive(true)
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isListening && activeSpeaker == "Speaker A") Color(0xFFEF4444) else Color(0xFF9333EA),
                            shape = CircleShape
                        )
                        .border(1.dp, Color(0x33FFFFFF), shape = CircleShape)
                        .testTag("convo_mic_speaker_a")
                ) {
                    Icon(
                        imageVector = if (isListening && activeSpeaker == "Speaker A") Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak as Speaker A",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Speak A", fontSize = 11.sp, color = primaryTextColor, fontWeight = FontWeight.SemiBold)
            }

            Text("Interpreter Mode", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)

            // Microphone Speaker B
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        val listening = isListening && activeSpeaker == "Speaker B"
                        if (listening) {
                            sttCoordinator.stopListening()
                            viewModel.setConversationActive(false)
                        } else {
                            viewModel.setActiveSpeaker("Speaker B")
                            val isPermissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (isPermissionGranted) {
                                if (!viewModel.isNetworkConnected()) {
                                    Toast.makeText(context, "Network unavailable. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                                } else {
                                    sttCoordinator.startListening(speakerBLang.code)
                                    viewModel.setConversationActive(true)
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isListening && activeSpeaker == "Speaker B") Color(0xFFEF4444) else Color(0xFF3B82F6),
                            shape = CircleShape
                        )
                        .border(1.dp, Color(0x33FFFFFF), shape = CircleShape)
                        .testTag("convo_mic_speaker_b")
                ) {
                    Icon(
                        imageVector = if (isListening && activeSpeaker == "Speaker B") Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Speak as Speaker B",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Speak B", fontSize = 11.sp, color = primaryTextColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Dialogue picker configs
    if (activeDialogSelection != null) {
        val targetPick = activeDialogSelection!!
        LanguagePickerDialog(
            title = "Speaker $targetPick Language",
            languages = LanguagesList.languages,
            onDismiss = { activeDialogSelection = null },
            onSelect = {
                if (targetPick == "A") {
                    viewModel.selectSpeakerALang(it)
                } else {
                    viewModel.selectSpeakerBLang(it)
                }
                activeDialogSelection = null
            }
        )
    }
}
