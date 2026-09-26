package com.example.data

import com.example.model.User
import com.example.security.PasswordSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    val currentUserIdFlow = sessionManager.activeUserIdFlow

    val currentUserFlow: Flow<User?> = currentUserIdFlow.flatMapLatest { userId ->
        when {
            userId == null -> flowOf(null)
            userId == SessionManager.GUEST_USER_ID -> flowOf(
                User(
                    id = SessionManager.GUEST_USER_ID,
                    email = "guest@ailuna.ai",
                    displayName = "Guest Creator",
                    passwordHash = "",
                    salt = "",
                    avatarColorHex = "#10B981"
                )
            )
            else -> userDao.getUserById(userId)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()

        if (!isValidEmail(trimmedEmail)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }

        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters long"))
        }

        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your display name"))
        }

        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalStateException("An account with this email already exists"))
        }

        val salt = PasswordSecurity.generateSalt()
        val passwordHash = PasswordSecurity.hashPassword(password, salt)
        val avatarColors = listOf("#6366F1", "#EC4899", "#8B5CF6", "#06B6D4", "#10B981", "#F59E0B")
        val avatarColor = avatarColors[kotlin.math.abs(trimmedEmail.hashCode()) % avatarColors.size]

        val user = User(
            email = trimmedEmail,
            displayName = trimmedName,
            passwordHash = passwordHash,
            salt = salt,
            avatarColorHex = avatarColor
        )

        try {
            val insertedId = userDao.insertUser(user)
            val createdUser = user.copy(id = insertedId)
            sessionManager.loginUser(insertedId)
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Email and password are required"))
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("No account found with this email"))

        val isValid = PasswordSecurity.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return@withContext Result.failure(IllegalArgumentException("Incorrect password. Please check your credentials"))
        }

        sessionManager.loginUser(user.id)
        Result.success(user)
    }

    fun logInAsGuest(): User {
        sessionManager.loginAsGuest()
        return User(
            id = SessionManager.GUEST_USER_ID,
            email = "guest@ailuna.ai",
            displayName = "Guest Creator",
            passwordHash = "",
            salt = "",
            avatarColorHex = "#10B981"
        )
    }

    fun logOut() {
        sessionManager.logout()
    }

    suspend fun seedDemoUserIfEmpty() = withContext(Dispatchers.IO) {
        if (userDao.getUserCount() == 0) {
            val salt = PasswordSecurity.generateSalt()
            val hash = PasswordSecurity.hashPassword("password123", salt)
            val demoUser = User(
                email = "alex@ailuna.ai",
                displayName = "Alex Davis",
                passwordHash = hash,
                salt = salt,
                avatarColorHex = "#6366F1"
            )
            userDao.insertUser(demoUser)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
