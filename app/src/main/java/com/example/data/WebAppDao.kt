package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.WebApp
import com.example.model.WebAppVersion
import kotlinx.coroutines.flow.Flow

@Dao
interface WebAppDao {
    @Query("SELECT * FROM web_apps WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getAppsForUser(userId: Long): Flow<List<WebApp>>

    @Query("SELECT * FROM web_apps WHERE userId = :userId AND isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritesForUser(userId: Long): Flow<List<WebApp>>

    @Query("SELECT * FROM web_apps WHERE id = :id LIMIT 1")
    fun getAppById(id: Long): Flow<WebApp?>

    @Query("SELECT * FROM web_apps WHERE id = :id LIMIT 1")
    suspend fun getAppByIdOnce(id: Long): WebApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: WebApp): Long

    @Update
    suspend fun updateApp(app: WebApp)

    @Delete
    suspend fun deleteApp(app: WebApp)

    @Query("SELECT * FROM web_app_versions WHERE appId = :appId ORDER BY versionNumber DESC")
    fun getVersionsForApp(appId: Long): Flow<List<WebAppVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: WebAppVersion): Long

    @Query("DELETE FROM web_app_versions WHERE appId = :appId")
    suspend fun deleteVersionsForApp(appId: Long)

    @Query("SELECT COUNT(*) FROM web_apps WHERE userId = :userId")
    suspend fun getAppCountForUser(userId: Long): Int
}
