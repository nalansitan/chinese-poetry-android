package com.nalansitan.chinesepoetry.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户活动实体 - 收藏和浏览历史
 */
@Entity(
    tableName = "user_activities",
    foreignKeys = [
        ForeignKey(
            entity = PoemEntity::class,
            parentColumns = ["id"],
            childColumns = ["poem_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["poem_id"]),
        Index(value = ["type"]),
        Index(value = ["timestamp"])
    ]
)
data class UserActivityEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = java.util.UUID.randomUUID().toString(),

    @ColumnInfo(name = "poem_id")
    val poemId: String,

    @ColumnInfo(name = "type")
    val type: String, // FAVORITE, HISTORY

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 活动类型枚举
 */
enum class ActivityType(val value: String) {
    FAVORITE("favorite"),
    HISTORY("history");

    companion object {
        fun fromValue(value: String): ActivityType {
            return entries.find { it.value == value } ?: HISTORY
        }
    }
}
