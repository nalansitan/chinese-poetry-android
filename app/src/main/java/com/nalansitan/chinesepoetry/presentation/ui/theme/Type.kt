package com.nalansitan.chinesepoetry.presentation.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nalansitan.chinesepoetry.R

// 字体配置 - 使用系统默认宋体作为回退
val NotoSerifSC = FontFamily.Default

// 古风排版样式
val PoetryTypography = Typography(
    // 大标题 - 用于诗词标题
    displayLarge = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 4.sp
    ),
    displayMedium = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 3.sp
    ),
    displaySmall = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 2.sp
    ),
    
    // 标题 - 用于作者名、分类名
    headlineLarge = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 2.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 1.sp
    ),
    
    // 正文 - 用于诗词内容
    bodyLarge = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 36.sp,
        letterSpacing = 2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp,
        letterSpacing = 1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp
    ),
    
    // 标签 - 用于小标签、提示
    labelLarge = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSerifSC,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// 竖排文字样式
val VerticalTextStyle = TextStyle(
    fontFamily = NotoSerifSC,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    lineHeight = 36.sp,
    letterSpacing = 8.sp
)
