package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.HistoryEntity
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.TranslationViewModel

@Composable
fun HistoryScreen(
    viewModel: TranslationViewModel,
    preSelectedFavoritesOnly: Boolean = false,
    onNavigateToHome: (String) -> Unit // Handles loading a history item back onto Home screen for reuse!
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val historySearchQuery by viewModel.historySearchQuery.collectAsState()
    val filteredHistory by viewModel.filteredHistory.collectAsState()
    val favoriteList by viewModel.favoriteList.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showFavoritesOnly by remember { mutableStateOf(preSelectedFavoritesOnly) }

    LaunchedEffect(preSelectedFavoritesOnly) {
        showFavoritesOnly = preSelectedFavoritesOnly
    }

    val activeList = if (showFavoritesOnly) favoriteList else filteredHistory
    val primaryTextColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardDivider = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear All History?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently erase all your saved translation histories? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmation = false
                        Toast.makeText(context, "History permanently cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = primaryTextColor)
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E2E) else Color.White,
            titleContentColor = primaryTextColor,
            textContentColor = secondaryTextColor
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // History Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Translations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
                Text(
                    text = "History, bookmarks, and search reuse",
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF64748B)
                )
            }
 
            if (filteredHistory.isNotEmpty() && !showFavoritesOnly) {
                IconButton(onClick = { showClearConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Clear all history",
                        tint = Color(0xFFF43F5E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle Buttons row: All History vs Bookmarks
        val bgAllHistory = if (!showFavoritesOnly) {
            Modifier.background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B82F6))))
        } else Modifier

        val bgFavorites = if (showFavoritesOnly) {
            Modifier.background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF3B82F6))))
        } else Modifier

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDarkMode) Color(0xFF11111E) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, if (isDarkMode) Color(0x1AFFFFFF) else Color.Transparent, shape = RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(bgAllHistory)
                    .clickable { showFavoritesOnly = false }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All History",
                    color = if (!showFavoritesOnly || isDarkMode) Color.White else Color(0xFF334155),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(bgFavorites)
                    .clickable { showFavoritesOnly = true }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Favorites Only",
                    color = if (showFavoritesOnly || isDarkMode) Color.White else Color(0xFF334155),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input field
        if (!showFavoritesOnly) {
            OutlinedTextField(
                value = historySearchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search translations...", color = Color(0xFF64748B), fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().testTag("history_search_input"),
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = primaryTextColor,
                    unfocusedTextColor = primaryTextColor,
                    unfocusedBorderColor = cardDivider,
                    focusedBorderColor = Color(0xFF9333EA)
                ),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // SCROLLABLE HISTORY ENTRIES LIST
        if (activeList.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (showFavoritesOnly) Icons.Default.BookmarkBorder else Icons.Default.HistoryToggleOff,
                    contentDescription = null,
                    tint = if (isDarkMode) Color(0xFF262C4D) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (showFavoritesOnly) "No bookmarks discovered" else "History list is empty",
                    color = secondaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (showFavoritesOnly) "Tap the Bookmark icon on any translated output to bookmark here." else "Translate sentences and words to build a history list.",
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("saved_items_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeList, key = { it.id }) { item ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Paste contents back on home screen for reuse
                                viewModel.updateInputText(item.sourceText)
                                // Trigger route change home in root container
                                onNavigateToHome("home")
                            }
                            .testTag("history_item_card_${item.id}"),
                        isDarkTheme = isDarkMode,
                        cornerRadius = 12.dp,
                        elevation = 2.dp
                    ) {
                        Column {
                            // Header of item: Languages used
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${item.sourceLangName} ➜ ${item.targetLangName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF1E40AF)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(item.category, fontSize = 9.sp, color = secondaryTextColor)
                                    }
                                }

                                Row {
                                    // Bookmark button
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(item) },
                                        modifier = Modifier.size(28.dp).testTag("bookmark_toggle_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Toggle favorite",
                                            tint = if (item.isFavorite) Color(0xFFF59E0B) else secondaryTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Direct Copy translated text
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(item.translatedText))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete history entry
                                    IconButton(
                                        onClick = { viewModel.deleteHistoryId(item.id) },
                                        modifier = Modifier.size(28.dp).testTag("history_delete_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove entry",
                                            tint = Color(0xFFF43F5E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Original text
                            Text(
                                text = item.sourceText,
                                fontSize = 14.sp,
                                color = primaryTextColor,
                                fontWeight = FontWeight.Normal,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = cardDivider)
                            Spacer(modifier = Modifier.height(4.dp))

                            // Translated text
                            Text(
                                text = item.translatedText,
                                fontSize = 15.sp,
                                color = if (isDarkMode) Color(0xFFD8B4FE) else Color(0xFF1F2937),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
