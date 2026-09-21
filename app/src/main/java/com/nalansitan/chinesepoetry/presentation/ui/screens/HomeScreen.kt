package com.nalansitan.chinesepoetry.presentation.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nalansitan.chinesepoetry.data.local.entity.PoemType
import com.nalansitan.chinesepoetry.presentation.ui.components.CategoryItem
import com.nalansitan.chinesepoetry.presentation.ui.components.DailyPoemCard
import com.nalansitan.chinesepoetry.presentation.ui.components.LoadingState
import com.nalansitan.chinesepoetry.presentation.ui.components.PoemCard
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.util.LunarDateFormatter
import com.nalansitan.chinesepoetry.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPoemClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onAuthorsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "古诗词",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = LunarDateFormatter.formatToday(),
                    style = MaterialTheme.typography.labelLarge,
                    color = PoetryColors.CinnabarRed,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // 每日一诗
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "每日一诗",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PoetryColors.InkBlack
                    )

                    IconButton(
                        onClick = viewModel::refreshDailyPoem,
                        enabled = !uiState.isRefreshingDailyPoem
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "换一首",
                            tint = if (uiState.isRefreshingDailyPoem) {
                                PoetryColors.MediumGray
                            } else {
                                PoetryColors.CinnabarRed
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    uiState.isLoading || uiState.isRefreshingDailyPoem -> {
                        LoadingState(modifier = Modifier.height(200.dp))
                    }
                    uiState.dailyPoem != null -> {
                        DailyPoemCard(
                            poem = uiState.dailyPoem!!,
                            onClick = { onPoemClick(uiState.dailyPoem!!.id) }
                        )
                    }
                    else -> {
                        Text(
                            text = uiState.error ?: "暂时没有可推荐的诗词",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PoetryColors.MediumGray,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
            
            // 分类入口
            item {
                Text(
                    text = "分类浏览",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PoetryColors.InkBlack
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        CategoryItem(
                            name = "唐诗",
                            icon = Icons.Default.MenuBook,
                            onClick = { onCategoryClick(PoemType.TANG_SHI.value) }
                        )
                    }
                    item {
                        CategoryItem(
                            name = "宋诗",
                            icon = Icons.Default.Book,
                            onClick = { onCategoryClick(PoemType.SONG_SHI.value) }
                        )
                    }
                    item {
                        CategoryItem(
                            name = "宋词",
                            icon = Icons.Default.Description,
                            onClick = { onCategoryClick(PoemType.SONG_CI.value) }
                        )
                    }
                    item {
                        CategoryItem(
                            name = "诗经",
                            icon = Icons.Default.MenuBook,
                            onClick = { onCategoryClick(PoemType.SHI_JING.value) }
                        )
                    }
                    item {
                        CategoryItem(
                            name = "诗人",
                            icon = Icons.Default.Person,
                            onClick = onAuthorsClick
                        )
                    }
                }
            }
            
            // 最近浏览
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "最近浏览",
                        style = MaterialTheme.typography.headlineSmall,
                        color = PoetryColors.InkBlack
                    )
                    
                    if (uiState.recentPoems.isNotEmpty()) {
                        Text(
                            text = "查看全部",
                            style = MaterialTheme.typography.labelLarge,
                            color = PoetryColors.CinnabarRed,
                            modifier = Modifier.clickable(onClick = onHistoryClick)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (uiState.recentPoems.isEmpty()) {
                    Text(
                        text = "暂无浏览记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PoetryColors.MediumGray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.recentPoems.take(5).forEach { poem ->
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
