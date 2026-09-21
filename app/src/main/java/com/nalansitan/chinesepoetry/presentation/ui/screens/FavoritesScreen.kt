package com.nalansitan.chinesepoetry.presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.nalansitan.chinesepoetry.presentation.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onPoemClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoritePoems = viewModel.favoritePoems.collectAsLazyPagingItems()
    val favoriteCount by viewModel.favoriteCount.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的收藏 ($favoriteCount)",
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
        when (favoritePoems.loadState.refresh) {
            is LoadState.Loading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            is LoadState.Error -> {
                // TODO: 显示错误状态
            }
            else -> {
                if (favoritePoems.itemCount == 0) {
                    EmptyState(
                        icon = Icons.Default.FavoriteBorder,
                        title = "暂无收藏",
                        subtitle = "点击诗词详情页的收藏按钮，将喜欢的诗词添加到这里",
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
                        items(favoritePoems.itemCount) { index ->
                            val poem = favoritePoems[index]
                            if (poem != null) {
                                Box {
                                    PoemCard(
                                        poem = poem,
                                        onClick = { onPoemClick(poem.id) }
                                    )

                                    IconButton(
                                        onClick = { viewModel.removeFavorite(poem.id) },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "取消收藏",
                                            tint = PoetryColors.CinnabarRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
