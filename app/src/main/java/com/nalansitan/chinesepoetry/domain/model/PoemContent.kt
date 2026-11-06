package com.nalansitan.chinesepoetry.domain.model

/** 兼容原始数据库的换行分段，以及转换器使用的竖线分段。 */
internal fun splitPoemContent(content: String): List<String> =
    content.split('|', '\n', '\r').filter { it.isNotBlank() }
