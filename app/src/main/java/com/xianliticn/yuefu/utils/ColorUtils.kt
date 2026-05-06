package com.xianliticn.yuefu.utils

import android.graphics.Color
import kotlin.random.Random

/**
 * 随机获得一个与品牌暖色调协调的浅色
 * 色值范围在暖黄绿区间，与设计文档色系一致
 */
fun getRandomPrettyColor(): Int {
    val red = Random.nextInt(180, 230)
    val green = Random.nextInt(170, 210)
    val blue = Random.nextInt(120, 170)
    // 返回不透明的ARGB颜色 (alpha=255)
    return Color.argb(255, red, green, blue)
}
