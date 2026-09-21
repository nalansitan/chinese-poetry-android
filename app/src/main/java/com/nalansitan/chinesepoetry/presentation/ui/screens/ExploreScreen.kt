package com.nalansitan.chinesepoetry.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.presentation.ui.components.AuthorItem
import com.nalansitan.chinesepoetry.presentation.ui.components.DynastyTag
import com.nalansitan.chinesepoetry.presentation.ui.components.EmptyState
import com.nalansitan.chinesepoetry.presentation.ui.components.LoadingState
import com.nalansitan.chinesepoetry.presentation.ui.components.PoemCard
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.viewmodel.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onPoemClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val poems = viewModel.searchResults.collectAsLazyPagingItems()
    val isSearchMode = uiState.searchQuery.isNotBlank()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "发现",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 搜索框
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("搜索诗词、作者...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PoetryColors.MediumGray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PoetryColors.LightGray,
                        unfocusedContainerColor = PoetryColors.LightGray,
                        focusedBorderColor = PoetryColors.CinnabarRed,
                        unfocusedBorderColor = PoetryColors.DividerColor
                    ),
                    singleLine = true
                )
            }
            
            // 朝代筛选
            item {
                Text(
                    text = "朝代",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PoetryColors.InkBlack
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Dynasty.entries) { dynasty ->
                        DynastyTag(
                            name = dynasty.displayName,
                            onClick = { viewModel.onDynastySelected(dynasty) },
                            isSelected = uiState.selectedDynasty == dynasty
                        )
                    }
                }
            }
            
            // 热门诗人
            item {
                Text(
                    text = "热门诗人",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PoetryColors.InkBlack
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                when {
                    uiState.isLoadingAuthors -> LoadingState(modifier = Modifier.height(100.dp))
                    uiState.topAuthors.isEmpty() -> {
                        Text(
                            text = "暂无数据",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PoetryColors.MediumGray
                        )
                    }
                    else -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(uiState.topAuthors) { author ->
                                AuthorItem(
                                    author = author,
                                    onClick = { onAuthorClick(author.name) }
                                )
                            }
                        }
                    }
                }
            }
            
            // 搜索结果或推荐
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when {
                            uiState.searchQuery.isNotEmpty() -> "搜索结果"
                            uiState.selectedDynasty != null -> "${uiState.selectedDynasty!!.displayName}诗词"
                            else -> "推荐诗词"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = PoetryColors.InkBlack
                    )

                    if (!isSearchMode) {
                        IconButton(onClick = viewModel::refreshRecommendations) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "换一批",
                                tint = PoetryColors.CinnabarRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!isSearchMode) {
                when {
                    uiState.isLoadingRecommendations -> {
                        item {
                            LoadingState(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    uiState.recommendationsError != null -> {
                        item {
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "加载失败",
                                subtitle = uiState.recommendationsError,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    uiState.recommendedPoems.isEmpty() -> {
                        item {
                            EmptyState(
                                icon = Icons.Default.Book,
                                title = "暂无推荐诗词",
                                subtitle = if (uiState.selectedDynasty != null) "当前朝代下暂时没有内容" else "换一批试试",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        items(uiState.recommendedPoems) { poem ->
                            PoemCard(
                                poem = poem,
                                onClick = { onPoemClick(poem.id) }
                            )
                        }
                    }
                }
            } else {
                when (poems.loadState.refresh) {
                    is LoadState.Loading -> {
                        item {
                            LoadingState(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            EmptyState(
                                icon = Icons.Default.Search,
                                title = "加载失败",
                                subtitle = (poems.loadState.refresh as LoadState.Error).error.localizedMessage,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        if (poems.itemCount == 0) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Book,
                                    title = if (uiState.searchQuery.isNotEmpty()) "暂无搜索结果" else "暂无推荐诗词",
                                    subtitle = if (uiState.searchQuery.isNotEmpty()) "换个关键词试试" else "当前条件下暂时没有内容",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            items(poems.itemCount) { index ->
                                val poem = poems[index]
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
    }
}
