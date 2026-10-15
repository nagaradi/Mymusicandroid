package com.example.mymusic

import android.app.Activity
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

class LocalMusic : Activity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var list: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 24, 20, 20)
        root.setBackgroundColor(Color.rgb(15, 15, 18))

        val title = TextView(this)
        title.text = "My Music — Phone Songs"
        title.textSize = 24f
        title.setTextColor(Color.WHITE)
        root.addView(title)

        status = TextView(this)
        status.setTextColor(Color.WHITE)
        status.text = "Loading songs..."
        root.addView(status)

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        val token = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                loadSongs()
            } catch (_: Exception) {
                status.text = "Player initialization failed."
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun loadSongs() {
        list.removeAllViews()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        try {
            contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val songTitle = cursor.getString(titleColumn) ?: "Unknown song"
                    val artist = cursor.getString(artistColumn) ?: "Unknown artist"
                    val songUri = android.content.ContentUris.withAppendedId(
                        collection,
                        id
                    )

                    val button = Button(this)
                    button.text = "$songTitle\n$artist"
                    button.setOnClickListener {
                        val player = controller
                        if (player != null) {
                            player.setMediaItem(MediaItem.fromUri(songUri))
                            player.prepare()
                            player.play()
                            status.text = "Playing: $songTitle"
                        } else {
                            status.text = "Player is starting. Try again."
                        }
                    }
                    list.addView(button)
                }
            }

            if (list.childCount == 0) {
                status.text = "No songs found. Add MP3/M4A files to your phone."
            } else {
                status.text = "Tap a song to play it."
            }
        } catch (_: Exception) {
            status.text = "Could not read songs from this phone."
        }
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
