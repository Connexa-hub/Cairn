package com.cairn.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A sparse dot grid — Nothing OS's signature decorative language — used as
 * a quiet texture accent rather than a literal copy of their glyph UI.
 * Meant to sit behind or beside content at low opacity, never as the main
 * focal element.
 */
@Composable
fun DotMatrix(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    dotAlpha: Float = 0.14f,
    dotRadius: Float = 1.6f,
    spacing: Float = 14f
) {
    Canvas(modifier = modifier) {
        drawDotGrid(dotColor.copy(alpha = dotAlpha), dotRadius, spacing)
    }
}

private fun DrawScope.drawDotGrid(color: Color, dotRadius: Float, spacing: Float) {
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = color, radius = dotRadius, center = androidx.compose.ui.geometry.Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}
