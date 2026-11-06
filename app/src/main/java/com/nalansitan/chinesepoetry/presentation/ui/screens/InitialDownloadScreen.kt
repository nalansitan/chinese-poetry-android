package com.nalansitan.chinesepoetry.presentation.ui.screens

import com.nalansitan.chinesepoetry.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nalansitan.chinesepoetry.data.manager.DownloadState
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.viewmodel.InitialDownloadViewModel

/**
 * 初始数据下载页面
 * 首次启动时显示，用于下载古诗词数据库
 */
@Composable
fun InitialDownloadScreen(
    onDownloadComplete: () -> Unit,
    viewModel: InitialDownloadViewModel = hiltViewModel()
) {
    val downloadState by viewModel.downloadState.collectAsState()
    val databaseSize by viewModel.databaseSize.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    var showServerConfigDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importDatabase)
    }

    Scaffold(
        containerColor = PoetryColors.PaperWhite
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 右上角设置按钮
            if (BuildConfig.DATABASE_URL.isBlank()) IconButton(
                onClick = { showServerConfigDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "服务器设置",
                    tint = PoetryColors.MediumGray
                )
            }

            // 主内容
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            // 图标
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = PoetryColors.CinnabarRed
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "古诗词",
                style = MaterialTheme.typography.displayMedium,
                color = PoetryColors.InkBlack
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题
            Text(
                text = "品味古典诗词之美",
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.MediumGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 作者
            Text(
                text = "纳兰斯坦 · 爱因容若",
                style = MaterialTheme.typography.bodyMedium,
                color = PoetryColors.LightInk
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 根据状态显示不同内容
            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    IdleContent(
                        databaseSize = databaseSize,
                        onDownloadClick = { viewModel.startDownload() }
                    )
                }

                is DownloadState.Downloading -> {
                    DownloadingContent(
                        progress = state.progress,
                        speedBytesPerSec = state.speedBytesPerSec,
                        downloadedBytes = state.downloadedBytes,
                        totalBytes = state.totalBytes
                    )
                }

                is DownloadState.Installing -> {
                    CircularProgressIndicator(color = PoetryColors.CinnabarRed)
                    Text("正在解压、校验并安装诗词数据…")
                }

                is DownloadState.Success -> {
                    SuccessContent(onContinue = onDownloadComplete)
                }

                is DownloadState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.startDownload() }
                    )
                }
                }
                if (downloadState is DownloadState.Idle || downloadState is DownloadState.Error) {
                    TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Text("从本地导入数据包")
                    }
                    Text(
                        "可在电脑下载 .db.gz 或 .db 文件，传到手机后导入",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = PoetryColors.MediumGray
                    )
                }
            }
        }
    }

    // 服务器配置对话框
    if (showServerConfigDialog) {
        InitialServerConfigDialog(
            currentUrl = serverUrl,
            onDismiss = { showServerConfigDialog = false },
            onSave = { url ->
                viewModel.setServerConfig(url)
                showServerConfigDialog = false
            },
            onReset = {
                viewModel.resetServerConfig()
                showServerConfigDialog = false
            }
        )
    }
}

@Composable
private fun InitialServerConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onReset: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器配置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (currentUrl.isBlank()) {
                        "当前地址：默认"
                    } else {
                        "当前地址：$currentUrl"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PoetryColors.MediumGray
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://your-server.com/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "留空则使用内置配置",
                    style = MaterialTheme.typography.bodySmall,
                    color = PoetryColors.MediumGray
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text("恢复默认")
            }
        }
    )
}

@Composable
private fun IdleContent(
    databaseSize: String,
    onDownloadClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "首次使用需要下载诗词数据",
            style = MaterialTheme.typography.bodyLarge,
            color = PoetryColors.DarkBrown,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "数据大小: $databaseSize",
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.MediumGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDownloadClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PoetryColors.CinnabarRed
            )
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("下载诗词数据")
        }
    }
}

@Composable
private fun DownloadingContent(progress: Int, speedBytesPerSec: Long, downloadedBytes: Long, totalBytes: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.size(80.dp),
            color = PoetryColors.CinnabarRed
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "正在下载诗词数据...",
            style = MaterialTheme.typography.bodyLarge,
            color = PoetryColors.DarkBrown
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$progress%",
            style = MaterialTheme.typography.headlineMedium,
            color = PoetryColors.CinnabarRed
        )

        if (speedBytesPerSec > 0 || downloadedBytes > 0) {
            Spacer(modifier = Modifier.height(8.dp))

            val speedText = formatSpeed(speedBytesPerSec)
            val downloadedText = formatFileSize(downloadedBytes)
            val totalText = if (totalBytes > 0) formatFileSize(totalBytes) else "?"

            Text(
                text = "$downloadedText / $totalText  ·  $speedText",
                style = MaterialTheme.typography.bodySmall,
                color = PoetryColors.MediumGray
            )
        }
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024 -> "%.1f KB/s".format(bytesPerSec / 1024.0)
        else -> "%.1f MB/s".format(bytesPerSec / (1024.0 * 1024.0))
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

@Composable
private fun SuccessContent(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "数据准备完成！",
            style = MaterialTheme.typography.headlineSmall,
            color = PoetryColors.Indigo
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "共收录 35万+ 首古诗词",
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.MediumGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PoetryColors.CinnabarRed
            )
        ) {
            Text("开始使用")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "数据初始化失败",
            style = MaterialTheme.typography.headlineSmall,
            color = PoetryColors.CinnabarRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.MediumGray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "网络不畅时可重试续传，或导入本地数据包",
            style = MaterialTheme.typography.bodySmall,
            color = PoetryColors.LightInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PoetryColors.CinnabarRed
            )
        ) {
            Text("重试")
        }
    }
}
