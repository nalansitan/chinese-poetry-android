package com.nalansitan.chinesepoetry.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 作者实体类
 */
@Entity(
    tableName = "authors",
    indices = [
        Index(value = ["name"]),
        Index(value = ["dynasty"])
    ]
)
data class AuthorEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "dynasty")
    val dynasty: String, // TANG, SONG, FIVE_DYNASTIES, PRE_QIN, QING

    @ColumnInfo(name = "intro")
    val intro: String? = null, // 详细介绍

    @ColumnInfo(name = "short_intro")
    val shortIntro: String? = null, // 简短介绍

    @ColumnInfo(name = "poem_count", defaultValue = "0")
    val poemCount: Int = 0 // 作品数量
)
