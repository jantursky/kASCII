package com.jantursky.kascii.samples.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jantursky.kascii.samples.app.GalleryApp

/**
 * Hosts the gallery. Optional intent extras open a page directly, for testing:
 * `adb shell am start -S -n com.jantursky.kascii.samples.gallery/com.jantursky.kascii.samples.android.MainActivity
 * --es entry fields --ez dark false` opens the fields page in the light theme.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val entry = intent.getStringExtra(EXTRA_ENTRY)
        val dark = if (intent.hasExtra(EXTRA_DARK)) intent.getBooleanExtra(EXTRA_DARK, true) else null
        setContent { GalleryApp(entry, dark, onDarkChange = ::matchSystemBars) }
    }

    /** Light icons over the dark theme and dark ones over the light theme, whatever the system theme is. */
    private fun matchSystemBars(dark: Boolean) {
        val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    private companion object {
        const val EXTRA_ENTRY = "entry"
        const val EXTRA_DARK = "dark"
    }
}
