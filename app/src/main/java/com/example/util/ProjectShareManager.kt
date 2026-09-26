package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.example.model.WebApp
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.abs

data class ProjectConfig(
    val shareId: String,
    val title: String,
    val prompt: String,
    val description: String,
    val category: String,
    val htmlCode: String,
    val versionCount: Int = 1,
    val authorName: String = "AI Luna Creator",
    val createdAt: Long = System.currentTimeMillis()
)

object ProjectShareManager {
    private const val APP_IDENTIFIER = "ai_luna_project"
    private const val DEEP_LINK_BASE = "https://ailuna.app/project"

    fun createConfig(app: WebApp, authorName: String = "AI Luna Creator"): ProjectConfig {
        val shareId = generateShareCode(app.id, app.title)
        return ProjectConfig(
            shareId = shareId,
            title = app.title,
            prompt = app.prompt,
            description = app.description,
            category = app.category,
            htmlCode = app.htmlCode,
            versionCount = app.versionCount,
            authorName = authorName,
            createdAt = app.createdAt
        )
    }

    fun generateShareCode(appId: Long, title: String): String {
        val hash = abs((title + appId.toString()).hashCode())
        val hex = hash.toString(16).uppercase().padStart(8, '0').take(8)
        return "LUNA-${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    fun toJson(config: ProjectConfig, pretty: Boolean = false): String {
        val json = JSONObject().apply {
            put("identifier", APP_IDENTIFIER)
            put("schemaVersion", 1)
            put("shareId", config.shareId)
            put("title", config.title)
            put("prompt", config.prompt)
            put("description", config.description)
            put("category", config.category)
            put("htmlCode", config.htmlCode)
            put("versionCount", config.versionCount)
            put("authorName", config.authorName)
            put("createdAt", config.createdAt)
        }
        return if (pretty) json.toString(2) else json.toString()
    }

    fun fromJson(jsonStr: String): ProjectConfig? {
        return try {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", "").ifEmpty { "Shared Web App" }
            val prompt = json.optString("prompt", "Shared from AI Luna")
            val htmlCode = json.optString("htmlCode", "")
            if (htmlCode.isEmpty()) return null

            ProjectConfig(
                shareId = json.optString("shareId", "LUNA-SHARE"),
                title = title,
                prompt = prompt,
                description = json.optString("description", ""),
                category = json.optString("category", "Utility"),
                htmlCode = htmlCode,
                versionCount = json.optInt("versionCount", 1),
                authorName = json.optString("authorName", "AI Luna Creator"),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }

    fun compressAndEncode(config: ProjectConfig): String {
        val jsonString = toJson(config, pretty = false)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        return Base64.encodeToString(bos.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun decodeAndDecompress(encodedData: String): ProjectConfig? {
        val trimmed = encodedData.trim()
        // Try GZIP compressed Base64 first
        try {
            val decodedBytes = Base64.decode(trimmed, Base64.URL_SAFE or Base64.DEFAULT)
            val bis = ByteArrayInputStream(decodedBytes)
            val jsonString = GZIPInputStream(bis).use { gzip ->
                gzip.reader(Charsets.UTF_8).readText()
            }
            val config = fromJson(jsonString)
            if (config != null) return config
        } catch (_: Exception) {
            // Fall through to other formats
        }

        // Try direct Base64 without GZIP
        try {
            val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
            val directJson = String(decodedBytes, Charsets.UTF_8)
            val config = fromJson(directJson)
            if (config != null) return config
        } catch (_: Exception) {
            // Fall through
        }

        // Try direct JSON
        return fromJson(trimmed)
    }

    fun generateShareLink(config: ProjectConfig): String {
        val payload = compressAndEncode(config)
        return "$DEEP_LINK_BASE?id=${config.shareId}#bundle=$payload"
    }

    fun generateShareText(config: ProjectConfig): String {
        val link = generateShareLink(config)
        return """
            🚀 Check out my AI Luna Web App: "${config.title}"
            Category: ${config.category}
            Prompt: "${config.prompt.take(120)}"
            Creator: ${config.authorName}
            
            🔗 Open & run directly in AI Luna:
            $link
            
            Project ID: ${config.shareId}
        """.trimIndent()
    }

    fun parseShareInput(rawInput: String): ProjectConfig? {
        val text = rawInput.trim()
        if (text.isEmpty()) return null

        // 1. If it's a URL or contains a URL
        if (text.contains("#bundle=")) {
            val bundlePart = text.substringAfter("#bundle=").substringBefore("&").substringBefore(" ").trim()
            val config = decodeAndDecompress(bundlePart)
            if (config != null) return config
        }
        if (text.contains("bundle=")) {
            val bundlePart = text.substringAfter("bundle=").substringBefore("&").substringBefore("#").substringBefore(" ").trim()
            val config = decodeAndDecompress(bundlePart)
            if (config != null) return config
        }

        // 2. If it's a direct compressed payload or direct JSON
        val directConfig = decodeAndDecompress(text)
        if (directConfig != null) return directConfig

        // 3. Search for any Base64-like token in text
        val tokens = text.split(Regex("[\\s\n\r]+"))
        for (token in tokens) {
            if (token.length > 30) {
                val candidate = decodeAndDecompress(token)
                if (candidate != null) return candidate
            }
        }

        return null
    }

    fun parseFromUri(uri: Uri): ProjectConfig? {
        // Query param 'bundle'
        uri.getQueryParameter("bundle")?.let { bundle ->
            decodeAndDecompress(bundle)?.let { return it }
        }
        // Fragment '#bundle=...'
        uri.fragment?.let { fragment ->
            if (fragment.startsWith("bundle=")) {
                val bundle = fragment.removePrefix("bundle=")
                decodeAndDecompress(bundle)?.let { return it }
            }
        }
        return null
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun shareProjectViaIntent(context: Context, config: ProjectConfig) {
        val shareText = generateShareText(config)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "AI Luna Project: ${config.title}")
            putExtra(Intent.EXTRA_TITLE, config.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Project via"))
    }
}
