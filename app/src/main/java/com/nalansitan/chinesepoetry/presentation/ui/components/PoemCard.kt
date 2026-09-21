package com.nalansitan.chinesepoetry.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.util.wrapPoemLine

/**
 * 诗词卡片组件
 */
@Composable
fun PoemCard(
    poem: Poem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPreview: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PoetryColors.LightGray
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Text(
                text = poem.getDisplayTitle(),
                style = MaterialTheme.typography.headlineSmall,
                color = PoetryColors.InkBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 作者
            Text(
                text = poem.getAuthorDisplay(),
                style = MaterialTheme.typography.bodySmall,
                color = PoetryColors.MediumGray
            )
            
            // 内容预览
            if (showPreview && poem.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                val previewText = poem.content.take(2).joinToString("，") +
                        if (poem.content.size > 2) "..." else ""
                
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoetryColors.LightInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}

/**
 * 每日一诗卡片（大卡片）
 */
@Composable
fun DailyPoemCard(
    poem: Poem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PoetryColors.WarmBeige
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // 标签
            Text(
                text = "每日一诗",
                style = MaterialTheme.typography.labelMedium,
                color = PoetryColors.CinnabarRed
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 标题
            Text(
                text = poem.title,
                style = MaterialTheme.typography.displaySmall,
                color = PoetryColors.InkBlack
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 作者
            Text(
                text = poem.getAuthorDisplay(),
                style = MaterialTheme.typography.bodyMedium,
                color = PoetryColors.MediumGray
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 内容（显示前4句）
            poem.content.take(4).forEach { line ->
                WrappedPoemCardLine(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PoetryColors.DarkBrown,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun WrappedPoemCardLine(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val wrappedLines = remember(text, style, constraints.maxWidth) {
            wrapPoemLine(
                text = text,
                textMeasurer = textMeasurer,
                style = style,
                maxWidthPx = constraints.maxWidth
            )
        }

        Column {
            wrappedLines.forEach { wrappedLine ->
                Text(
                    text = wrappedLine,
                    style = style,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
