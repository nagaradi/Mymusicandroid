package com.example.mymusic

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private lateinit var root: LinearLayout
    private lateinit var songList: LinearLayout
    private lateinit var status: TextView
    private lateinit var searchInput: EditText
    private lateinit var miniTitle: TextView
    private lateinit var playButton: Button

    private val songs = mutableListOf<Song>()
    private val visibleSongs = mutableListOf<Song>()

    data class Song(val id: Long, val title: String, val artist: String)

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(16, 16, 20)
        window.navigationBarColor = Color.rgb(16, 16, 20)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 8)
            setBackgroundColor(Color.rgb(16, 16, 20))
        }

        val header = TextView(this).apply {
            text = "♫  My Music"
            textSize = 27f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(4, 4, 4, 14)
        }
        root.addView(header)

        searchInput = EditText(this).apply {
            hint = "⌕  Search songs on your phone"
            setSingleLine(true)
            textSize = 15f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(14, 4, 14, 4)
            setBackgroundColor(Color.rgb(38, 38, 45))
        }
        root.addView(
            searchInput,
            LinearLayout.LayoutParams(-1, 52).apply {
                bottomMargin = 12
            }
        )

        val youtubeButton = Button(this).apply {
            text = "Search YouTube  ▶"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, YouTubeSearch::class.java))
            }
        }
        root.addView(
            youtubeButton,
            LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = 14
            }
        )

        val latestRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val latestTitle = TextView(this).apply {
            text = "Latest Songs"
            textSize = 21f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        latestRow.addView(
            latestTitle,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val refreshButton = TextView(this).apply {
            text = "↻ Refresh"
            textSize = 14f
            setTextColor(Color.rgb(100, 210, 180))
            setPadding(8, 8, 4, 8)
            setOnClickListener { loadSongs() }
        }
        latestRow.addView(refreshButton)
        root.addView(latestRow)

        status = TextView(this).apply {
            text = "Loading songs..."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(2, 8, 2, 8)
        }
        root.addView(status)

        val scroll = ScrollView(this)
        songList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(
            songList,
            ScrollView.LayoutParams(-1, -2)
        )
        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val divider = android.view.View(this).apply {
            setBackgroundColor(Color.rgb(55, 55, 62))
        }
        root.addView(divider, LinearLayout.LayoutParams(-1, 1))

        val miniPlayer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 8, 4, 8)
        }

        val cover = TextView(this).apply {
            text = "♫"
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(55, 48, 75))
        }
        miniPlayer.addView(
            cover,
            LinearLayout.LayoutParams(48, 48)
        )

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 0, 4, 0)
        }

        miniTitle = TextView(this).apply {
            text = "No song playing"
            textSize = 14f
            setTextColor(Color.WHITE)
            maxLines = 1
        }
        titleColumn.addView(miniTitle)

        val subtitle = TextView(this).apply {
            text = "My Music Player"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }
        titleColumn.addView(subtitle)

        miniPlayer.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        playButton = Button(this).apply {
            text = "▶"
            setOnClickListener {
                val player = controller
                if (player != null) {
                    if (player.isPlaying) {
                        player.pause()
                        text = "▶"
                    } else {
                        player.play()
                        text = "Ⅱ"
                    }
                } else {
                    status.text = "Player is starting. Please wait."
                }
            }
        }
        miniPlayer.addView(
            playButton,
            LinearLayout.LayoutParams(64, -2)
        )

        root.addView(miniPlayer)
        setContentView(root)

        searchInput.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    showSongs(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: android.text.Editable?) {}
            }
        )

        val token = SessionToken(
            this,
            ComponentName(this, PlaybackService::class.java)
        )

        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
            } catch (_: Exception) {
                status.text = "Player could not start."
            }
        }, ContextCompat.getMainExecutor(this))

        if (ContextCompat.checkSelfPermission(this, audioPermission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadSongs()
        } else {
            permissionLauncher.launch(audioPermission)
        }
    }

    private fun loadSongs() {
        if (ContextCompat.checkSelfPermission(this, audioPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            status.text = "Allow music access to show your phone songs."
            permissionLauncher.launch(audioPermission)
            return
        }

        try {
            songs.clear()

            val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST
            )

            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            contentResolver.query(
                collection,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )
                val titleCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )
                val artistCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

                while (cursor.moveToNext()) {
                    songs.add(
                        Song(
                            cursor.getLong(idCol),
                            cursor.getString(titleCol) ?: "Unknown song",
                            cursor.getString(artistCol) ?: "Unknown artist"
                        )
                    )
                }
            }

            showSongs(searchInput.text.toString())
        } catch (e: Exception) {
            status.text = "Could not load songs: ${e.message ?: "unknown error"}"
        }
    }

    private fun showSongs(query: String) {
        songList.removeAllViews()
        visibleSongs.clear()

        val filtered = songs.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
        }

        visibleSongs.addAll(filtered)

        status.text = if (filtered.isEmpty()) {
            if (songs.isEmpty()) {
                "No songs found. Add audio files to your phone."
            } else {
                "No matching songs found."
            }
        } else {
            "${filtered.size} songs"
        }

        filtered.forEachIndexed { index, song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(4, 9, 4, 9)
                setBackgroundColor(
                    if (index % 2 == 0) Color.rgb(23, 23, 29)
                    else Color.rgb(16, 16, 20)
                )
                isClickable = true
                setOnClickListener { playSong(song) }
            }

            val icon = TextView(this).apply {
                text = "♫"
                textSize = 23f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(55, 48, 75))
            }
            row.addView(icon, LinearLayout.LayoutParams(46, 46))

            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 0, 4, 0)
            }

            val songTitle = TextView(this).apply {
                text = song.title
                textSize = 15f
                setTextColor(Color.WHITE)
                maxLines = 1
            }
            details.addView(songTitle)

            val artist = TextView(this).apply {
                text = song.artist
                textSize = 12f
                setTextColor(Color.LTGRAY)
                maxLines = 1
            }
            details.addView(artist)

            row.addView(
                details,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            val playIcon = TextView(this).apply {
                text = "▶"
                textSize = 18f
                setTextColor(Color.rgb(100, 210, 180))
                setPadding(8, 8, 8, 8)
            }
            row.addView(playIcon)

            songList.addView(
                row,
                LinearLayout.LayoutParams(-1, -2)
            )
        }
    }

    private fun playSong(song: Song) {
        val player = controller
        if (player == null) {
            status.text = "Player is starting. Try again in a moment."
            return
        }

        val uri = android.content.ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            song.id
        )

        try {
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.play()
            miniTitle.text = song.title
            playButton.text = "Ⅱ"
            status.text = "Playing: ${song.title}"
        } catch (e: Exception) {
            status.text = "Playback failed: ${e.message ?: "unknown error"}"
        }
    }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                loadSongs()
            } else {
                status.text = "Music permission denied. Allow it in Settings."
            }
        }
    }private val permissionLauncher =
    registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadSongs()
        } else {
            status.text = "Music permission denied. Allow it in Settings."
        }
    }
    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}
