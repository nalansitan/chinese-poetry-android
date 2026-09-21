package com.nalansitan.chinesepoetry.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nalansitan.chinesepoetry.domain.model.Author
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors

/**
 * 作者项组件
 */
@Composable
fun AuthorItem(
    author: Author,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像（使用首字）
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(PoetryColors.CinnabarRed.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = author.name.firstOrNull()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineMedium,
                color = PoetryColors.CinnabarRed
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 姓名
        Text(
            text = author.name,
            style = MaterialTheme.typography.labelLarge,
            color = PoetryColors.InkBlack,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // 朝代
        Text(
            text = "[${author.dynasty.displayName}]",
            style = MaterialTheme.typography.labelSmall,
            color = PoetryColors.MediumGray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 作者卡片（大）
 */
@Composable
fun AuthorCard(
    author: Author,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .clickable(onClick = onClick),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = PoetryColors.LightGray
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 头像
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PoetryColors.CinnabarRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = author.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PoetryColors.CinnabarRed
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PoetryColors.InkBlack
                    )

                    Text(
                        text = "${author.dynasty.displayName} · ${author.poemCount}首",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoetryColors.MediumGray
                    )
                }
            }

            // 简介
            if (!author.shortIntro.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = author.shortIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = PoetryColors.LightInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
