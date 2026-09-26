package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.WebApp
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.ImportProjectDialog
import com.example.ui.components.ShareProjectDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.library.AppLibraryScreen
import com.example.ui.preview.WebPreviewScreen
import com.example.ui.studio.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.ProjectShareManager

enum class AppNavScreen {
    STUDIO,
    PREVIEW,
    LIBRARY
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val config = ProjectShareManager.parseFromUri(uri)
        if (config != null) {
            viewModel.setPendingImport(config)
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userApps by viewModel.userApps.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val currentVersions by viewModel.currentVersions.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationStep by viewModel.generationStep.collectAsStateWithLifecycle()
    val generationError by viewModel.generationError.collectAsStateWithLifecycle()

    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val pendingImportConfig by viewModel.pendingImportConfig.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppNavScreen.STUDIO) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var sharingApp by remember { mutableStateOf<WebApp?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    // If selectedApp becomes non-null, navigate to Preview
    val onAppSelected: (WebApp) -> Unit = { app ->
        viewModel.selectApp(app)
        currentScreen = AppNavScreen.PREVIEW
    }

    if (showProfileDialog) {
        UserProfileDialog(
            user = currentUser,
            appCount = userApps.size,
            favoriteCount = favoriteApps.size,
            onDismiss = { showProfileDialog = false },
            onLogout = {
                viewModel.logOut {
                    currentScreen = AppNavScreen.STUDIO
                }
            }
        )
    }

    if (showSettingsDialog) {
        val hasEnvKey = try {
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
        } catch (e: Throwable) {
            false
        }

        ApiKeyDialog(
            currentCustomKey = viewModel.sessionManager.getCustomApiKey(),
            currentModel = viewModel.sessionManager.getPreferredModel(),
            hasEnvKey = hasEnvKey,
            onSaveKey = { viewModel.sessionManager.setCustomApiKey(it) },
            onSelectModel = { viewModel.sessionManager.setPreferredModel(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (sharingApp != null) {
        ShareProjectDialog(
            app = sharingApp!!,
            authorName = currentUser?.displayName ?: "AI Luna Creator",
            onDismiss = { sharingApp = null }
        )
    }

    if (showImportDialog || pendingImportConfig != null) {
        ImportProjectDialog(
            preloadedConfig = pendingImportConfig,
            onImport = { config ->
                viewModel.importSharedProject(config) { importedApp ->
                    viewModel.selectApp(importedApp)
                    currentScreen = AppNavScreen.PREVIEW
                }
            },
            onDismiss = {
                showImportDialog = false
                viewModel.setPendingImport(null)
            }
        )
    }

    AnimatedContent(
        targetState = currentUser != null,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "auth_transition",
        modifier = Modifier.fillMaxSize()
    ) { isLoggedIn ->
        if (!isLoggedIn) {
            AuthScreen(
                isLoading = isAuthLoading,
                errorMessage = authError,
                onClearError = { viewModel.clearAuthError() },
                onLogIn = { email, pass ->
                    viewModel.logIn(email, pass) {
                        currentScreen = AppNavScreen.STUDIO
                    }
                },
                onSignUp = { email, pass, name ->
                    viewModel.signUp(email, pass, name) {
                        currentScreen = AppNavScreen.STUDIO
                    }
                },
                onGuestLogin = {
                    viewModel.logInAsGuest {
                        currentScreen = AppNavScreen.STUDIO
                    }
                }
            )
        } else {
            when (currentScreen) {
                AppNavScreen.STUDIO -> {
                    StudioScreen(
                        currentUser = currentUser,
                        userApps = userApps,
                        isGenerating = isGenerating,
                        generationStep = generationStep,
                        generationError = generationError,
                        onClearError = { viewModel.clearGenerationError() },
                        onGenerate = { prompt, enhancements ->
                            viewModel.generateNewApp(prompt, enhancements) { generatedApp ->
                                currentScreen = AppNavScreen.PREVIEW
                            }
                        },
                        onLoadTemplate = { template ->
                            viewModel.loadPresetTemplate(template) { app ->
                                currentScreen = AppNavScreen.PREVIEW
                            }
                        },
                        onSelectApp = onAppSelected,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onShareApp = { app -> sharingApp = app },
                        onOpenImport = { showImportDialog = true },
                        onOpenProfile = { showProfileDialog = true },
                        onOpenSettings = { showSettingsDialog = true },
                        onOpenLibrary = { currentScreen = AppNavScreen.LIBRARY }
                    )
                }

                AppNavScreen.PREVIEW -> {
                    if (selectedApp != null) {
                        WebPreviewScreen(
                            app = selectedApp!!,
                            versions = currentVersions,
                            consoleLogs = consoleLogs,
                            isGenerating = isGenerating,
                            generationStep = generationStep,
                            onBack = {
                                viewModel.selectApp(null)
                                currentScreen = AppNavScreen.STUDIO
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onShareProject = { app -> sharingApp = app },
                            onIterate = { appId, prompt ->
                                viewModel.iterateApp(appId, prompt) { }
                            },
                            onSaveManualCode = { appId, newHtml ->
                                viewModel.updateAppCodeManually(appId, newHtml) { }
                            },
                            onRevertVersion = { version ->
                                viewModel.revertToVersion(version) { }
                            },
                            onAddConsoleLog = { level, msg ->
                                viewModel.addConsoleLog(level, msg)
                            },
                            onClearConsoleLogs = { viewModel.clearConsoleLogs() }
                        )
                    } else {
                        currentScreen = AppNavScreen.STUDIO
                    }
                }

                AppNavScreen.LIBRARY -> {
                    AppLibraryScreen(
                        userApps = userApps,
                        onSelectApp = onAppSelected,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDeleteApp = { viewModel.deleteApp(it) },
                        onShareApp = { app -> sharingApp = app },
                        onOpenImport = { showImportDialog = true },
                        onBack = { currentScreen = AppNavScreen.STUDIO }
                    )
                }
            }
        }
    }
}
