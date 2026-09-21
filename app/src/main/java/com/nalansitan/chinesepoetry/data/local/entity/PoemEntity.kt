package com.nalansitan.chinesepoetry.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 诗词实体类
 */
@Entity(
    tableName = "poems",
    indices = [
        Index(value = ["author_name"]),
        Index(value = ["dynasty"]),
        Index(value = ["type"]),
        Index(value = ["is_favorite"])
    ]
)
data class PoemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author_name")
    val authorName: String,

    @ColumnInfo(name = "author_id")
    val authorId: String? = null,

    @ColumnInfo(name = "dynasty")
    val dynasty: String, // TANG, SONG, FIVE_DYNASTIES, PRE_QIN

    @ColumnInfo(name = "content")
    val content: String, // 段落用 | 分隔

    @ColumnInfo(name = "type")
    val type: String, // TANG_SHI, SONG_SHI, SONG_CI, SHI_JING, LUN_YU, HUA_JIAN_JI

    @ColumnInfo(name = "rhythmic")
    val rhythmic: String? = null, // 词牌名(宋词)或韵律(唐诗)

    @ColumnInfo(name = "chapter")
    val chapter: String? = null, // 章节(诗经/论语)

    @ColumnInfo(name = "section")
    val section: String? = null, // 篇(诗经)

    // 扩展字段 - 预留
    @ColumnInfo(name = "comment")
    val comment: String? = null,

    @ColumnInfo(name = "appreciation")
    val appreciation: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null,

    @ColumnInfo(name = "translation")
    val translation: String? = null,

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 诗词类型枚举
 */
enum class PoemType(val value: String) {
    TANG_SHI("tang_shi"),
    SONG_SHI("song_shi"),
    SONG_CI("song_ci"),
    SHI_JING("shi_jing"),
    LUN_YU("lun_yu"),
    CHU_CI("chu_ci"),
    YUAN_QU("yuan_qu"),
    HUA_JIAN_JI("hua_jian_ji"),
    NAN_TANG("nan_tang"),
    NALAN_CI("nalan_ci"),
    CAO_CAO("cao_cao"),
    YOU_MENG_YING("you_meng_ying"),
    SI_SHU("si_shu"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): PoemType {
            return entries.find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * 朝代枚举
 */
enum class Dynasty(val value: String, val displayName: String) {
    TANG("tang", "唐"),
    SONG("song", "宋"),
    FIVE_DYNASTIES("five_dynasties", "五代"),
    PRE_QIN("pre_qin", "先秦"),
    THREE_KINGDOMS("three_kingdoms", "三国"),
    YUAN("yuan", "元"),
    QING("qing", "清"),
    UNKNOWN("unknown", "未知");

    companion object {
        fun fromValue(value: String): Dynasty {
            return entries.find { dynasty ->
                dynasty.value == value || dynasty.displayName == value
            } ?: UNKNOWN
        }
    }
}
