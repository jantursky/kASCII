package com.jantursky.kascii.compose

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.text.font.FontFamily
import com.jantursky.kascii.compose.internal.CellMetrics
import com.jantursky.kascii.compose.internal.buildAtlas
import com.jantursky.kascii.compose.internal.pixelMeasurer
import kotlin.test.Test
import kotlin.test.assertTrue

class AtlasImmutableTest {
    /** A GPU canvas uploads a changeable bitmap again on every draw call, which took gigabytes on iOS. */
    @Test
    fun aBuiltAtlasIsReadOnlyAfterItIsDrawn() {
        val resolver =
            androidx.compose.ui.text.font
                .createFontFamilyResolver()
        val atlas = buildAtlas("ab·", CellMetrics(8, 18, 13f), FontFamily.Monospace, pixelMeasurer(resolver))
        assertTrue(atlas.image.asSkiaBitmap().isImmutable)
    }
}
