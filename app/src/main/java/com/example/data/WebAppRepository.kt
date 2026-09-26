package com.example.data

import com.example.model.WebApp
import com.example.model.WebAppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WebAppRepository(
    private val webAppDao: WebAppDao
) {
    fun getAppsForUser(userId: Long): Flow<List<WebApp>> {
        return webAppDao.getAppsForUser(userId)
    }

    fun getFavoritesForUser(userId: Long): Flow<List<WebApp>> {
        return webAppDao.getFavoritesForUser(userId)
    }

    fun getAppById(id: Long): Flow<WebApp?> {
        return webAppDao.getAppById(id)
    }

    suspend fun getAppByIdOnce(id: Long): WebApp? = withContext(Dispatchers.IO) {
        webAppDao.getAppByIdOnce(id)
    }

    fun getVersionsForApp(appId: Long): Flow<List<WebAppVersion>> {
        return webAppDao.getVersionsForApp(appId)
    }

    suspend fun createWebApp(
        userId: Long,
        title: String,
        prompt: String,
        description: String,
        htmlCode: String,
        category: String
    ): WebApp = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val webApp = WebApp(
            userId = userId,
            title = title,
            prompt = prompt,
            description = description,
            htmlCode = htmlCode,
            category = category,
            versionCount = 1,
            createdAt = now,
            updatedAt = now
        )
        val insertedId = webAppDao.insertApp(webApp)
        val initialVersion = WebAppVersion(
            appId = insertedId,
            versionNumber = 1,
            prompt = "Initial Generation: $prompt",
            htmlCode = htmlCode,
            timestamp = now
        )
        webAppDao.insertVersion(initialVersion)
        webApp.copy(id = insertedId)
    }

    suspend fun importWebApp(
        userId: Long,
        title: String,
        prompt: String,
        description: String,
        htmlCode: String,
        category: String,
        authorName: String = "AI Luna Creator"
    ): WebApp = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val desc = if (description.isNotBlank()) {
            if (authorName.isNotBlank() && !description.contains("Shared by")) "$description (Shared by $authorName)" else description
        } else {
            "Shared project by $authorName"
        }
        val webApp = WebApp(
            userId = userId,
            title = title,
            prompt = prompt,
            description = desc,
            htmlCode = htmlCode,
            category = category,
            versionCount = 1,
            createdAt = now,
            updatedAt = now
        )
        val insertedId = webAppDao.insertApp(webApp)
        val initialVersion = WebAppVersion(
            appId = insertedId,
            versionNumber = 1,
            prompt = "Imported Project: $prompt",
            htmlCode = htmlCode,
            timestamp = now
        )
        webAppDao.insertVersion(initialVersion)
        webApp.copy(id = insertedId)
    }

    suspend fun updateWebAppCode(
        appId: Long,
        newHtmlCode: String,
        changePrompt: String,
        newTitle: String? = null,
        newDescription: String? = null
    ): WebApp? = withContext(Dispatchers.IO) {
        val current = webAppDao.getAppByIdOnce(appId) ?: return@withContext null
        val newVersionNumber = current.versionCount + 1
        val now = System.currentTimeMillis()

        val updated = current.copy(
            htmlCode = newHtmlCode,
            title = newTitle ?: current.title,
            description = newDescription ?: current.description,
            versionCount = newVersionNumber,
            updatedAt = now
        )
        webAppDao.updateApp(updated)

        val version = WebAppVersion(
            appId = appId,
            versionNumber = newVersionNumber,
            prompt = changePrompt,
            htmlCode = newHtmlCode,
            timestamp = now
        )
        webAppDao.insertVersion(version)
        updated
    }

    suspend fun revertToVersion(appId: Long, version: WebAppVersion): WebApp? = withContext(Dispatchers.IO) {
        val current = webAppDao.getAppByIdOnce(appId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val newVersionNumber = current.versionCount + 1

        val updated = current.copy(
            htmlCode = version.htmlCode,
            versionCount = newVersionNumber,
            updatedAt = now
        )
        webAppDao.updateApp(updated)

        val newVersionEntry = WebAppVersion(
            appId = appId,
            versionNumber = newVersionNumber,
            prompt = "Reverted to v${version.versionNumber}: ${version.prompt}",
            htmlCode = version.htmlCode,
            timestamp = now
        )
        webAppDao.insertVersion(newVersionEntry)
        updated
    }

    suspend fun toggleFavorite(app: WebApp) = withContext(Dispatchers.IO) {
        val updated = app.copy(isFavorite = !app.isFavorite)
        webAppDao.updateApp(updated)
    }

    suspend fun deleteApp(app: WebApp) = withContext(Dispatchers.IO) {
        webAppDao.deleteVersionsForApp(app.id)
        webAppDao.deleteApp(app)
    }

    suspend fun seedPresetIfEmpty(userId: Long) = withContext(Dispatchers.IO) {
        val count = webAppDao.getAppCountForUser(userId)
        if (count == 0) {
            PresetTemplates.templates.forEach { template ->
                createWebApp(
                    userId = userId,
                    title = template.title,
                    prompt = template.prompt,
                    description = template.description,
                    htmlCode = template.prebuiltHtml,
                    category = template.category
                )
            }
        }
    }
}
