package com.nalansitan.chinesepoetry.presentation.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val openLink: (String) -> Unit = { url ->
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "未找到可打开链接的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
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
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutLinkRow(
                        title = "项目 GitHub",
                        subtitle = "源码 · 版本发布 · 问题反馈",
                        icon = Icons.Default.Code,
                        onClick = { openLink("https://github.com/nalansitan/chinese-poetry-android") }
                    )
                    AboutLinkRow(
                        title = "作者 GitHub",
                        subtitle = "@nalansitan",
                        icon = Icons.Default.Person,
                        onClick = { openLink("https://github.com/nalansitan") }
                    )
                }

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

@Composable
private fun AboutLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PoetryColors.LightGray
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = PoetryColors.CinnabarRed,
                modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = PoetryColors.InkBlack)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PoetryColors.LightInk)
            }
            Icon(Icons.Default.OpenInNew, contentDescription = "打开外部链接",
                tint = PoetryColors.LightInk, modifier = Modifier.size(18.dp))
        }
    }
}
