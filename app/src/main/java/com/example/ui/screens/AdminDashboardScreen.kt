package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.*
import com.example.ui.components.GlassCard
import com.example.ui.viewmodel.TranslationViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardDialog(
    viewModel: TranslationViewModel,
    onDismiss: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val installations by viewModel.installations.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val feedbacksList by viewModel.feedbacks.collectAsState()
    val historyList by viewModel.historyList.collectAsState()
    val favoritesList by viewModel.favoriteList.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Audience, 1: Usage, 2: Language, 3: Health
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val primaryColor = Color(0xFF9333EA) // Royal Purple
    val scaffoldBgColor = if (isDarkMode) Color(0xFF0F111E) else Color(0xFFF8FAFC)
    val cardBgColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    // Calculate Analytics Metrics
    val totalInstalls = installations.size
    val currentTime = System.currentTimeMillis()
    val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
    val activeInstalls = installations.count { currentTime - it.lastActiveTimestamp <= sevenDaysMs }
    val activeRate = if (totalInstalls > 0) (activeInstalls.toFloat() / totalInstalls * 100).toInt() else 100

    // Device counts
    val manufacturerCounts = installations.groupBy { it.deviceManufacturer }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Version distribution
    val versionCounts = installations.groupBy { it.appVersion }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Android version distribution
    val androidCounts = installations.groupBy { it.androidVersion }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Filter raw logs
    val filteredLogs = installations.filter {
        it.deviceModel.contains(searchQuery, ignoreCase = true) ||
        it.deviceManufacturer.contains(searchQuery, ignoreCase = true) ||
        it.appVersion.contains(searchQuery, ignoreCase = true) ||
        it.androidVersion.contains(searchQuery, ignoreCase = true) ||
        it.installationId.contains(searchQuery, ignoreCase = true)
    }

    // Consolidated secure email sending matching requirements
    fun sendConsolidatedDiagnosticsEmail() {
        if (diagnostics.isEmpty() && feedbacksList.isEmpty()) {
            android.widget.Toast.makeText(context, "No diagnostics telemetry or user feedback recorded locally.", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val pendingLogs = diagnostics.filter { !it.isMailNotificationSent }

        val emailBody = StringBuilder().apply {
            append("=================================================================\n")
            append("             ANHA.AI SECURE DIAGNOSTICS & TELEMETRY REPORT\n")
            append("=================================================================\n\n")
            
            append("1. ENVIRONMENT SPECIFICATIONS:\n")
            append("- Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            append("- Device Manufacturer: ${android.os.Build.MANUFACTURER}\n")
            append("- Device Model: ${android.os.Build.MODEL}\n")
            append("- Android OS version: ${android.os.Build.VERSION.RELEASE}\n")
            append("- App Build Version: v${viewModel.getAppVersionName()}\n\n")

            append("2. LOCAL USAGE ANALYTICS:\n")
            append("- Total App Launch Sessions: ${viewModel.getUsageSessionCount()}\n")
            append("- Device Cumulative Active Duration: ${viewModel.getUsageTotalTime() / 1000} seconds\n")
            append("- Local First Open Timestamp: ${if (viewModel.getUsageFirstOpen() > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(viewModel.getUsageFirstOpen())) else "N/A"}\n")
            append("- Local Last Open Timestamp: ${if (viewModel.getUsageLastOpen() > 0) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(viewModel.getUsageLastOpen())) else "N/A"}\n\n")

            append("3. USER FEEDBACK ENTRIES (${feedbacksList.size}):\n")
            append("-----------------------------------------------------------------\n")
            feedbacksList.forEachIndexed { index, feedback ->
                val fbDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(feedback.timestamp))
                append("[FEEDBACK #${index + 1}] | $fbDate\n")
                append("- USER COMMENT: '${feedback.comment}'\n")
                append("- LANGUAGES: ${feedback.languages}\n")
                append("- REFERENCE CONTEXT: '${feedback.contextText}'\n")
                if (!feedback.errorDetails.isNullOrBlank()) {
                    append("- HOST ERRORS DETECTED: ${feedback.errorDetails}\n")
                }
                append("- DEVICE SPEC: ${feedback.deviceManufacturer} ${feedback.deviceModel} (Android ${feedback.androidVersion})\n")
                append("-----------------------------------------------------------------\n")
            }
            append("\n")

            append("4. PENDING SECURE SYSTEM TELEMETRY EVENTS (${pendingLogs.size}):\n")
            append("-----------------------------------------------------------------\n")
            
            pendingLogs.forEachIndexed { index, diag ->
                val dateStr = try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(diag.timestamp))
                } catch (e: Exception) {
                    "Unknown"
                }
                append("[REPORT EVENT #${index + 1}]\n")
                append("- EVENT TYPE:  ${diag.type}\n")
                append("- RECORD TIME: $dateStr\n")
                append("- APP VERSION: v${diag.appVersion}\n")
                append("- ANDROID OS:  Android ${diag.androidVersion}\n")
                append("- EVENT DETAILS:\n${diag.details}\n")
                append("-----------------------------------------------------------------\n")
            }
            
            append("\n5. DATA PROTECTION & DISCLOSURE:\n")
            append("No personal files, emails, passwords, precise GPS paths, messages, phone numbers, or IMEIs were collected. Diagnostics remain strictly operational, in full compliance with secure sandboxing standards.\n")
        }.toString()

        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("indo00059@gmail.com"))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Anha.ai Diagnostics Outbox & Onboarding Telemetry")
            putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
        }

        try {
            context.startActivity(android.content.Intent.createChooser(intent, "Send Diagnostics Email"))
            if (pendingLogs.isNotEmpty()) {
                viewModel.markDiagnosticsAsSent(pendingLogs.map { it.id })
            }
            android.widget.Toast.makeText(context, "Transmitting logs. Outbox status updated.", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Copy fallback
            android.widget.Toast.makeText(context, "No email client found. Diagnostics report copied to clipboard.", android.widget.Toast.LENGTH_LONG).show()
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Anha.ai Diagnostics Report", emailBody)
            clipboard.setPrimaryClip(clip)
            if (pendingLogs.isNotEmpty()) {
                viewModel.markDiagnosticsAsSent(pendingLogs.map { it.id })
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .testTag("admin_dashboard_dialog_container"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = scaffoldBgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // DIALOG HEADER BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ANHA.AI OWNER SYSTEM CENTRE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Secure Local Administrative Terminal",
                                fontSize = 10.sp,
                                color = textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_admin_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dashboard",
                            tint = textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4-TAB SYSTEM NAVIGATION BAR
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Audience", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_audience_btn")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Usage", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_usage_btn")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Language", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_language_btn")
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Health", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("tab_health_btn")
                    )
                }

                // TAB CONTENT ROUTER
                when (selectedTab) {
                    0 -> { // TAB 0: AUDIENCE
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "Total Installs",
                                        value = "$totalInstalls",
                                        subtitle = "Host registers",
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Active Installs",
                                        value = "$activeInstalls",
                                        subtitle = "Active (7d)",
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Active Rate",
                                        value = "$activeRate%",
                                        subtitle = "Core ratio",
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            // Device Manufacturer Chart
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PhoneAndroid, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Device Manufacturer Statistics", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (manufacturerCounts.isEmpty()) {
                                            Text("No device metadata recorded yet.", fontSize = 12.sp, color = textSecondary)
                                        } else {
                                            manufacturerCounts.forEachIndexed { index, pair ->
                                                val pct = if (totalInstalls > 0) pair.second.toFloat() / totalInstalls else 0f
                                                val color = when (index % 5) {
                                                    0 -> Color(0xFF9333EA)
                                                    1 -> Color(0xFF3B82F6)
                                                    2 -> Color(0xFF10B981)
                                                    3 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                DistributionLine(
                                                    label = pair.first,
                                                    count = pair.second,
                                                    percentage = pct,
                                                    color = color,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // App Version Distribution
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("App Version Distribution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (versionCounts.isEmpty()) {
                                            Text("No versions recorded yet.", fontSize = 12.sp, color = textSecondary)
                                        } else {
                                            versionCounts.forEachIndexed { index, pair ->
                                                val pct = if (totalInstalls > 0) pair.second.toFloat() / totalInstalls else 0f
                                                val color = when (index % 5) {
                                                    0 -> Color(0xFF10B981)
                                                    1 -> Color(0xFF3B82F6)
                                                    2 -> Color(0xFF9333EA)
                                                    3 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                DistributionLine(
                                                    label = "v${pair.first}",
                                                    count = pair.second,
                                                    percentage = pct,
                                                    color = color,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Android Version Distributions
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Build, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Android OS Version Telemetry", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (androidCounts.isEmpty()) {
                                            Text("No OS metadata recorded yet.", fontSize = 12.sp, color = textSecondary)
                                        } else {
                                            androidCounts.forEachIndexed { index, pair ->
                                                val pct = if (totalInstalls > 0) pair.second.toFloat() / totalInstalls else 0f
                                                val color = when (index % 5) {
                                                    0 -> Color(0xFFF59E0B)
                                                    1 -> Color(0xFF9333EA)
                                                    2 -> Color(0xFF10B981)
                                                    3 -> Color(0xFF3B82F6)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                DistributionLine(
                                                    label = "Android ${pair.first}",
                                                    count = pair.second,
                                                    percentage = pct,
                                                    color = color,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // SEARCH & INSTALL LOGS
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "RAW SECURE INSTALL LOGS (${filteredLogs.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textSecondary) },
                                        placeholder = { Text("Filter logs by device, OS, or version...", fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth().testTag("install_search_bar"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = dividerColor
                                        ),
                                        singleLine = true
                                    )
                                }
                            }

                            if (filteredLogs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(cardBgColor, shape = RoundedCornerShape(8.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No matching record found.", fontSize = 12.sp, color = textSecondary)
                                    }
                                }
                            } else {
                                items(filteredLogs) { log ->
                                    InstallLogItem(
                                        log = log,
                                        isDarkMode = isDarkMode,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        dividerColor = dividerColor
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // TAB 1: USAGE & SESSIONS
                        val firstOpen = viewModel.getUsageFirstOpen()
                        val lastOpen = viewModel.getUsageLastOpen()
                        val sessionCount = viewModel.getUsageSessionCount()
                        val elapsedSeconds by viewModel.currentSessionDurationSec.collectAsState()
                        val totalUsageSec = (viewModel.getUsageTotalTime() / 1000) + elapsedSeconds

                        val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                        fun formatTimeSec(seconds: Long): String {
                            val h = seconds / 3600
                            val m = (seconds % 3600) / 60
                            val s = seconds % 60
                            return if (h > 0) "${h}h ${m}m ${s}s" else "${m}m ${s}s"
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "Session Count",
                                        value = "$sessionCount",
                                        subtitle = "App launches",
                                        color = Color(0xFF9333EA),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Active Clock",
                                        value = formatTimeSec(elapsedSeconds),
                                        subtitle = "Current session",
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Total Time",
                                        value = formatTimeSec(totalUsageSec),
                                        subtitle = "Cumulative usage",
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Timeline, null, tint = Color(0xFF9333EA), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "LOCAL USAGE STATISTICS",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                        HorizontalDivider(color = dividerColor)
                                        
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("First App Open Launch", fontSize = 11.sp, color = textSecondary)
                                                Text(
                                                    text = if (firstOpen > 0) sdf.format(Date(firstOpen)) else "Pending Init",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Last App Open Launch", fontSize = 11.sp, color = textSecondary)
                                                Text(
                                                    text = if (lastOpen > 0) sdf.format(Date(lastOpen)) else "Pending Init",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Average Session Duration", fontSize = 11.sp, color = textSecondary)
                                                val avgSec = if (sessionCount > 0) totalUsageSec / sessionCount else 0L
                                                Text(
                                                    text = formatTimeSec(avgSec),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textPrimary
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Retention Interval Category", fontSize = 11.sp, color = textSecondary)
                                                val catText = when {
                                                    sessionCount >= 10 -> "Power User \uD83D\uDD25"
                                                    sessionCount >= 4 -> "Regular User \uD83D\uDE80"
                                                    else -> "Onboarding Phase \uD83C\uDF1F"
                                                }
                                                Text(
                                                    text = catText,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.05f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "ℹ\uFE0F REAL-TIME TELEMETRY TRACKER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Anha.ai increments the cumulative local usage duration periodically automatically. This stopwatch represents active thread engagement on the current hardware device.",
                                            fontSize = 10.sp,
                                            color = textSecondary,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // TAB 2: TRANSLATIONS
                        val totalTranslations = historyList.size
                        val totalFavorites = favoritesList.size

                        // Compute Top Languages
                        val topLanguages = historyList.flatMap { listOf(it.sourceLangName, it.targetLangName) }
                            .filter { !it.isNullOrBlank() }
                            .groupBy { it }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)
                            
                        // Compute Top Translation Directions
                        val topDirections = historyList.map { "${it.sourceLangName} → ${it.targetLangName}" }
                            .filter { !it.contains("null") }
                            .groupBy { it }
                            .mapValues { it.value.size }
                            .toList()
                            .sortedByDescending { it.second }
                            .take(5)

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "Translations",
                                        value = "$totalTranslations",
                                        subtitle = "Jobs executed",
                                        color = Color(0xFF3B82F6),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Favorites",
                                        value = "$totalFavorites",
                                        subtitle = "Starred items",
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Directions",
                                        value = "${topDirections.size}",
                                        subtitle = "Unique arcs",
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            // Top Languages Distribution
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Translate, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Most Engaged Languages", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (topLanguages.isEmpty()) {
                                            Text("No translated history recorded. Run translations to track logs.", fontSize = 12.sp, color = textSecondary)
                                        } else {
                                            val maxL = topLanguages.maxOfOrNull { it.second } ?: 1
                                            topLanguages.forEachIndexed { index, pair ->
                                                val pct = pair.second.toFloat() / maxL
                                                val color = when (index % 5) {
                                                    0 -> Color(0xFF3B82F6)
                                                    1 -> Color(0xFF10B981)
                                                    2 -> Color(0xFF9333EA)
                                                    3 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                DistributionLine(
                                                    label = pair.first,
                                                    count = pair.second,
                                                    percentage = pct,
                                                    color = color,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Top Translation Directions
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Send, null, tint = Color(0xFF9333EA), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Top Translation Directions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (topDirections.isEmpty()) {
                                            Text("No directions generated yet.", fontSize = 12.sp, color = textSecondary)
                                        } else {
                                            val maxD = topDirections.maxOfOrNull { it.second } ?: 1
                                            topDirections.forEachIndexed { index, pair ->
                                                val pct = pair.second.toFloat() / maxD
                                                val color = when (index % 5) {
                                                    0 -> Color(0xFF9333EA)
                                                    1 -> Color(0xFF3B82F6)
                                                    2 -> Color(0xFF10B981)
                                                    3 -> Color(0xFFF59E0B)
                                                    else -> Color(0xFFEF4444)
                                                }
                                                DistributionLine(
                                                    label = pair.first,
                                                    count = pair.second,
                                                    percentage = pct,
                                                    color = color,
                                                    textPrimary = textPrimary,
                                                    textSecondary = textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // TAB 3: HEALTH, FAILS & FEEDBACK Inbox
                        val crashCount = diagnostics.count { it.type == "CRASH_REPORT" || it.details.contains("Crash") }
                        val transFailedCount = diagnostics.count { it.type == "TRANSLATION_FAILURE" || it.details.contains("time out") }
                        val sysErrorCount = diagnostics.count { it.type == "SYSTEM_ERROR" || it.details.contains("fail") }
                        val pendingList = diagnostics.filter { !it.isMailNotificationSent }
                        val pendingCount = pendingList.size

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // METRIC TILES
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetricCard(
                                        title = "App Crashes",
                                        value = "$crashCount",
                                        subtitle = "Trace audits",
                                        color = Color(0xFF7C3AED),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "Trans. Fails",
                                        value = "$transFailedCount",
                                        subtitle = "Timeout events",
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                    MetricCard(
                                        title = "System Errors",
                                        value = "$sysErrorCount",
                                        subtitle = "Operational faults",
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f),
                                        isDarkMode = isDarkMode
                                    )
                                }
                            }

                            // Privacy warning card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "DIAGNOSTICS & DATA PRIVACY SECURE STATEMENT",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Anha.ai does NOT collect contacts, passwords, precise locations, phone numbers, or IMEIs. Operational diagnostics are logged locally to protect user privacy.",
                                                fontSize = 9.sp,
                                                color = textSecondary,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // EMAIL TRANSMITTER CARD
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 2.dp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MailOutline, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CONSOLIDATED OWNER DISPATCH CENTRE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                        Text(
                                            text = "Transcribe analytical sessions, user diagnostics, translation failures, and raw feedbacks to the app owner (indo00059@gmail.com) over standard SMTP email client.",
                                            fontSize = 10.sp,
                                            color = textSecondary,
                                            lineHeight = 14.sp
                                        )
                                        val totalPendingLogs = pendingCount + feedbacksList.size
                                        Button(
                                            onClick = { sendConsolidatedDiagnosticsEmail() },
                                            enabled = totalPendingLogs > 0,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981),
                                                disabledContainerColor = textSecondary.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("transmit_diagnostics_email_button"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = null,
                                                tint = if (totalPendingLogs > 0) Color.White else textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (totalPendingLogs > 0) "Transmit $totalPendingLogs Logs to Owner" else "Outbox Synchronized ✅",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (totalPendingLogs > 0) Color.White else textSecondary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // THE SIMULATION TEST PANEL
                            item {
                                GlassCard(isDarkTheme = isDarkMode, elevation = 1.dp) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Share, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "INTEGRITY TEST CORES",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimary
                                            )
                                        }
                                        Text(
                                            text = "Trigger simulated processes to verify crash safety notifications or re-seed entire datasets.",
                                            fontSize = 10.sp,
                                            color = textSecondary,
                                            lineHeight = 13.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.triggerSimulatedUpdateLog("1.4.1", 4, "1.5.0", 5) },
                                                modifier = Modifier.weight(1f).testTag("sim_update_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(6.dp)
                                            ) {
                                                Text("Update", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            Button(
                                                onClick = { viewModel.simulateCrash("RuntimeException: Thread local index pointer overflow exception triggered from Test Deck") },
                                                modifier = Modifier.weight(1f).testTag("sim_crash_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(6.dp)
                                            ) {
                                                Text("Crash", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        viewModel.submitFeedback(
                                                            comment = "Simulated API issue: local HTTP request payload was truncated.",
                                                            languages = "English -> German",
                                                            contextText = "Safe journey",
                                                            appVersion = viewModel.getAppVersionName(),
                                                            androidVersion = android.os.Build.VERSION.RELEASE,
                                                            deviceModel = android.os.Build.MODEL,
                                                            deviceManufacturer = android.os.Build.MANUFACTURER,
                                                            errorDetails = "Network socket timeout. Code: 504 Gateway Timeout"
                                                        )
                                                        viewModel.simulateCrash("SocketTimeoutException: Connection closed active handshake timeout.")
                                                    }
                                                },
                                                modifier = Modifier.weight(1.1f).testTag("sim_failure_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(6.dp)
                                            ) {
                                                Text("API Fail", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { viewModel.clearAllInstallations() },
                                                modifier = Modifier.testTag("reset_analytics_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Reset & Seed SandboxCenter", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            TextButton(
                                                onClick = { viewModel.clearDiagnostics() },
                                                modifier = Modifier.testTag("clear_diagnostics_button")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFEF4444))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Wipe Log", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // USER FEEDBACK INBOX SECTION
                            item {
                                Text(
                                    text = "USER FEEDBACK INBOX (${feedbacksList.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            if (feedbacksList.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(cardBgColor, shape = RoundedCornerShape(8.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No user feedbacks logged locally.", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                            } else {
                                items(feedbacksList) { feedback ->
                                    FeedbackInboxItem(
                                        feedback = feedback,
                                        isDarkMode = isDarkMode,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        dividerColor = dividerColor
                                    )
                                }
                            }

                            // HISTORICAL EVENT LOGS
                            item {
                                Text(
                                    text = "PERSISTENT TRACE DIAGNOSTICS LOGS (${diagnostics.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            if (diagnostics.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(cardBgColor, shape = RoundedCornerShape(8.dp))
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No health diagnostic entries generated.", fontSize = 11.sp, color = textSecondary)
                                    }
                                }
                            } else {
                                items(diagnostics) { diag ->
                                    DiagnosticLogItem(
                                        diag = diag,
                                        isDarkMode = isDarkMode,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        dividerColor = dividerColor
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

@Composable
fun FeedbackInboxItem(
    feedback: FeedbackEntity,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color
) {
    val dateStr = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(feedback.timestamp))
    } catch (e: Exception) {
        "Unknown Date"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = feedback.languages,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"${feedback.comment}\"",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )

            if (!feedback.contextText.isBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref Context: '${feedback.contextText}'",
                    fontSize = 10.sp,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!feedback.errorDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.08f), shape = RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "Captured Handshake Error:\n${feedback.errorDetails}",
                        fontSize = 9.sp,
                        color = Color(0xFFEF4444),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Device: ${feedback.deviceManufacturer} ${feedback.deviceModel} | App v${feedback.appVersion}",
                fontSize = 9.sp,
                color = textSecondary
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    val bg = if (isDarkMode) Color(0xFF1E293B) else Color.White
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DistributionLine(
    label: String,
    count: Int,
    percentage: Float,
    color: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count (${(percentage * 100).toInt()}%)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(2.5.dp))
                .background(if (percentage > 0f) color.copy(alpha = 0.12f) else android.graphics.Color.TRANSPARENT.let { Color.Transparent })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percentage.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun InstallLogItem(
    log: com.example.data.db.InstallationEntity,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color
) {
    val dateStr = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.installTimestamp))
    } catch (e: Exception) {
        "Unknown Date"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (log.installationId.startsWith("demo")) Color(0xFF64748B) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${log.deviceManufacturer} ${log.deviceModel}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Text(
                    text = "v${log.appVersion}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9333EA),
                    modifier = Modifier
                        .background(Color(0xFF9333EA).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OS: Android ${log.androidVersion}",
                        fontSize = 10.sp,
                        color = textSecondary
                    )
                    Text(
                        text = "ID: ${log.installationId.take(12)}...",
                        fontSize = 10.sp,
                        color = textSecondary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }
        }
    }
}

@Composable
fun DiagnosticLogItem(
    diag: com.example.data.db.DiagnosticsEntity,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    dividerColor: Color
) {
    val dateStr = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(diag.timestamp))
    } catch (e: Exception) {
        "Unknown Date"
    }

    val typeColor = when (diag.type) {
        "FIRST_LAUNCH" -> Color(0xFF10B981) // Emerald
        "APP_UPDATE" -> Color(0xFF3B82F6) // Blue
        "TRANSLATION_FAILURE" -> Color(0xFFEF4444) // Red
        "CRASH_REPORT" -> Color(0xFF7C3AED) // Purple
        else -> Color(0xFFF59E0B) // Amber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = diag.type,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (diag.isMailNotificationSent) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (diag.isMailNotificationSent) "Emailed ✅" else "Pending Outbox",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diag.isMailNotificationSent) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = diag.details,
                fontSize = 11.sp,
                color = textPrimary,
                fontFamily = if (diag.type == "CRASH_REPORT") androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default,
                maxLines = if (diag.type == "CRASH_REPORT") 10 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = dividerColor)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App v${diag.appVersion} | OS ${diag.androidVersion}",
                    fontSize = 9.sp,
                    color = textSecondary
                )
                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    color = textSecondary
                )
            }
        }
    }
}
