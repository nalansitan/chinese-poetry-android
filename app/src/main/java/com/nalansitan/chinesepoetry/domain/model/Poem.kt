package com.nalansitan.chinesepoetry.domain.model

import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.data.local.entity.PoemType

/**
 * 诗词领域模型
 */
data class Poem(
    val id: String,
    val title: String,
    val authorName: String,
    val authorId: String?,
    val dynasty: Dynasty,
    val content: List<String>, // 已分割的段落
    val type: PoemType,
    val rhythmic: String?,
    val chapter: String?,
    val section: String?,
    val comment: String?,
    val appreciation: String?,
    val notes: String?,
    val translation: String?,
    val isFavorite: Boolean,
    val createdAt: Long
) {
    /**
     * 获取完整内容文本
     */
    fun getFullContent(): String = content.joinToString("\n")

    /**
     * 获取显示标题（带词牌名）
     */
    fun getDisplayTitle(): String {
        return if (rhythmic != null && type == PoemType.SONG_CI) {
            "$rhythmic · $title"
        } else {
            title
        }
    }

    /**
     * 获取作者信息展示
     */
    fun getAuthorDisplay(): String {
        return "[${dynasty.displayName}] $authorName"
    }
}

/**
 * 作者领域模型
 */
data class Author(
    val id: String,
    val name: String,
    val dynasty: Dynasty,
    val intro: String?,
    val shortIntro: String?,
    val poemCount: Int
)

/**
 * 诗词分类
 */
data class PoemCategory(
    val type: PoemType,
    val name: String,
    val icon: String,
    val count: Int = 0
)

/**
 * 每日推荐
 */
data class DailyPoem(
    val poem: Poem,
    val date: String,
    val quote: String? = null
)
