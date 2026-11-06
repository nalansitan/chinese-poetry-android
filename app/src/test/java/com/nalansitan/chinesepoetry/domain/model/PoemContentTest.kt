package com.nalansitan.chinesepoetry.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PoemContentTest {
    @Test fun splitsNewlineDatabaseContentIntoPoemLines() {
        assertEquals(
            listOf("秦川雄帝宅，函谷壮皇居。", "绮殿千寻起，离宫百雉余。"),
            splitPoemContent("秦川雄帝宅，函谷壮皇居。\n绮殿千寻起，离宫百雉余。")
        )
    }

    @Test fun preservesPipeSeparatedDatabaseSupport() {
        assertEquals(listOf("床前明月光，", "疑是地上霜。"), splitPoemContent("床前明月光，|疑是地上霜。"))
    }

    @Test fun ignoresEmptyLinesAcrossMixedLineEndings() {
        assertEquals(listOf("甲", "乙", "丙"), splitPoemContent("\r\n甲\r\n\r\n乙|\n丙\n"))
    }
}
