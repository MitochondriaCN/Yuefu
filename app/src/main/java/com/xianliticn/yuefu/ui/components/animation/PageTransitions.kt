package com.xianliticn.yuefu.ui.components.animation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

// 页面进入：从右滑入 + 淡入（弹簧动画，更自然）
fun slideInFromRight(): EnterTransition =
    fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + slideInHorizontally(
        initialOffsetX = { it / 4 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

// 页面退出：向左滑出 + 淡出
fun slideOutToLeft(): ExitTransition =
    fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
        targetOffsetX = { -it / 6 },
        animationSpec = tween(200)
    )

// 返回进入：从左滑入 + 淡入
fun slideInFromLeft(): EnterTransition =
    fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + slideInHorizontally(
        initialOffsetX = { -it / 4 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

// 返回退出：向右滑出 + 淡出
fun slideOutToRight(): ExitTransition =
    fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
        targetOffsetX = { it / 6 },
        animationSpec = tween(200)
    )

// Tab 切换：垂直滑入 + 淡入（短距离弹簧）
fun tabEnterTransition(): EnterTransition =
    fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMedium)
    ) + slideInVertically(
        initialOffsetY = { it / 8 },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

// Tab 退出：淡出
fun tabExitTransition(): ExitTransition =
    fadeOut(animationSpec = tween(150))

// 缩放淡入（参考 GSAP fadeScale）
fun scaleFadeIn(): EnterTransition =
    fadeIn(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

// 缩放淡出
fun scaleFadeOut(): ExitTransition =
    fadeOut(animationSpec = tween(200)) + scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(200)
    )

// 构建标准 ContentTransform：进入+退出
fun standardContentTransform(): ContentTransform =
    slideInFromRight() togetherWith slideOutToLeft()

// 返回 ContentTransform
fun popContentTransform(): ContentTransform =
    slideInFromLeft() togetherWith slideOutToRight()

// Tab 切换 ContentTransform
fun tabContentTransform(): ContentTransform =
    tabEnterTransition() togetherWith tabExitTransition()
