package com.nalansitan.chinesepoetry.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// 古风配色方案
object PoetryColors {
    // 主色调
    val PaperWhite = Color(0xFFFAF9F6)      // 宣纸白 - 主背景
    val InkBlack = Color(0xFF2F2F2F)        // 墨黑 - 主文字
    val CinnabarRed = Color(0xFFB22222)     // 朱砂红 - 强调色
    val Indigo = Color(0xFF4A6741)          // 青黛 - 辅助色
    val PaleGold = Color(0xFFD4AF37)        // 淡金 - 装饰色
    
    // 辅助色
    val LightGray = Color(0xFFF5F5F0)       // 浅灰 - 卡片背景
    val WarmBeige = Color(0xFFF5F5DC)       // 米白 - 次要背景
    val DarkBrown = Color(0xFF5C4033)       // 深褐 - 深色文字
    val MediumGray = Color(0xFF888888)      // 中灰 - 次要文字
    val LightInk = Color(0xFF666666)        // 淡墨 - 提示文字
    
    // 特殊效果
    val ShadowGray = Color(0x1F000000)      // 阴影
    val DividerColor = Color(0xFFE0E0E0)    // 分割线
    val HighlightRed = Color(0xFFE53935)    // 高亮红
}

// 夜间模式配色
object PoetryDarkColors {
    val Background = Color(0xFF1A1A1A)      // 深色背景
    val Surface = Color(0xFF2D2D2D)         // 卡片背景
    val OnBackground = Color(0xFFE8E8E8)    // 主文字
    val OnSurface = Color(0xFFB0B0B0)       // 次要文字
    val Accent = Color(0xFFE57373)          // 强调色（浅朱砂）
}
