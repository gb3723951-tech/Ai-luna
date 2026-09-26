package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("webcraft_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"
        private const val KEY_PREFERRED_MODEL = "preferred_model"
        const val GUEST_USER_ID = 0L
    }

    private val _activeUserIdFlow = MutableStateFlow(getActiveUserId())
    val activeUserIdFlow: StateFlow<Long?> = _activeUserIdFlow.asStateFlow()

    fun getActiveUserId(): Long? {
        val id = prefs.getLong(KEY_ACTIVE_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun isGuest(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun loginUser(userId: Long) {
        prefs.edit()
            .putLong(KEY_ACTIVE_USER_ID, userId)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
        _activeUserIdFlow.value = userId
    }

    fun loginAsGuest() {
        prefs.edit()
            .putLong(KEY_ACTIVE_USER_ID, GUEST_USER_ID)
            .putBoolean(KEY_IS_GUEST, true)
            .apply()
        _activeUserIdFlow.value = GUEST_USER_ID
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_ACTIVE_USER_ID)
            .putBoolean(KEY_IS_GUEST, false)
            .apply()
        _activeUserIdFlow.value = null
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun getPreferredModel(): String {
        return prefs.getString(KEY_PREFERRED_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
    }

    fun setPreferredModel(model: String) {
        prefs.edit().putString(KEY_PREFERRED_MODEL, model).apply()
    }
}
