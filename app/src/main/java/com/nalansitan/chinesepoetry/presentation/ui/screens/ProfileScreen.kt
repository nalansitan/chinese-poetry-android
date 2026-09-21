package com.nalansitan.chinesepoetry.presentation.ui.screens

import com.nalansitan.chinesepoetry.BuildConfig
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.nalansitan.chinesepoetry.presentation.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    hasPendingUpdate: Boolean,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val historyCount by viewModel.historyCount.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()
    val readingDaysCount by viewModel.readingDaysCount.collectAsState()
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineMedium
                    )
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
            // 用户统计卡片
            UserStatsCard(
                historyCount = historyCount,
                favoriteCount = favoriteCount,
                readingDaysCount = readingDaysCount
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 菜单列表
            MenuSection(
                items = listOf(
                    MenuItem("阅读历史", Icons.Default.History, onHistoryClick),
                    MenuItem("我的收藏", Icons.Default.Favorite, onFavoritesClick),
                    MenuItem("设置", Icons.Default.Settings, onSettingsClick, showBadge = hasPendingUpdate),
                    MenuItem("关于", Icons.Default.Info, onClick = { showAboutDialog = true })
                )
            )
        }
    }

    if (showAboutDialog) {
        PoetryAboutDialog(
            versionName = BuildConfig.VERSION_NAME,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun UserStatsCard(
    historyCount: Int,
    favoriteCount: Int,
    readingDaysCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 头像和用户名区域
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PoetryColors.CinnabarRed
            )
            
            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = "诗词爱好者",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PoetryColors.InkBlack
                )
                
                Text(
                    text = "品味古典诗词之美",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PoetryColors.MediumGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 统计数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(icon = Icons.Default.Book, label = "浏览诗词", value = historyCount.toString())
            StatItem(icon = Icons.Default.Favorite, label = "收藏", value = favoriteCount.toString())
            StatItem(icon = Icons.Default.History, label = "阅读天数", value = readingDaysCount.toString())
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PoetryColors.CinnabarRed,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = PoetryColors.InkBlack
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PoetryColors.MediumGray
        )
    }
}

@Composable
private fun MenuSection(items: List<MenuItem>) {
    Column {
        items.forEachIndexed { index, item ->
            MenuItemRow(item)
            
            if (index < items.size - 1) {
                Divider(
                    color = PoetryColors.DividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = item.onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = PoetryColors.InkBlack,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            color = PoetryColors.InkBlack,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        if (item.showBadge) {
            BadgedBox(
                badge = {
                    Badge(containerColor = PoetryColors.CinnabarRed)
                },
                modifier = Modifier.padding(end = 12.dp)
            ) {}
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PoetryColors.MediumGray
        )
    }
}

private data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val showBadge: Boolean = false
)
