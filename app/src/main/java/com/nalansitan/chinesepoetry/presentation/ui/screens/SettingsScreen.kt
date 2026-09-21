package com.nalansitan.chinesepoetry.presentation.ui.screens

import com.nalansitan.chinesepoetry.BuildConfig
import android.content.Intent
import android.os.Process
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextRotationNone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nalansitan.chinesepoetry.presentation.ui.components.PoetryAboutDialog
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.viewmodel.AppUpdateUiStatus
import com.nalansitan.chinesepoetry.presentation.viewmodel.SettingsViewModel
import com.nalansitan.chinesepoetry.presentation.viewmodel.UpdateStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showServerConfigDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = PoetryColors.InkBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoetryColors.PaperWhite,
                    titleContentColor = PoetryColors.InkBlack
                )
            )
        },
        containerColor = PoetryColors.PaperWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 阅读设置
            SettingsSection(title = "阅读设置") {
                // 字体大小
                SettingsItem(
                    icon = Icons.Default.FormatSize,
                    title = "字体大小",
                    subtitle = when (uiState.fontSize) {
                        0 -> "小"
                        1 -> "中"
                        2 -> "大"
                        else -> "中"
                    },
                    onClick = { showFontSizeDialog = true }
                )

                Divider(color = PoetryColors.DividerColor)

                // 竖排显示
                SettingsSwitchItem(
                    icon = Icons.Default.TextRotationNone,
                    title = "竖排显示",
                    subtitle = "从右到左阅读",
                    checked = uiState.isVerticalLayout,
                    onCheckedChange = viewModel::setVerticalLayout
                )

                Divider(color = PoetryColors.DividerColor)

                // 夜间模式
                SettingsItem(
                    icon = Icons.Default.Brightness4,
                    title = "夜间模式",
                    subtitle = when (uiState.darkMode) {
                        -1 -> "跟随系统"
                        0 -> "关闭"
                        1 -> "开启"
                        else -> "跟随系统"
                    },
                    onClick = { showDarkModeDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 数据管理
            SettingsSection(title = "数据管理") {
                // 检查更新
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "检查数据更新",
                    showBadge = uiState.hasPendingUpdate,
                    subtitle = when (val status = uiState.updateStatus) {
                        is UpdateStatus.Idle -> "当前版本: ${uiState.dataVersion}"
                        is UpdateStatus.Checking -> "正在检查更新..."
                        is UpdateStatus.NoUpdate -> "已是最新版本"
                        is UpdateStatus.HasUpdate -> "发现新版本 ${status.newVersion}"
                        is UpdateStatus.Downloading -> "正在下载更新... ${status.progress}%"
                        is UpdateStatus.Success -> "更新完成"
                        is UpdateStatus.Error -> "更新失败: ${status.message}"
                    },
                    onClick = { viewModel.checkForUpdate() }
                )

                // 显示更新按钮（当有新版本时）
                if (uiState.updateStatus is UpdateStatus.HasUpdate) {
                    Divider(color = PoetryColors.DividerColor)
                    SettingsActionItem(
                        icon = Icons.Default.Download,
                        title = "立即更新",
                        subtitle = (uiState.updateStatus as UpdateStatus.HasUpdate).changelog,
                        actionText = "下载",
                        onAction = { viewModel.startUpdate() }
                    )
                }

                // 显示更新进度（下载中）
                if (uiState.updateStatus is UpdateStatus.Downloading) {
                    Divider(color = PoetryColors.DividerColor)
                    SettingsProgressItem(
                        progress = (uiState.updateStatus as UpdateStatus.Downloading).progress,
                        title = "正在下载更新..."
                    )
                }

                Divider(color = PoetryColors.DividerColor)

                // 清除并重新下载
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "清除并重新下载数据",
                    subtitle = "删除本地数据库，重新从服务器下载",
                    onClick = { showClearDataDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsSection(title = "应用更新") {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "检查应用更新",
                    showBadge = uiState.hasPendingAppUpdate,
                    subtitle = when (val status = uiState.appUpdateStatus) {
                        is AppUpdateUiStatus.Idle -> "当前应用版本 ${uiState.appVersionName}"
                        is AppUpdateUiStatus.Checking -> "正在检查应用更新..."
                        is AppUpdateUiStatus.NoUpdate -> "当前已是最新版本 ${status.currentVersionName}"
                        is AppUpdateUiStatus.HasUpdate -> "发现新版本 ${status.versionName}"
                        is AppUpdateUiStatus.Downloading -> "正在下载 ${status.versionName}（${status.progress}%）"
                        is AppUpdateUiStatus.InstallPermissionRequired -> "需要授权安装未知来源应用"
                        is AppUpdateUiStatus.ReadyToInstall -> "下载完成，正在准备安装 ${status.versionName}"
                        is AppUpdateUiStatus.Error -> "检查失败: ${status.message}"
                    },
                    onClick = { viewModel.checkForAppUpdate() }
                )

                when (val status = uiState.appUpdateStatus) {
                    is AppUpdateUiStatus.HasUpdate -> {
                        Divider(color = PoetryColors.DividerColor)
                        SettingsActionItem(
                            icon = Icons.Default.Download,
                            title = "立即更新应用",
                            subtitle = status.changelog,
                            actionText = "下载",
                            onAction = { viewModel.startAppUpdate() }
                        )
                    }
                    is AppUpdateUiStatus.InstallPermissionRequired -> {
                        Divider(color = PoetryColors.DividerColor)
                        SettingsActionItem(
                            icon = Icons.Default.Info,
                            title = "允许安装更新",
                            subtitle = "请先授权本应用安装未知来源应用，然后重新下载",
                            actionText = "去授权",
                            onAction = { viewModel.startAppUpdate() }
                        )
                    }
                    is AppUpdateUiStatus.ReadyToInstall -> {
                        Divider(color = PoetryColors.DividerColor)
                        SettingsActionItem(
                            icon = Icons.Default.Download,
                            title = "立即安装更新",
                            subtitle = status.changelog.ifBlank { "下载已完成，若系统没有自动弹出安装页，可点这里继续安装" },
                            actionText = "安装",
                            onAction = { viewModel.startAppUpdate() }
                        )
                    }
                    else -> Unit
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 服务器配置
            SettingsSection(title = "服务器配置") {
                SettingsItem(
                    icon = Icons.Default.Cloud,
                    title = "服务器地址",
                    subtitle = uiState.serverUrl.ifBlank { "当前地址：默认" }.let { subtitle ->
                        if (uiState.serverUrl.isBlank()) subtitle else "当前地址：${uiState.serverUrl}"
                    },
                    onClick = { showServerConfigDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 关于
            SettingsSection(title = "关于") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于古诗词",
                    subtitle = "版本 ${BuildConfig.VERSION_NAME}",
                    onClick = { showAboutDialog = true }
                )
            }
        }
    }

    // 字体大小选择对话框
    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = uiState.fontSize,
            onDismiss = { showFontSizeDialog = false },
            onSelect = {
                viewModel.setFontSize(it)
                showFontSizeDialog = false
            }
        )
    }

    // 夜间模式选择对话框
    if (showDarkModeDialog) {
        DarkModeDialog(
            currentMode = uiState.darkMode,
            onDismiss = { showDarkModeDialog = false },
            onSelect = {
                viewModel.setDarkMode(it)
                showDarkModeDialog = false
            }
        )
    }

    // 服务器配置对话框
    if (showServerConfigDialog) {
        ServerConfigDialog(
            currentUrl = uiState.serverUrl,
            currentUsername = uiState.serverUsername,
            currentPassword = uiState.serverPassword,
            onDismiss = { showServerConfigDialog = false },
            onSave = { url, username, password ->
                viewModel.setServerConfig(url, username, password)
                showServerConfigDialog = false
            },
            onReset = {
                viewModel.resetServerConfig()
                showServerConfigDialog = false
            }
        )
    }

    // 关于对话框
    if (showAboutDialog) {
        PoetryAboutDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = { showAboutDialog = false }
        )
    }

    // 清除数据确认对话框
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清除并重新下载") },
            text = {
                Text("确定要清除本地诗词数据库吗？\n\n清除后将重启应用并重新下载数据。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDataDialog = false
                        viewModel.clearDatabase {
                            // 完整重启进程，确保 Hilt/Room 单例和分页缓存全部释放
                            val activity = context as? android.app.Activity
                            activity?.let {
                                val launchIntent = Intent.makeRestartActivityTask(it.componentName)
                                it.startActivity(launchIntent)
                                it.finishAffinity()
                                Process.killProcess(Process.myPid())
                            }
                        }
                    }
                ) {
                    Text("确认清除", color = PoetryColors.CinnabarRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = PoetryColors.MediumGray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Column {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoetryColors.InkBlack,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PoetryColors.InkBlack
                )

                if (showBadge) {
                    Badge(containerColor = PoetryColors.CinnabarRed)
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PoetryColors.MediumGray
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoetryColors.InkBlack,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.InkBlack
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PoetryColors.MediumGray
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择字体大小") },
        text = {
            Column {
                listOf("小" to 0, "中" to 1, "大" to 2).forEach { (name, size) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSize == size,
                            onClick = { onSelect(size) }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DarkModeDialog(
    currentMode: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题模式") },
        text = {
            Column {
                listOf(
                    "跟随系统" to -1,
                    "浅色模式" to 0,
                    "深色模式" to 1
                ).forEach { (name, mode) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onSelect(mode) }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoetryColors.CinnabarRed,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.InkBlack
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = PoetryColors.MediumGray
            )
        }

        TextButton(onClick = onAction) {
            Text(
                text = actionText,
                color = PoetryColors.CinnabarRed
            )
        }
    }
}

@Composable
private fun SettingsProgressItem(
    progress: Int,
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.InkBlack,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = PoetryColors.CinnabarRed,
            trackColor = PoetryColors.DividerColor
        )
    }
}

@Composable
private fun ServerConfigDialog(
    currentUrl: String,
    currentUsername: String,
    currentPassword: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onReset: () -> Unit
) {
    var url by remember { mutableStateOf(currentUrl) }
    var username by remember { mutableStateOf(currentUsername) }
    var password by remember { mutableStateOf(currentPassword) }
    var showPassword by remember { mutableStateOf(false) }
    var hasUserEditedPassword by remember { mutableStateOf(false) }
    val canTogglePasswordVisibility = hasUserEditedPassword || currentPassword.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("服务器配置") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("https://your-server.com/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        hasUserEditedPassword = true
                    },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = if (showPassword && canTogglePasswordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (canTogglePasswordVisibility) {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                                )
                            }
                        }
                    },
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
            TextButton(onClick = { onSave(url, username, password) }) {
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
