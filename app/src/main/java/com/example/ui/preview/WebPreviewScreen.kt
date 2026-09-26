package com.example.ui.preview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.WebApp
import com.example.model.WebAppVersion
import com.example.ui.components.ConsoleDrawer
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPink
import com.example.ui.theme.PrimaryIndigo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPreviewScreen(
    app: WebApp,
    versions: List<WebAppVersion>,
    consoleLogs: List<ConsoleLogEntry>,
    isGenerating: Boolean,
    generationStep: String,
    onBack: () -> Unit,
    onToggleFavorite: (WebApp) -> Unit,
    onShareProject: (WebApp) -> Unit,
    onIterate: (appId: Long, prompt: String) -> Unit,
    onSaveManualCode: (appId: Long, newHtml: String) -> Unit,
    onRevertVersion: (WebAppVersion) -> Unit,
    onAddConsoleLog: (LogLevel, String) -> Unit,
    onClearConsoleLogs: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preview, 1: Code, 2: AI Refine, 3: Versions, 4: Console
    var isFullscreen by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var editableCode by remember(app.htmlCode) { mutableStateOf(app.htmlCode) }
    var iterationPrompt by remember { mutableStateOf("") }

    val quickIterationIdeas = listOf(
        "Add dark / light mode toggle",
        "Add Web Audio click sound effects",
        "Add reset and clear buttons",
        "Make it cyberpunk theme with neon glow",
        "Save state in localStorage so progress persists",
        "Add a particle explosion animation"
    )

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = app.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "v${app.versionCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = app.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.testTag("reload_webview_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        IconButton(
                            onClick = { onShareProject(app) },
                            modifier = Modifier.testTag("share_project_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Project")
                        }
                        IconButton(onClick = { onToggleFavorite(app) }) {
                            Icon(
                                imageVector = if (app.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (app.isFavorite) AccentPink else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { isFullscreen = !isFullscreen }) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
                .imePadding()
        ) {
            // Tabs Bar (hidden in Fullscreen mode)
            if (!isFullscreen) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Preview", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            editableCode = app.htmlCode
                            selectedTab = 1
                        },
                        icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Code", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Iterate", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Revisions (${versions.size})", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text("Console", fontSize = 12.sp) }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Live WebView Preview
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppWebView(
                                htmlCode = app.htmlCode,
                                onWebViewCreated = { webViewInstance = it },
                                onConsoleMessage = { level, msg ->
                                    onAddConsoleLog(level, msg)
                                }
                            )

                            if (isFullscreen) {
                                IconButton(
                                    onClick = { isFullscreen = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Code Editor Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D1117))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HTML5 Standalone Bundle (${editableCode.length} chars)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF8B949E),
                                    fontFamily = FontFamily.Monospace
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onShareProject(app) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("code_share_button")
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            onSaveManualCode(app.id, editableCode)
                                            selectedTab = 0
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("apply_code_button")
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply & Run")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editableCode,
                                onValueChange = { editableCode = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("html_code_editor"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF161B22),
                                    unfocusedContainerColor = Color(0xFF161B22),
                                    focusedTextColor = Color(0xFFC9D1D9),
                                    unfocusedTextColor = Color(0xFFC9D1D9),
                                    cursorColor = AccentCyan,
                                    focusedBorderColor = AccentCyan.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color(0xFF30363D)
                                ),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    2 -> {
                        // AI Iterate & Refine Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = PrimaryIndigo,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Iterate with AI Prompt",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Tell the AI what changes, new controls, features, or design styles to add to '${app.title}'.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = iterationPrompt,
                                            onValueChange = { iterationPrompt = it },
                                            placeholder = { Text("e.g., 'Add a high score counter and a restart button with confetti'...") },
                                            minLines = 3,
                                            maxLines = 5,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("iterate_prompt_field")
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (isGenerating) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = PrimaryIndigo
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = generationStep,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = PrimaryIndigo
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (iterationPrompt.isNotBlank()) {
                                                        onIterate(app.id, iterationPrompt.trim())
                                                        iterationPrompt = ""
                                                        selectedTab = 0
                                                    }
                                                },
                                                enabled = iterationPrompt.isNotBlank(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("submit_iteration_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "Update Web Application",
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "1-Tap Quick Modification Ideas:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(quickIterationIdeas) { idea ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            iterationPrompt = idea
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = PrimaryIndigo,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = idea,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Revisions History Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "Version History (${versions.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Every iteration is saved. Revert to any prior snapshot at any time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            items(versions) { ver ->
                                VersionItemCard(
                                    version = ver,
                                    isCurrent = ver.versionNumber == app.versionCount,
                                    onRevert = {
                                        onRevertVersion(ver)
                                        selectedTab = 0
                                    }
                                )
                            }
                        }
                    }

                    4 -> {
                        // DevTools Console Tab
                        ConsoleDrawer(
                            logs = consoleLogs,
                            onClearLogs = onClearConsoleLogs,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VersionItemCard(
    version: WebAppVersion,
    isCurrent: Boolean,
    onRevert: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                PrimaryIndigo.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrent) Modifier.border(1.dp, PrimaryIndigo, RoundedCornerShape(12.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isCurrent) PrimaryIndigo else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "v${version.versionNumber}" + if (isCurrent) " (Current)" else "",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = version.prompt,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateFormat.format(Date(version.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCurrent) {
                IconButton(
                    onClick = onRevert,
                    modifier = Modifier.testTag("revert_version_${version.versionNumber}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Revert to this version",
                        tint = PrimaryIndigo
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AppWebView(
    htmlCode: String,
    onWebViewCreated: (WebView) -> Unit,
    onConsoleMessage: (LogLevel, String) -> Unit
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    allowFileAccess = true
                    mediaPlaybackRequiresUserGesture = false
                }

                // Inject Console Interceptor
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun log(level: String, msg: String) {
                            val logLevel = when (level.lowercase()) {
                                "error" -> LogLevel.ERROR
                                "warn" -> LogLevel.WARN
                                "info" -> LogLevel.INFO
                                else -> LogLevel.LOG
                            }
                            onConsoleMessage(logLevel, msg)
                        }
                    },
                    "AndroidConsole"
                )

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            val level = when (it.messageLevel()) {
                                ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARN
                                ConsoleMessage.MessageLevel.LOG -> LogLevel.LOG
                                else -> LogLevel.INFO
                            }
                            onConsoleMessage(level, "${it.message()} (line ${it.lineNumber()})")
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Hook console.log to Android bridge
                        view?.evaluateJavascript(
                            """
                            (function() {
                                var origLog = console.log;
                                var origWarn = console.warn;
                                var origErr = console.error;
                                var origInfo = console.info;
                                console.log = function() {
                                    var msg = Array.from(arguments).map(String).join(' ');
                                    if (window.AndroidConsole) window.AndroidConsole.log('log', msg);
                                    origLog.apply(console, arguments);
                                };
                                console.warn = function() {
                                    var msg = Array.from(arguments).map(String).join(' ');
                                    if (window.AndroidConsole) window.AndroidConsole.log('warn', msg);
                                    origWarn.apply(console, arguments);
                                };
                                console.error = function() {
                                    var msg = Array.from(arguments).map(String).join(' ');
                                    if (window.AndroidConsole) window.AndroidConsole.log('error', msg);
                                    origErr.apply(console, arguments);
                                };
                                console.info = function() {
                                    var msg = Array.from(arguments).map(String).join(' ');
                                    if (window.AndroidConsole) window.AndroidConsole.log('info', msg);
                                    origInfo.apply(console, arguments);
                                };
                            })();
                            """.trimIndent(),
                            null
                        )
                    }
                }

                onWebViewCreated(this)
                loadDataWithBaseURL("https://ailuna.local", htmlCode, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://ailuna.local", htmlCode, "text/html", "UTF-8", null)
        },
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_webview")
    )
}

fun shareHtml(context: Context, title: String, htmlCode: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_SUBJECT, "$title (AI Luna)")
        putExtra(Intent.EXTRA_TEXT, htmlCode)
        type = "text/html"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Web App HTML")
    context.startActivity(shareIntent)
}
