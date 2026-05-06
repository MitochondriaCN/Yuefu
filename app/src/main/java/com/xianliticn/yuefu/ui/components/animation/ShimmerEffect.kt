package com.xianliticn.yuefu.ui.components.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.ui.theme.BgDark
import com.xianliticn.yuefu.ui.theme.BgLight

@Composable
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color.Unspecified,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val resolvedBase = if (baseColor == Color.Unspecified) {
        BgLight.copy(alpha = 0.6f)
    } else baseColor

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            resolvedBase,
            resolvedBase.copy(alpha = 0.4f),
            resolvedBase,
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    this
        .clip(shape)
        .background(shimmerBrush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = Color.Unspecified,
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.fillMaxWidth() else Modifier)
            .height(height)
            .shimmer(shape, baseColor)
    )
}
