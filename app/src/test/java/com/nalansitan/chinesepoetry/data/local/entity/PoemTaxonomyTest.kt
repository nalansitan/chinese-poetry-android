package com.nalansitan.chinesepoetry.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class PoemTaxonomyTest {
    @Test
    fun `converter poem types map without falling back`() {
        val values = listOf(
            "tang_shi", "song_shi", "song_ci", "shi_jing", "lun_yu",
            "chu_ci", "yuan_qu", "nalan_ci", "hua_jian_ji", "nan_tang",
            "cao_cao", "you_meng_ying", "si_shu"
        )

        assertEquals(values, values.map { PoemType.fromValue(it).value })
    }

    @Test
    fun `converter dynasties map without falling back`() {
        val displayNames = listOf("先秦", "三国", "唐", "五代", "宋", "元", "清")

        assertEquals(displayNames, displayNames.map { Dynasty.fromValue(it).displayName })
    }

    @Test
    fun `unknown taxonomy values remain unknown instead of becoming Tang poetry`() {
        assertEquals(PoemType.UNKNOWN, PoemType.fromValue("future_type"))
        assertEquals(Dynasty.UNKNOWN, Dynasty.fromValue("future_dynasty"))
    }
}
