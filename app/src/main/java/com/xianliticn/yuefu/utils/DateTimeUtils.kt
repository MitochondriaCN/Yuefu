package com.xianliticn.yuefu.utils

import android.text.format.DateUtils
import java.time.Instant

/**
 * 获取与当前系统时区相关的友好字符串。
 */
fun Instant.toFriendlyString(): String {
    val timeInMillis = this.toEpochMilli()
    val nowInMillis = System.currentTimeMillis()

    return DateUtils.getRelativeTimeSpanString(
        timeInMillis,
        nowInMillis,
        DateUtils.MINUTE_IN_MILLIS, // 最小分辨率为分钟，小于一分钟显示"刚刚"或"0分钟前"
        DateUtils.FORMAT_SHOW_DATE
                or DateUtils.FORMAT_SHOW_YEAR
                or DateUtils.FORMAT_ABBREV_ALL
    ).toString()
}