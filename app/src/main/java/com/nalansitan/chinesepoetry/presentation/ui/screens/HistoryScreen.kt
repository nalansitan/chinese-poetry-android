package com.nalansitan.chinesepoetry.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.nalansitan.chinesepoetry.presentation.ui.components.EmptyState
import com.nalansitan.chinesepoetry.presentation.ui.components.LoadingState
import com.nalansitan.chinesepoetry.presentation.ui.components.PoemCard
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    onPoemClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyPoems = viewModel.historyPoems.collectAsLazyPagingItems()
    val historyCount by viewModel.historyCount.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "阅读历史 ($historyCount)",
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
                actions = {
                    if (historyCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清空历史",
                                tint = PoetryColors.CinnabarRed
                            )
                        }
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
        when (historyPoems.loadState.refresh) {
            is LoadState.Loading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is LoadState.Error -> {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "加载失败",
                    subtitle = (historyPoems.loadState.refresh as LoadState.Error).error.localizedMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                if (historyPoems.itemCount == 0) {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "暂无阅读历史",
                        subtitle = "打开诗词详情后，这里会记录你的阅读足迹",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyPoems.itemCount) { index ->
                            val poem = historyPoems[index]
                            if (poem != null) {
                                PoemCard(
                                    poem = poem,
                                    onClick = { onPoemClick(poem.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空阅读历史") },
            text = { Text("确定要清空全部阅读历史吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearHistory()
                    }
                ) {
                    Text("确认清空", color = PoetryColors.CinnabarRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
