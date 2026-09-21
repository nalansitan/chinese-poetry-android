package com.nalansitan.chinesepoetry.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors

@Composable
fun PoetryAboutDialog(
    versionName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PoetryColors.PaperWhite,
        titleContentColor = PoetryColors.InkBlack,
        textContentColor = PoetryColors.DarkBrown,
        title = {
            Text(
                text = "古诗词",
                style = MaterialTheme.typography.headlineSmall,
                color = PoetryColors.InkBlack
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "一应古诗词收录与阅读应用，\n收录唐诗、宋诗、宋词、诗经、论语等 35万+ 首古诗词。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoetryColors.DarkBrown
                )

                HorizontalDivider(color = PoetryColors.DividerColor)

                AboutInfoRow("版本", versionName)
                AboutInfoRow("作者", "纳兰斯坦、爱因容若")
                AboutInfoRow("数据来源", "开源项目")

                HorizontalDivider(color = PoetryColors.DividerColor)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Made with ",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoetryColors.MediumGray
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = PoetryColors.CinnabarRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = " by 纳兰斯坦",
                        style = MaterialTheme.typography.bodySmall,
                        color = PoetryColors.MediumGray
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = PoetryColors.CinnabarRed)
            }
        }
    )
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.MediumGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.InkBlack
        )
    }
}
