package com.nalansitan.chinesepoetry.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.presentation.ui.components.EmptyState
import com.nalansitan.chinesepoetry.presentation.ui.components.LoadingState
import com.nalansitan.chinesepoetry.presentation.ui.components.PoemCard
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.viewmodel.PoemListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemListScreen(
    title: String,
    onBackClick: () -> Unit,
    onPoemClick: (String) -> Unit,
    viewModel: PoemListViewModel = hiltViewModel()
) {
    val poems = viewModel.poems.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
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
        PoemListContent(
            poems = poems,
            onPoemClick = onPoemClick,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun PoemListContent(
    poems: LazyPagingItems<Poem>,
    onPoemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (poems.loadState.refresh) {
        is LoadState.Loading -> {
            LoadingState(modifier = modifier.fillMaxSize())
        }
        is LoadState.Error -> {
            val error = (poems.loadState.refresh as LoadState.Error).error
            EmptyState(
                icon = Icons.Default.Warning,
                title = "加载失败",
                subtitle = error.localizedMessage ?: "数据加载异常，请尝试重新打开",
                modifier = modifier.fillMaxSize()
            )
        }
        else -> {
            if (poems.itemCount == 0) {
                EmptyState(
                    icon = Icons.Default.Book,
                    title = "暂无数据",
                    subtitle = "该分类下暂时没有诗词",
                    modifier = modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(poems.itemCount) { index ->
                        val poem = poems[index]
                        if (poem != null) {
                            PoemCard(
                                poem = poem,
                                onClick = { onPoemClick(poem.id) }
                            )
                        }
                    }

                    // 加载更多状态
                    when (poems.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                LoadingState(
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                Text(
                                    text = "加载更多失败",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PoetryColors.MediumGray,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
