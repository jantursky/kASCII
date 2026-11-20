package com.jantursky.kascii.compose

/** Which renderer draws the glyph grid. [Auto] picks the GPU path where it is available and worth it. */
public enum class AsciiRenderer {
    /** GPU shader when the platform supports it and the grid is large, otherwise [Cpu]. */
    Auto,

    /** Glyph atlas plus a runtime shader. Falls back to [Cpu] where unavailable. */
    Gpu,

    /** Glyph grid drawn from a cached atlas. */
    Cpu,

    /** Real selectable text. Slowest, but copyable and readable by accessibility services. */
    Text,
}
