package com.xianliticn.yuefu.utils

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil.size.Size
import coil.transform.Transformation

/**
 * 一个通用的 Coil 变换，用于应用亮度和对比度。
 * @param brightness 亮度值，从 -1.0f (黑) 到 1.0f (白)。0f 是默认值。
 * @param contrast 对比度值。1.0f 是默认值。大于1增加对比度，小于1减少对比度。
 * @param saturation 饱和度值。1.0f 是默认值。0f 是灰度。
 */
class ColorFilterTransformation(
    private val brightness: Float = 0f,
    private val contrast: Float = 1f,
    private val saturation: Float = 1f,
) : Transformation {

    // 为这个 Transformation 创建一个唯一的 key，Coil 用它来缓存结果
    override val cacheKey: String = "${javaClass.name}-b$brightness-c$contrast-s$saturation"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (brightness == 0f && contrast == 1f && saturation == 1f) {
            return input // 如果没有变化，直接返回原图
        }

        val softwareBitmap = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            input
        }

        val output =
            createBitmap(input.width, input.height, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply {
            isAntiAlias = true
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                // 设置饱和度
                setSaturation(saturation)

                // 叠加亮度和对比度矩阵
                // 注意：顺序很重要
                postConcat(ColorMatrix().apply {
                    // 对比度矩阵
                    val scale = contrast
                    setScale(scale, scale, scale, 1f)

                    // 亮度矩阵
                    val translate = (-0.5f * scale + 0.5f + brightness) * 255f
                    postConcat(
                        ColorMatrix(
                            floatArrayOf(
                                1f, 0f, 0f, 0f, translate,
                                0f, 1f, 0f, 0f, translate,
                                0f, 0f, 1f, 0f, translate,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                })
            })
        }

        output.applyCanvas {
            drawBitmap(softwareBitmap, 0f, 0f, paint)
        }

        return output
    }
}