package com.jantursky.kascii.compose.internal

// Image children of runtime effects are not verified in browsers yet, so the web draws with the atlas painter.
internal actual fun createShaderPainter(): ShaderPainter? = null
