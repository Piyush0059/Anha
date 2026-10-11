package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MainActivity
import com.example.data.api.TranslationResult
import com.example.data.model.Language
import com.example.data.model.LanguagesList
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.TranslationUiState
import com.example.ui.viewmodel.TranslationViewModel
import com.example.util.SpeechToTextManager
import com.example.util.TextToSpeechManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

@Composable
fun HomeScreen(
    viewModel: TranslationViewModel,
    ttsManager: TextToSpeechManager,
    sttManager: SpeechToTextManager
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val inputText by viewModel.inputText.collectAsState()
    val sourceLang by viewModel.sourceLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()
    val categoryStyle by viewModel.categoryStyle.collectAsState()
    val translationState by viewModel.translationState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isLargeText by viewModel.isLargeText.collectAsState()
    val ttsSpeed by viewModel.ttsSpeed.collectAsState()
    val ttsGender by viewModel.ttsGender.collectAsState()
    val isListening by viewModel.isVoiceInputActive.collectAsState()

    var showSourceDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!viewModel.isNetworkConnected()) {
                Toast.makeText(context, "Network unavailable. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val code = if (sourceLang.code == "auto") "en" else sourceLang.code
            sttManager.startListening(code)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice translation.", Toast.LENGTH_LONG).show()
        }
    }

    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val cardBg = if (isDarkMode) Color(0xFF13172E).copy(alpha = 0.5f) else Color.White

    // Handle large text sizes easily
    val titleSize = if (isLargeText) 24.sp else 20.sp
    val bodySize = if (isLargeText) 18.sp else 15.sp
    val subtitleSize = if (isLargeText) 15.sp else 12.sp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // HEADER ROW WITH LOGO ACCENTS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_anha_logo),
                    contentDescription = "Anha.ai Brand Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Anha",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = ".ai",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA) // Purple-400
                    )
                }
            }

            // Quick Dark Mode Toggle from Toolbar
            IconButton(
                onClick = { viewModel.toggleDarkMode() },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0x0DFFFFFF), shape = CircleShape)
            ) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF1E293B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // LANGUAGE CHANGER CARD (Pill style from HTML)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x0DFFFFFF), shape = CircleShape)
                .border(1.dp, Color(0x1BFFFFFF), shape = CircleShape)
                .padding(4.dp)
                .testTag("language_selector_panel"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Language Selector
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { showSourceDialog = true }
                        .padding(vertical = 11.dp)
                        .testTag("source_language_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sourceLang.code == "auto") {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF9333EA), shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = sourceLang.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC084FC) // purple-300
                    )
                }

                // Middle Swap Button
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0x1AFFFFFF), shape = CircleShape)
                        .border(1.dp, Color(0x10FFFFFF), shape = CircleShape)
                        .testTag("swap_languages_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Swap languages",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Target Language Selector
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { showTargetDialog = true }
                        .padding(vertical = 11.dp)
                        .testTag("target_language_button"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = targetLang.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI STYLING PILLS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val categories = listOf("Casual", "Formal", "Business", "Academic", "Travel")
            categories.forEach { category ->
                val isSelected = categoryStyle == category
                val bgModifier = if (isSelected) {
                    Modifier.background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B82F6))))
                } else {
                    Modifier.background(if (isDarkMode) Color(0x0DFFFFFF) else Color(0xFFE2E8F0))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .then(bgModifier)
                        .clickable { viewModel.setCategoryStyle(category) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = if (isSelected || isDarkMode) Color.White else Color(0xFF334155),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // INPUT SECTION CARD
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            isDarkTheme = isDarkMode,
            cornerRadius = 32.dp,
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Absolute top decorative gradient line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF9333EA).copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "SOURCE LANGUAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B), // slate-500
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    TextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        modifier = Modifier.fillMaxSize().testTag("translation_input_field"),
                        placeholder = {
                            Text(
                                "Type or paste text to translate...",
                                color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                                fontSize = if (isLargeText) 20.sp else 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A),
                            unfocusedTextColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            viewModel.translate()
                        }),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = if (isLargeText) 22.sp else 18.sp,
                            fontWeight = FontWeight.Light,
                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A)
                        )
                    )

                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearInput() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(36.dp)
                                .background(Color(0x11FFFFFF), shape = CircleShape)
                                .testTag("clear_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Divider(
                    color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Voice microphone button (w-11 h-11 circular style)
                        IconButton(
                            onClick = {
                                if (isListening) {
                                    sttManager.stopListening()
                                } else {
                                    val isPermissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (isPermissionGranted) {
                                        if (!viewModel.isNetworkConnected()) {
                                            Toast.makeText(context, "Network unavailable. Please check your internet connection.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val code = if (sourceLang.code == "auto") "en" else sourceLang.code
                                            sttManager.startListening(code)
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Color(0xFFEF4444) else Color(0x0DFFFFFF)
                                )
                                .border(1.dp, Color(0x1BFFFFFF), shape = CircleShape)
                                .testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Voice Speech Recognition",
                                tint = if (isListening) Color.White else (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A)),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Play Original audio pronunciation
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val code = if (sourceLang.code == "auto") "en" else sourceLang.code
                                    ttsManager.speak(inputText, code, ttsSpeed, ttsGender)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x0DFFFFFF))
                                    .border(1.dp, Color(0x1BFFFFFF), shape = CircleShape)
                                    .testTag("pronounce_original_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak Original",
                                    tint = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF1E40AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Translate Action Button (Gradient from purple to blue)
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.translate()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                        modifier = Modifier
                            .testTag("translate_pills_button")
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B82F6))),
                                shape = RoundedCornerShape(24.dp)
                            ),
                        enabled = inputText.isNotBlank()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Text("Translate", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Active equalizer waveform canvas indicator during active listening state
        if (isListening) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF10B981), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Anha is currently listening to your voice...", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TRANSLATED RESULT VIEW STATE
        AnimatedVisibility(
            visible = translationState !is TranslationUiState.Idle,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, shape = RoundedCornerShape(32.dp), clip = false)
                    .border(1.dp, Color(0x33A78BFA), shape = RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x524B1A7A), // purple-900/42
                                Color(0x171E3A8A)  // blue-900/18
                            )
                        )
                    )
                    .padding(24.dp)
                    .testTag("result_display_panel")
            ) {
                // Background ambient soft light
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-40).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0x299333EA), Color.Transparent)
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    when (val state = translationState) {
                        is TranslationUiState.Idle -> {
                            // Keep idle state clean
                        }
                        is TranslationUiState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color(0xFFA78BFA))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Engine translating...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                        is TranslationUiState.Error -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(state.message, color = if (isDarkMode) Color(0xFFF9A8D4) else Color(0xFFDC2626), textAlign = TextAlign.Center, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.translate() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("retry_translation_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry Icon", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Translation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        is TranslationUiState.Success -> {
                            val result = state.result
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Diagnostics detected language row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI detected", tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Detected: ${result.detected_language_name}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC084FC), // Purple-300
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF9333EA).copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFF9333EA).copy(alpha = 0.3f), shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "SCORE: ${"%.0f".format(result.confidence_score * 100)}%",
                                            fontSize = 10.sp,
                                            color = Color(0xFFE9D5FF),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "TRANSLATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC084FC),
                                    letterSpacing = 1.2.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Text(
                                    text = result.translated_text,
                                    fontSize = if (isLargeText) 22.sp else 18.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.fillMaxWidth().testTag("translated_result_text"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 26.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Divider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Mode Pill Button matching HTML
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0x1AFFFFFF), shape = CircleShape)
                                            .border(1.dp, Color(0x33A78BFA), shape = CircleShape)
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "$categoryStyle Mode",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE9D5FF) // purple-200
                                        )
                                    }

                                    // Circular actions in footer: Speech, Copy, Share
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Speak Translated audio pronunciation
                                        IconButton(
                                            onClick = {
                                                ttsManager.speak(result.translated_text, targetLang.code, ttsSpeed, ttsGender)
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, Color(0x10FFFFFF), shape = CircleShape)
                                                .testTag("pronounce_translated_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak translation text out loud",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Copy result
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(result.translated_text))
                                                Toast.makeText(context, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, Color(0x10FFFFFF), shape = CircleShape)
                                                .testTag("copy_result_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Translation",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Share result
                                        IconButton(
                                            onClick = {
                                                val intent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_TEXT, result.translated_text)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                                            },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x1AFFFFFF))
                                                .border(1.dp, Color(0x10FFFFFF), shape = CircleShape)
                                                .testTag("share_result_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
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

    // LANGUAGE SELECTOR DIALOGS
    if (showSourceDialog) {
        LanguagePickerDialog(
            title = "Translate From",
            languages = viewModel.sourceLanguages,
            onDismiss = { showSourceDialog = false },
            onSelect = {
                viewModel.selectSourceLanguage(it)
                showSourceDialog = false
            }
        )
    }

    if (showTargetDialog) {
        LanguagePickerDialog(
            title = "Translate To",
            languages = viewModel.targetLanguages,
            onDismiss = { showTargetDialog = false },
            onSelect = {
                viewModel.selectTargetLanguage(it)
                showTargetDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    title: String,
    languages: List<Language>,
    onDismiss: () -> Unit,
    onSelect: (Language) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredLangs = remember(searchQuery) {
        if (searchQuery.isBlank()) languages else {
            languages.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val indianLangs = filteredLangs.filter { it.isIndian }
    val internationalLangs = filteredLangs.filter { !it.isIndian && it.code != "auto" }
    val autoOption = filteredLangs.find { it.code == "auto" }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13172E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search language...", fontSize = 14.sp, color = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth().testTag("language_picker_search_input"),
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF262C4D)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f).testTag("languages_picker_list")) {
                    if (autoOption != null) {
                        item {
                            ListItem(
                                headlineContent = { Text(autoOption.name, color = Color(0xFF10B981), fontWeight = FontWeight.Bold) },
                                leadingContent = { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSelect(autoOption) }.testTag("language_option_${autoOption.code}")
                            )
                            Divider(color = Color(0xFF262C4D))
                        }
                    }

                    if (indianLangs.isNotEmpty()) {
                        item {
                            Text(
                                "INDIAN LANGUAGES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                        items(indianLangs) { lang ->
                            ListItem(
                                headlineContent = { Text(lang.name, color = Color.White) },
                                leadingContent = { Icon(imageVector = Icons.Default.Translate, contentDescription = null, tint = Color(0xFF94A3B8)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSelect(lang) }.testTag("language_option_${lang.code}")
                            )
                        }
                    }

                    if (internationalLangs.isNotEmpty()) {
                        item {
                            Text(
                                "INTERNATIONAL LANGUAGES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                            )
                        }
                        items(internationalLangs) { lang ->
                            ListItem(
                                headlineContent = { Text(lang.name, color = Color.White) },
                                leadingContent = { Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = Color(0xFF94A3B8)) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSelect(lang) }.testTag("language_option_${lang.code}")
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
