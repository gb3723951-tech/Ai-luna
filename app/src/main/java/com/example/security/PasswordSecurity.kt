package com.example.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val calculatedHash = hashPassword(password, salt)
        return calculatedHash == expectedHash
    }

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }

    fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        var hasDigit = false
        var hasUpper = false
        var hasLower = false
        var hasSpecial = false

        for (c in password) {
            when {
                c.isDigit() -> hasDigit = true
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                !c.isLetterOrDigit() -> hasSpecial = true
            }
        }

        val criteriaMet = (if (hasDigit) 1 else 0) +
                (if (hasUpper) 1 else 0) +
                (if (hasLower) 1 else 0) +
                (if (hasSpecial) 1 else 0)

        return when {
            password.length >= 8 && criteriaMet >= 3 -> PasswordStrength.STRONG
            password.length >= 6 && criteriaMet >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
