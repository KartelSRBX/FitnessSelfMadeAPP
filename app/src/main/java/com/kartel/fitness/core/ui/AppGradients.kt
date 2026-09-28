package com.kartel.fitness.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.kartel.fitness.ui.theme.Blue
import com.kartel.fitness.ui.theme.Lilac
import com.kartel.fitness.ui.theme.Purple

/** Фирменный градиент палитры (синий/фиолетовый/сиреневый) для хедеров, FAB и акцентов. */
object AppGradients {
    val brand = Brush.linearGradient(colors = listOf(Blue, Purple, Lilac))

    val brandDiagonal = Brush.linearGradient(
        colors = listOf(Blue, Purple, Lilac),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f),
    )
}
