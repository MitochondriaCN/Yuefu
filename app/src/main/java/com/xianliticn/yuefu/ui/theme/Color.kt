package com.xianliticn.yuefu.ui.theme

import androidx.compose.ui.graphics.Color

// ── 品牌色：蓝紫强调色 ──────────────────────────────────
val BlueAccent = Color(0xFF5266E8)         // 主强调色，CTA、活跃状态
val BlueAccentDark = Color(0xFFB0B8FF)     // Dark Theme 主色
val BlueAccentContainer = Color(0xFFE0E4FF)
val BlueAccentContainerDark = Color(0xFF3A4AC0)

// ── 品牌色：暖黄辅助色 ──────────────────────────────────
val YellowMain = Color(0xFFDDD575)         // 暖黄，装饰、高亮、标签
val YellowMainDark = Color(0xFFE8E0A0)
val YellowLight = Color(0xFFDBD37F)        // 浅黄，背景点缀
val YellowContainer = Color(0xFFF5F0C8)
val YellowContainerDark = Color(0xFF7A7530)

// ── 品牌色：墨绿灰 ──────────────────────────────────────
val GreenDark = Color(0xFF73715C)          // 副标题、装饰文字
val GreenDarkDark = Color(0xFFC8C6B4)
val GreenMuted = Color(0xFFB8BA99)         // 边框、分割线

// ── 全局背景 ────────────────────────────────────────────
val BgLight = Color(0xFFFCFBFA)            // 简洁明亮的底色
val BgDark = Color(0xFF1A1A16)             // 深暖黑（非纯黑）

// ── 文字层级 ────────────────────────────────────────────
val TextPrimary = Color(0xFF2D2D2D)        // 主标题、正文
val TextSecondary = Color(0xFF73715C)      // 副标题、描述（墨绿灰）
val TextMuted = Color(0xFF999999)          // 辅助文字

// ── 状态语义色 ──────────────────────────────────────────
val SuccessGreen = Color(0xFF4CAF50)       // 成功/在线
val SuccessGreenDark = Color(0xFF81C784)
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedDark = Color(0xFFFFB4AB)

// ── 乐谱声部配色（用于 PianoRoll 音符流） ────────────────
val PartColor0 = Color(0xFF4F5BFF)         // 主旋律：更饱和的蓝紫
val PartColor1 = Color(0xFFF0E060)         // 第二声部：更鲜明的暖黄
val PartColor2 = Color(0xFF8FB84A)         // 第三声部：更鲜亮的橄榄绿
val PartColor3 = Color(0xFFE8983A)         // 第四声部：更浓郁的蜜橙

// ── 钢琴键暖色调 ────────────────────────────────────────
val PianoWhiteDefault = Color(0xFFFCFBFA)  // 暖米白，与页面背景同色系
val PianoWhitePressed = Color(0xFFE8E6D9)  // 暖灰按压态
val PianoBlackDefault = Color(0xFF3D3A2A)  // 深棕，温润非纯黑
val PianoBlackPressed = Color(0xFF5A5640)  // 暖深棕按压态
val PianoWhiteGradientTop = Color(0xFFFFFFF8) // 白键渐变顶部
val PianoWhiteGradientBottom = Color(0xFFF2F0E8) // 白键渐变底部
val PianoBlackGradientTop = Color(0xFF4A4735) // 黑键渐变顶部
val PianoBlackGradientBottom = Color(0xFF3D3A2A) // 黑键渐变底部
val PianoBorder = Color(0xFFD5D3C5)        // 暖灰边框

// ── 播放控制栏 ──────────────────────────────────────────
val ControlBarDark = Color(0xFF2D2D28)     // 暖深色（替代纯黑）
