package com.jantursky.kascii.samples.gallery

import androidx.compose.ui.platform.ClipEntry

/** A clipboard entry holding [text]. Compose builds entries per platform, so there is no common constructor. */
internal expect fun plainTextEntry(text: String): ClipEntry
