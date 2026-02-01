package com.xianliticn.yuefu.utils

import android.graphics.Color
import kotlin.random.Random

/**
 * 随机获得一个非常好看的颜色，而且是较浅色
 */
fun getRandomPrettyColor(): Int {
    val red = Random.nextInt(150, 200)
    val green = Random.nextInt(150, 200)
    val blue = Random.nextInt(150, 200)
    // 返回不透明的ARGB颜色 (alpha=255)
    return Color.argb(255, red, green, blue)
}