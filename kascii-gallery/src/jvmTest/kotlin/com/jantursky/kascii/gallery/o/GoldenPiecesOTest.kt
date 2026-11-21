package com.jantursky.kascii.gallery.o

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares each piece at a fixed time and seed with `src/jvmTest/resources/golden/o/<name>.txt`. Set the
 * environment variable `KASCII_UPDATE_GOLDEN=1` to write the files instead of asserting.
 */
class GoldenPiecesOTest {
    private val directory = File("src/jvmTest/resources/golden/o")
    private val update = System.getenv("KASCII_UPDATE_GOLDEN") == "1"

    @Test
    fun everyPieceMatchesItsGoldenFrame() {
        for (case in pieceCases) {
            val text = case.create(SEED).render(TIME).toText()
            val file = File(directory, "${case.name}.txt")
            if (update) {
                directory.mkdirs()
                file.writeText(text + "\n")
            } else {
                assertTrue(file.exists(), "Missing golden ${file.path}; run with KASCII_UPDATE_GOLDEN=1")
                assertEquals(file.readText().removeSuffix("\n"), text, case.name)
            }
        }
    }

    private companion object {
        const val SEED = 7
        const val TIME = 3f
    }
}
