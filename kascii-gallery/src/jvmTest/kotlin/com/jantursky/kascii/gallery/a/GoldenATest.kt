package com.jantursky.kascii.gallery

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoldenATest {
    private val update = (System.getProperty("KASCII_UPDATE_GOLDEN") ?: System.getenv("KASCII_UPDATE_GOLDEN")) == "1"
    private val dir = File("src/jvmTest/resources/golden/a")

    @Test
    fun framesMatchTheirGoldenText() {
        for (entry in galleryPiecesA) {
            val actual = entry.create(0).render(FIXED_TIME).toText()
            val file = File(dir, "${entry.id}.txt")
            if (update) {
                dir.mkdirs()
                file.writeText(actual)
            } else {
                assertTrue(file.exists(), "missing golden ${file.path}; run with KASCII_UPDATE_GOLDEN=1")
                assertEquals(file.readText().replace("\r\n", "\n"), actual, "${entry.id} differs from its golden")
            }
        }
    }

    private companion object {
        const val FIXED_TIME = 1.7f
    }
}
