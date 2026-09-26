package com.example.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "web_apps",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["updatedAt"])
    ]
)
data class WebApp(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long, // 0 for guest, or specific User.id
    val title: String,
    val prompt: String,
    val description: String,
    val htmlCode: String,
    val category: String = "Utility",
    val isFavorite: Boolean = false,
    val versionCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "web_app_versions",
    foreignKeys = [
        ForeignKey(
            entity = WebApp::class,
            parentColumns = ["id"],
            childColumns = ["appId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["appId"])]
)
data class WebAppVersion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val versionNumber: Int,
    val prompt: String,
    val htmlCode: String,
    val timestamp: Long = System.currentTimeMillis()
)
