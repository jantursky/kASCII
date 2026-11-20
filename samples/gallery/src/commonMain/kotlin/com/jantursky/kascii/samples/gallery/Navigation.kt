package com.jantursky.kascii.samples.gallery

import androidx.compose.runtime.compositionLocalOf

/** Opens a page by id. Provided by [GalleryApp]. */
val LocalNavigate = compositionLocalOf<(String) -> Unit> { {} }

/** Whether the window is wide enough for the desktop layout. Provided by [GalleryApp]. */
val LocalWide = compositionLocalOf { true }
