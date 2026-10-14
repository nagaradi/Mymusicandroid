package com.example.mymusic

import android.content.ComponentName
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var status: TextView
    private lateinit var urlInput: EditText
    private lateinit var quality: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(15, 15, 18)
        window.navigationBarColor = android.graphics.Color.rgb(15, 15, 18)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 36, 24, 24)
            setBackgroundColor(android.graphics.Color.rgb(15, 15, 18))
        }
        fun text(value: String, size: Float = 16f) = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 8, 0, 8)
        }
        fun button(label: String, action: () -> Unit) = Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

        root.addView(text("My Music", 30f))
        root.addView(text("Dark • Background playback • Direct audio URLs", 13f))
        root.addView(button("Search YouTube") {
    startActivity(
        android.content.Intent(this, YouTubeSearch::class.java)
    )
}, LinearLayout.LayoutParams(-1, -2))
        urlInput = EditText(this).apply {
            hint = "Paste a permitted direct audio URL (MP3/M4A)"
            setHintTextColor(0xFFAAAAAA.toInt())
            setTextColor(-1)
            setSingleLine(true)
        }
        root.addView(urlInput, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(button("Play in background") { playUrl() }, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Download MP3/audio") { downloadAudio() }, LinearLayout.LayoutParams(-1, -2))

        root.addView(text("Quality preference (does not upsample source audio):", 13f))
        quality = Spinner(this)
        quality.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Original quality", "128 kbps", "192 kbps", "320 kbps"))
        root.addView(quality, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Pause") { controller?.pause() }, LinearLayout.LayoutParams(-1, -2))
        status = text("Paste a direct audio-file URL to get started.", 14f)
        root.addView(status)
        root.addView(text("Use only audio you own or are permitted to download. Quality choices are preferences; this starter does not transcode audio.", 12f))
        setContentView(root)

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try { controller = controllerFuture?.get() } catch (_: Exception) { status.text = "Player initialization failed." }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun playUrl() {
        val url = urlInput.text.toString().trim()
        if (!url.startsWith("https://")) {
            status.text = "Please enter a valid HTTPS direct audio URL."
            return
        }
        val player = controller
        if (player == null) { status.text = "Player is starting. Try again in a moment."; return }
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
        status.text = "Playing. Playback can continue in the background."
    }

    private fun downloadAudio() {
        val url = urlInput.text.toString().trim()
        if (!url.startsWith("https://")) {
            status.text = "Please enter a valid HTTPS direct audio URL."
            return
        }
        status.text = "Download started…"
        Thread {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                connection.connect()
                if (connection.responseCode !in 200..299) throw Exception("HTTP ${connection.responseCode}")
                val name = url.substringAfterLast('/').substringBefore('?').ifBlank { "downloaded-audio.mp3" }
                    .replace(Regex("[^A-Za-z0-9._-]"), "_")
                val safeName = if (name.contains('.')) name else "$name.mp3"
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, if (safeName.endsWith(".m4a")) "audio/mp4" else "audio/mpeg")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Music/My Music")
                }
                val uri = contentResolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw Exception("Could not create download file")
                contentResolver.openOutputStream(uri)?.use { output ->
                    connection.inputStream.use { input -> input.copyTo(output) }
                } ?: throw Exception("Could not open output file")
                connection.disconnect()
                runOnUiThread { status.text = "Saved to Music/My Music/$safeName" }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Download failed: ${e.message ?: "unknown error"}" }
            }
        }.start()
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
