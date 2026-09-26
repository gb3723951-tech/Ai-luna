package com.example.model

enum class LogLevel {
    LOG, INFO, WARN, ERROR
}

data class ConsoleLogEntry(
    val level: LogLevel,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TemplateApp(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val description: String,
    val iconName: String,
    val colorHex: String,
    val prebuiltHtml: String
)
