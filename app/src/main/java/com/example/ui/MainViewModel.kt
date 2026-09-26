package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.AppDatabase
import com.example.data.AuthRepository
import com.example.data.PresetTemplates
import com.example.data.SessionManager
import com.example.data.WebAppRepository
import com.example.model.ConsoleLogEntry
import com.example.model.LogLevel
import com.example.model.TemplateApp
import com.example.model.User
import com.example.model.WebApp
import com.example.model.WebAppVersion
import com.example.util.ProjectConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    val sessionManager = SessionManager(application)
    val authRepository = AuthRepository(database.userDao(), sessionManager)
    val webAppRepository = WebAppRepository(database.webAppDao())
    val geminiService = GeminiService(customApiKeyProvider = { sessionManager.getCustomApiKey() })

    val currentUser: StateFlow<User?> = authRepository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeUserId: StateFlow<Long?> = authRepository.currentUserIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.getActiveUserId())

    val userApps: StateFlow<List<WebApp>> = activeUserId.flatMapLatest { userId ->
        if (userId != null) {
            webAppRepository.getAppsForUser(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<WebApp>> = activeUserId.flatMapLatest { userId ->
        if (userId != null) {
            webAppRepository.getFavoritesForUser(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedApp = MutableStateFlow<WebApp?>(null)
    val selectedApp: StateFlow<WebApp?> = _selectedApp.asStateFlow()

    val currentVersions: StateFlow<List<WebAppVersion>> = _selectedApp.flatMapLatest { app ->
        if (app != null) {
            webAppRepository.getVersionsForApp(app.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Generation state
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationStep = MutableStateFlow("")
    val generationStep: StateFlow<String> = _generationStep.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // Console logs for active preview
    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    // Auth screen feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.seedDemoUserIfEmpty()
            val userId = sessionManager.getActiveUserId()
            if (userId != null) {
                webAppRepository.seedPresetIfEmpty(userId)
            }
        }
    }

    fun selectApp(app: WebApp?) {
        _selectedApp.value = app
        _consoleLogs.value = emptyList()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearGenerationError() {
        _generationError.value = null
    }

    fun signUp(email: String, password: String, displayName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.signUp(email, password, displayName)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    webAppRepository.seedPresetIfEmpty(user.id)
                    onSuccess()
                },
                onFailure = { error ->
                    _authError.value = error.message ?: "Registration failed. Please try again."
                }
            )
        }
    }

    fun logIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            val result = authRepository.logIn(email, password)
            _isAuthLoading.value = false
            result.fold(
                onSuccess = { user ->
                    webAppRepository.seedPresetIfEmpty(user.id)
                    onSuccess()
                },
                onFailure = { error ->
                    _authError.value = error.message ?: "Login failed. Please check your credentials."
                }
            )
        }
    }

    fun logInAsGuest(onSuccess: () -> Unit) {
        val guest = authRepository.logInAsGuest()
        viewModelScope.launch {
            webAppRepository.seedPresetIfEmpty(guest.id)
            onSuccess()
        }
    }

    fun logOut(onLoggedOut: () -> Unit) {
        authRepository.logOut()
        _selectedApp.value = null
        _consoleLogs.value = emptyList()
        onLoggedOut()
    }

    fun generateNewApp(
        prompt: String,
        enhancements: List<String> = emptyList(),
        model: String = sessionManager.getPreferredModel(),
        onSuccess: (WebApp) -> Unit
    ) {
        val userId = activeUserId.value ?: SessionManager.GUEST_USER_ID
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationStep.value = "Analyzing prompt & architecture..."

            val progressSteps = listOf(
                "Designing responsive HTML5 layout...",
                "Crafting custom CSS & sleek themes...",
                "Implementing interactive JavaScript logic...",
                "Finalizing standalone application..."
            )

            val progressJob = launch {
                for (step in progressSteps) {
                    delay(1200)
                    _generationStep.value = step
                }
            }

            val result = geminiService.generateWebApp(
                userPrompt = prompt,
                modelName = model,
                enhancements = enhancements
            )

            progressJob.cancel()
            _isGenerating.value = false

            result.fold(
                onSuccess = { genResult ->
                    val app = webAppRepository.createWebApp(
                        userId = userId,
                        title = genResult.title,
                        prompt = prompt,
                        description = genResult.description,
                        htmlCode = genResult.htmlCode,
                        category = genResult.category
                    )
                    _selectedApp.value = app
                    onSuccess(app)
                },
                onFailure = { error ->
                    _generationError.value = error.message ?: "Failed to generate application."
                }
            )
        }
    }

    fun loadPresetTemplate(template: TemplateApp, onSuccess: (WebApp) -> Unit) {
        val userId = activeUserId.value ?: SessionManager.GUEST_USER_ID
        viewModelScope.launch {
            val app = webAppRepository.createWebApp(
                userId = userId,
                title = template.title,
                prompt = template.prompt,
                description = template.description,
                htmlCode = template.prebuiltHtml,
                category = template.category
            )
            _selectedApp.value = app
            onSuccess(app)
        }
    }

    fun iterateApp(
        appId: Long,
        modificationPrompt: String,
        model: String = sessionManager.getPreferredModel(),
        onSuccess: (WebApp) -> Unit
    ) {
        val current = _selectedApp.value ?: return
        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null
            _generationStep.value = "Updating web app with '$modificationPrompt'..."

            val result = geminiService.iterateWebApp(
                currentHtml = current.htmlCode,
                modificationPrompt = modificationPrompt,
                modelName = model
            )

            _isGenerating.value = false

            result.fold(
                onSuccess = { genResult ->
                    val updated = webAppRepository.updateWebAppCode(
                        appId = appId,
                        newHtmlCode = genResult.htmlCode,
                        changePrompt = modificationPrompt,
                        newTitle = genResult.title
                    )
                    if (updated != null) {
                        _selectedApp.value = updated
                        onSuccess(updated)
                    }
                },
                onFailure = { error ->
                    _generationError.value = error.message ?: "Failed to update application."
                }
            )
        }
    }

    fun updateAppCodeManually(appId: Long, newHtml: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val updated = webAppRepository.updateWebAppCode(
                appId = appId,
                newHtmlCode = newHtml,
                changePrompt = "Manual code edit"
            )
            if (updated != null) {
                _selectedApp.value = updated
                onSuccess()
            }
        }
    }

    fun revertToVersion(version: WebAppVersion, onSuccess: () -> Unit) {
        val current = _selectedApp.value ?: return
        viewModelScope.launch {
            val updated = webAppRepository.revertToVersion(current.id, version)
            if (updated != null) {
                _selectedApp.value = updated
                onSuccess()
            }
        }
    }

    fun toggleFavorite(app: WebApp) {
        viewModelScope.launch {
            webAppRepository.toggleFavorite(app)
            if (_selectedApp.value?.id == app.id) {
                _selectedApp.value = _selectedApp.value?.copy(isFavorite = !app.isFavorite)
            }
        }
    }

    fun deleteApp(app: WebApp) {
        viewModelScope.launch {
            webAppRepository.deleteApp(app)
            if (_selectedApp.value?.id == app.id) {
                _selectedApp.value = null
            }
        }
    }

    // Pending imported project (from deep link or paste)
    private val _pendingImportConfig = MutableStateFlow<ProjectConfig?>(null)
    val pendingImportConfig: StateFlow<ProjectConfig?> = _pendingImportConfig.asStateFlow()

    fun setPendingImport(config: ProjectConfig?) {
        _pendingImportConfig.value = config
    }

    fun importSharedProject(config: ProjectConfig, onSuccess: (WebApp) -> Unit) {
        val userId = activeUserId.value ?: SessionManager.GUEST_USER_ID
        viewModelScope.launch {
            val app = webAppRepository.importWebApp(
                userId = userId,
                title = config.title,
                prompt = config.prompt,
                description = config.description,
                htmlCode = config.htmlCode,
                category = config.category,
                authorName = config.authorName
            )
            _selectedApp.value = app
            _pendingImportConfig.value = null
            onSuccess(app)
        }
    }

    fun addConsoleLog(level: LogLevel, message: String) {
        val entry = ConsoleLogEntry(level = level, message = message)
        _consoleLogs.value = (_consoleLogs.value + entry).takeLast(100)
    }

    fun clearConsoleLogs() {
        _consoleLogs.value = emptyList()
    }
}
