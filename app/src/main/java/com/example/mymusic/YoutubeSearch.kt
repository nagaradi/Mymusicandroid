package com.example.mymusic

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient

class YouTubeSearch : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(16, 24, 16, 16)
        layout.setBackgroundColor(Color.rgb(15, 15, 18))

        val title = TextView(this)
        title.text = "My Music - YouTube"
        title.textSize = 24f
        title.setTextColor(Color.WHITE)
        layout.addView(title)

        val search = EditText(this)
        search.hint = "गाने का नाम लिखें"
        search.setSingleLine(true)
        search.setTextColor(Color.WHITE)
        search.setHintTextColor(Color.LTGRAY)
        layout.addView(search)

        val button = Button(this)
        button.text = "Search YouTube"
        layout.addView(button)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.webViewClient = WebViewClient()

        val webParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        layout.addView(web, webParams)

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
