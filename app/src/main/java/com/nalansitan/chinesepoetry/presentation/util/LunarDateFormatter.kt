package com.nalansitan.chinesepoetry.presentation.util

import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object LunarDateFormatter {

    private val heavenlyStems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val earthlyBranches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val zodiacAnimals = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
    private val lunarMonths = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
    private val lunarDays = listOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )
    private val gregorianFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.SIMPLIFIED_CHINESE)
    private val weekdays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    fun formatToday(): String {
        val today = LocalDate.now()
        val gregorianText = today.format(gregorianFormatter)
        val weekdayText = weekdays[today.dayOfWeek.ordinal]
        val chineseCalendar = ChineseCalendar(Locale.SIMPLIFIED_CHINESE)
        val cyclicalYear = chineseCalendar.get(Calendar.YEAR)
        val monthIndex = chineseCalendar.get(Calendar.MONTH)
        val dayIndex = chineseCalendar.get(Calendar.DAY_OF_MONTH)
        val isLeapMonth = chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1

        val stem = heavenlyStems[(cyclicalYear - 1) % heavenlyStems.size]
        val branchIndex = (cyclicalYear - 1) % earthlyBranches.size
        val branch = earthlyBranches[branchIndex]
        val zodiac = zodiacAnimals[branchIndex]
        val monthText = lunarMonths.getOrElse(monthIndex) { "${monthIndex + 1}月" }
        val dayText = lunarDays.getOrElse(dayIndex - 1) { dayIndex.toString() }

        return buildString {
            append(gregorianText)
            append(" ")
            append(weekdayText)
            append(" · ")
            append(stem)
            append(branch)
            append(zodiac)
            append("年 ")
            if (isLeapMonth) append("闰")
            append(monthText)
            append(dayText)
        }
    }
}
