package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.WebApp
import com.example.security.PasswordSecurity
import com.example.util.ProjectConfig
import com.example.util.ProjectShareManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("AI Luna", appName)
  }

  @Test
  fun `password hashing and verification works`() {
    val salt = PasswordSecurity.generateSalt()
    val hash = PasswordSecurity.hashPassword("secret123", salt)
    assertTrue(PasswordSecurity.verifyPassword("secret123", salt, hash))
    assertFalse(PasswordSecurity.verifyPassword("wrongpass", salt, hash))
  }

  @Test
  fun `project config creation and unique share code generation`() {
    val testApp = WebApp(
      id = 42,
      userId = 1,
      title = "Cyber Clock",
      prompt = "Build a futuristic cyberpunk neon clock",
      description = "Neon glow digital clock",
      htmlCode = "<html><body><h1>12:00:00</h1></body></html>",
      category = "Utility",
      versionCount = 2
    )

    val config = ProjectShareManager.createConfig(testApp, authorName = "Alex")
    assertEquals("Cyber Clock", config.title)
    assertTrue(config.shareId.startsWith("LUNA-"))
    assertEquals(14, config.shareId.length) // e.g. "LUNA-ABCD-EF01"
    assertEquals("Alex", config.authorName)
    assertEquals("Utility", config.category)
  }

  @Test
  fun `project config json and compression roundtrip`() {
    val original = ProjectConfig(
      shareId = "LUNA-1234-5678",
      title = "2048 Game",
      prompt = "2048 numbers game with smooth tiles",
      description = "Addictive puzzle",
      category = "Game",
      htmlCode = "<!DOCTYPE html><html><body><div id='board'></div></body></html>",
      versionCount = 3,
      authorName = "Luna Dev"
    )

    // JSON roundtrip
    val json = ProjectShareManager.toJson(original)
    val fromJson = ProjectShareManager.fromJson(json)
    assertNotNull(fromJson)
    assertEquals(original.title, fromJson?.title)
    assertEquals(original.shareId, fromJson?.shareId)
    assertEquals(original.htmlCode, fromJson?.htmlCode)

    // Compressed Base64 link roundtrip
    val shareLink = ProjectShareManager.generateShareLink(original)
    assertTrue(shareLink.contains("https://ailuna.app/project"))
    assertTrue(shareLink.contains("#bundle="))

    val parsed = ProjectShareManager.parseShareInput(shareLink)
    assertNotNull(parsed)
    assertEquals(original.title, parsed?.title)
    assertEquals(original.prompt, parsed?.prompt)
    assertEquals(original.htmlCode, parsed?.htmlCode)
  }

  @Test
  fun `parse share input from deep link Uri`() {
    val config = ProjectConfig(
      shareId = "LUNA-9999-0000",
      title = "Drawing Canvas",
      prompt = "Freehand paint tool",
      description = "Smooth canvas",
      category = "Creative",
      htmlCode = "<html><body><canvas id='c'></canvas></body></html>",
      versionCount = 1,
      authorName = "Artist"
    )

    val encoded = ProjectShareManager.compressAndEncode(config)
    val uri = Uri.parse("https://ailuna.app/project?id=${config.shareId}&bundle=$encoded")
    val parsed = ProjectShareManager.parseFromUri(uri)

    assertNotNull(parsed)
    assertEquals("Drawing Canvas", parsed?.title)
    assertEquals("Creative", parsed?.category)
  }
}
