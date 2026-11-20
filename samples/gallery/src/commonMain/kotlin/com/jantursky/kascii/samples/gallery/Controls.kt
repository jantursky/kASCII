package com.jantursky.kascii.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jantursky.kascii.compose.AsciiRenderer
import com.jantursky.kascii.core.Motion
import kotlin.math.roundToInt

private val RendererLabels =
    mapOf(
        AsciiRenderer.Auto to "Auto",
        AsciiRenderer.Gpu to "GPU",
        AsciiRenderer.Cpu to "CPU",
        AsciiRenderer.Text to "Text",
    )

private val MotionLabels = mapOf(Motion.System to "System", Motion.Full to "Full", Motion.Reduced to "Reduced")

@Composable
fun SpeedControl(state: GalleryState) =
    SliderField("Speed", state.speed, { state.speed = it }, 0.25f..3f, step = 0.25f, format = { "${it}x" })

@Composable
fun SeedControl(state: GalleryState) =
    SliderField(
        "Seed",
        state.seed.toFloat(),
        { state.seed = it.roundToInt() },
        0f..99f,
        step = 1f,
        format = { it.roundToInt().toString() },
    )

@Composable
fun RendererControl(state: GalleryState) =
    Labeled("Renderer") {
        Choice(RendererLabels.keys.toList(), state.renderer, { state.renderer = it }, { RendererLabels.getValue(it) })
    }

/** Reduced motion: System follows the platform, Full always animates, Reduced always holds the still frame. */
@Composable
fun MotionControl(state: GalleryState) =
    Labeled("Reduced motion") {
        Choice(MotionLabels.keys.toList(), state.motion, { state.motion = it }, { MotionLabels.getValue(it) })
    }

@Composable
fun ThemeControl(state: GalleryState) =
    Labeled("Theme") {
        Choice(listOf(true, false), state.dark, { state.useDark(it) }, { if (it) "Dark" else "Light" })
    }

@Composable
fun CellControl(
    cell: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) = SliderField("Cell size", cell, onChange, 4f..16f, modifier, step = 1f, format = { "${it.roundToInt()}dp" })
