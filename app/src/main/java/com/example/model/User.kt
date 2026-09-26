package com.example.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val salt: String,
    val avatarColorHex: String = "#6366F1",
    val createdAt: Long = System.currentTimeMillis()
)
