package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private val customApiKeyProvider: () -> String = { "" }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String {
        val custom = customApiKeyProvider().trim()
        if (custom.isNotEmpty()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun generateWebApp(
        userPrompt: String,
        modelName: String = "gemini-3.5-flash",
        enhancements: List<String> = emptyList()
    ): Result<GeneratedAppResult> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel or in the in-app Settings.")
            )
        }

        val enhancementPrompt = if (enhancements.isNotEmpty()) {
            "\nAdditional Requirements:\n" + enhancements.joinToString("\n") { "- $it" }
        } else ""

        val systemPrompt = """
            You are AI Luna, a world-class web developer and creative designer.
            You build complete, interactive, gorgeous Single-Page Web Applications inside a SINGLE self-contained HTML file.
            
            Strict Guidelines:
            1. Output MUST be a complete, working HTML5 document (starting with <!DOCTYPE html> and ending with </html>).
            2. Responsive & Mobile-First: Include `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">`. Ensure it looks stunning on mobile phone screens and tablets.
            3. Styles: Include all CSS in `<style>` tags in `<head>`. Use modern CSS variables, smooth transitions, box-shadows, rounded corners, clean typography, vibrant accents, and accessible touch targets (min 44px).
            4. Logic: Include all JavaScript in `<script>` tags before `</body>`. All buttons, forms, canvas rendering, audio, or game loops must be 100% fully functional without placeholders, TODOs, or broken functions. Use `localStorage` for state persistence where appropriate.
            5. Self-Contained: Do not rely on external npm or build steps. You may use standard CDN libraries if helpful (e.g. Tailwind via cdn `<script src="https://cdn.tailwindcss.com"></script>`, FontAwesome / Lucide icons, Canvas, Web Audio API, or Confetti `<script src="https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.3/dist/confetti.browser.min.js"></script>`).
            6. In the <title> tag, provide a creative, concise title for this web application.
        """.trimIndent()

        val fullUserPrompt = "Create a complete, fully functional interactive web application based on this request: $userPrompt$enhancementPrompt"

        try {
            val responseText = executeGeminiRequest(
                apiKey = apiKey,
                modelName = modelName,
                systemInstruction = systemPrompt,
                userPrompt = fullUserPrompt
            )

            val cleanedHtml = cleanHtmlResponse(responseText)
            val title = extractTitle(cleanedHtml, userPrompt)
            val description = extractDescription(userPrompt)

            Result.success(
                GeneratedAppResult(
                    title = title,
                    description = description,
                    htmlCode = cleanedHtml,
                    category = inferCategory(userPrompt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iterateWebApp(
        currentHtml: String,
        modificationPrompt: String,
        modelName: String = "gemini-3.5-flash"
    ): Result<GeneratedAppResult> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add your key in the AI Studio Secrets panel or in the in-app Settings.")
            )
        }

        val systemPrompt = """
            You are AI Luna. The user has an existing self-contained HTML web application and wants to modify, improve, or add features to it.
            You must output the ENTIRE updated, complete HTML document with all requested changes integrated cleanly.
            Keep all working features intact while introducing the new requirements.
            Do not output diffs or partial snippets. Output the complete <!DOCTYPE html> ... </html> document.
        """.trimIndent()

        val userMessage = """
            Here is the existing web app HTML:
            ```html
            $currentHtml
            ```
            
            Requested Modification:
            $modificationPrompt
            
            Please provide the complete updated HTML file now.
        """.trimIndent()

        try {
            val responseText = executeGeminiRequest(
                apiKey = apiKey,
                modelName = modelName,
                systemInstruction = systemPrompt,
                userPrompt = userMessage
            )

            val cleanedHtml = cleanHtmlResponse(responseText)
            val title = extractTitle(cleanedHtml, "Updated App")

            Result.success(
                GeneratedAppResult(
                    title = title,
                    description = "Updated with: $modificationPrompt",
                    htmlCode = cleanedHtml,
                    category = inferCategory(modificationPrompt)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeGeminiRequest(
        apiKey: String,
        modelName: String,
        systemInstruction: String,
        userPrompt: String
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("topP", 0.95)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errJson = JSONObject(responseBody)
                errJson.optJSONObject("error")?.optString("message") ?: "API Error HTTP ${response.code}"
            } catch (e: Exception) {
                "API Error HTTP ${response.code}: $responseBody"
            }
            throw RuntimeException(errorMsg)
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw RuntimeException("No response generated by Gemini model.")
        }

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw RuntimeException("Empty content received from Gemini model.")
        }

        return parts.getJSONObject(0).optString("text", "")
    }

    private fun cleanHtmlResponse(rawText: String): String {
        var text = rawText.trim()

        // Strip leading markdown ```html
        if (text.startsWith("```html", ignoreCase = true)) {
            text = text.substring(7).trim()
        } else if (text.startsWith("```")) {
            text = text.substring(3).trim()
        }

        // Strip trailing markdown ```
        if (text.endsWith("```")) {
            text = text.substring(0, text.length - 3).trim()
        }

        // Find <!DOCTYPE html or <html
        val docTypeIdx = text.indexOf("<!DOCTYPE html", ignoreCase = true)
        val htmlIdx = text.indexOf("<html", ignoreCase = true)
        val startIdx = when {
            docTypeIdx != -1 -> docTypeIdx
            htmlIdx != -1 -> htmlIdx
            else -> 0
        }

        val endIdx = text.lastIndexOf("</html>", ignoreCase = true)
        if (endIdx != -1) {
            text = text.substring(startIdx, endIdx + 7)
        } else if (startIdx > 0) {
            text = text.substring(startIdx)
        }

        return text
    }

    private fun extractTitle(html: String, fallback: String): String {
        val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
        val match = titleRegex.find(html)
        if (match != null && match.groupValues[1].isNotBlank()) {
            return match.groupValues[1].trim()
        }
        return fallback.take(30).trim().capitalizeFirst()
    }

    private fun extractDescription(prompt: String): String {
        return prompt.take(120).trim()
    }

    private fun inferCategory(prompt: String): String {
        val p = prompt.lowercase()
        return when {
            p.contains("game") || p.contains("arcade") || p.contains("play") || p.contains("puzzle") || p.contains("quiz") -> "Game"
            p.contains("synth") || p.contains("audio") || p.contains("art") || p.contains("paint") || p.contains("music") || p.contains("draw") -> "Creative"
            p.contains("dash") || p.contains("chart") || p.contains("crypto") || p.contains("finance") || p.contains("stock") -> "Dashboard"
            p.contains("habit") || p.contains("todo") || p.contains("timer") || p.contains("pomodoro") || p.contains("note") || p.contains("tracker") -> "Productivity"
            else -> "Utility"
        }
    }

    private fun String.capitalizeFirst(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

data class GeneratedAppResult(
    val title: String,
    val description: String,
    val htmlCode: String,
    val category: String
)
