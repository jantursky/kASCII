package com.jantursky.kascii.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily

/**
 * The bundled font once anything has loaded it, with the resolver that loaded it, for drawing outside composition
 * ([com.jantursky.kascii.compose.ascii] and grid bitmaps). The family is snapshot state, so a draw that read the
 * fallback redraws when the font lands.
 */
internal object FontRegistry {
    var family: FontFamily? by mutableStateOf(null)
        private set
    var resolver: FontFamily.Resolver? = null
        private set

    fun register(
        family: FontFamily,
        resolver: FontFamily.Resolver,
    ) {
        if (this.family != null) return
        this.resolver = resolver
        this.family = family
    }
}
