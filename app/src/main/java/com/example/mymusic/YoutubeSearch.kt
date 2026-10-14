package com.example.mymusic

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient

class YouTubeSearch : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 24, 16, 16)
            setBackgroundColor(Color.rgb(15, 15, 18))
        }

        val title = TextView(this).apply {
            text = "My Music - YouTube"
            textSize = 24f
            setTextColor(Color.WHITE)
        }
        layout.addView(title)

        val search = EditText(this).apply {
            hint = "गाने का नाम लिखें"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        layout.addView(search)

        val button = Button(this).apply {
            text = "Search YouTube"
        }
        layout.addView(button)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.webViewClient = WebViewClient()

        layout.addView(
            web,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        button.setOnClickListener {
            val query = search.text.toString().trim()
            if (query.isNotEmpty()) {
                val encoded = android.net.Uri.encode(query)
                web.loadUrl(
                    "https://www.youtube.com/results?search_query=$encoded"
                )
            }
        }

        setContentView(layout)
    }
}
