package com.nalansitan.chinesepoetry.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.presentation.ui.components.LoadingState
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.util.wrapPoemLine
import com.nalansitan.chinesepoetry.presentation.util.PoemTtsController
import com.nalansitan.chinesepoetry.presentation.util.TtsSettingsNavigator
import com.nalansitan.chinesepoetry.presentation.viewmodel.PoemDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoemDetailScreen(
    poemId: String,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: PoemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val ttsController = remember { PoemTtsController(context) }
    var showTtsHelpDialog by remember { mutableStateOf(false) }

    // 加载诗词数据
    viewModel.loadPoem(poemId)

    LaunchedEffect(poemId) {
        ttsController.stop()
    }

    LaunchedEffect(ttsController.errorMessage) {
        val message = ttsController.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        showTtsHelpDialog = true
        ttsController.consumeError()
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsController.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
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
                    uiState.poem?.let { poem ->
                        IconButton(onClick = { ttsController.toggle(context, poem) }) {
                            Icon(
                                imageVector = if (ttsController.isSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (ttsController.isSpeaking) "停止朗读" else "朗读",
                                tint = if (ttsController.isSpeaking) PoetryColors.CinnabarRed else PoetryColors.InkBlack
                            )
                        }
                    }

                    // 收藏按钮
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "取消收藏" else "收藏",
                            tint = if (uiState.isFavorite) PoetryColors.CinnabarRed else PoetryColors.InkBlack
                        )
                    }
                    
                    // 分享按钮
                    IconButton(onClick = { viewModel.sharePoem(context) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "分享",
                            tint = PoetryColors.InkBlack
                        )
                    }
                    
                    // 复制按钮
                    IconButton(onClick = {
                        uiState.poem?.let { poem ->
                            clipboardManager.setText(AnnotatedString(poem.getFullContent()))
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = PoetryColors.InkBlack
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PoetryColors.PaperWhite,
                    navigationIconContentColor = PoetryColors.InkBlack
                )
            )
        },
        containerColor = PoetryColors.PaperWhite
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            uiState.poem == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "诗词不存在",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PoetryColors.MediumGray
                    )
                }
            }
            else -> {
                PoemDetailContent(
                    poem = uiState.poem!!,
                    isVerticalLayout = uiState.isVerticalLayout,
                    onAuthorClick = onAuthorClick,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    if (showTtsHelpDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTtsHelpDialog = false },
            title = { Text("系统朗读不可用") },
            text = {
                Text(
                    text = buildString {
                        append("当前设备的系统朗读引擎初始化失败。")
                        ttsController.currentEngineName?.let {
                            append("\n\n默认引擎：")
                            append(it)
                        }
                        append("\n\n你可以前往系统设置切换朗读引擎后再试。")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTtsHelpDialog = false
                        TtsSettingsNavigator.open(context)
                    }
                ) {
                    Text("打开设置", color = PoetryColors.CinnabarRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTtsHelpDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun PoemDetailContent(
    poem: Poem,
    isVerticalLayout: Boolean,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 词牌名（如果有）
        if (poem.rhythmic != null) {
            Text(
                text = poem.rhythmic,
                style = MaterialTheme.typography.labelLarge,
                color = PoetryColors.MediumGray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // 标题
        Text(
            text = poem.title,
            style = MaterialTheme.typography.displayMedium,
            color = PoetryColors.InkBlack,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 作者（可点击）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "[${poem.dynasty.displayName}]",
                style = MaterialTheme.typography.bodyMedium,
                color = PoetryColors.MediumGray
            )
            
            Text(
                text = poem.authorName,
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.CinnabarRed,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onAuthorClick(poem.authorName) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 诗词内容
        if (isVerticalLayout) {
            // 竖排显示
            VerticalPoemContent(poem = poem)
        } else {
            // 横排显示
            HorizontalPoemContent(poem = poem)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        HorizontalDivider(color = PoetryColors.DividerColor)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 扩展信息区域（注释、译文、赏析）
        ExtensionSection(poem = poem)
    }
}

@Composable
private fun HorizontalPoemContent(poem: Poem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        poem.content.forEach { line ->
            BalancedPoemLine(
                text = line,
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.DarkBrown,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BalancedPoemLine(
    text: String,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = constraints.maxWidth
        val wrappedLines = remember(text, style, maxWidthPx) {
            wrapPoemLine(
                text = text,
                textMeasurer = textMeasurer,
                style = style,
                maxWidthPx = maxWidthPx
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            wrappedLines.forEach { wrappedLine ->
                Text(
                    text = wrappedLine,
                    style = style,
                    color = color,
                    textAlign = TextAlign.Center,
                    lineHeight = style.lineHeight * 1.5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VerticalPoemContent(poem: Poem) {
    // 简化的竖排实现 - 从右到左排列
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        // 将诗句分组，每列显示一定数量的字
        poem.content.reversed().forEach { line ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                line.toList().forEach { char ->
                    Text(
                        text = char.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = PoetryColors.DarkBrown,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtensionSection(poem: Poem) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 注释
        if (!poem.notes.isNullOrBlank()) {
            ExtensionItem(
                title = "注释",
                content = poem.notes
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 译文
        if (!poem.translation.isNullOrBlank()) {
            ExtensionItem(
                title = "译文",
                content = poem.translation
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 赏析
        if (!poem.appreciation.isNullOrBlank()) {
            ExtensionItem(
                title = "赏析",
                content = poem.appreciation
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 评论
        if (!poem.comment.isNullOrBlank()) {
            ExtensionItem(
                title = "评论",
                content = poem.comment
            )
        }
        
        // 如果没有扩展数据，显示提示
        if (poem.notes.isNullOrBlank() && 
            poem.translation.isNullOrBlank() && 
            poem.appreciation.isNullOrBlank() && 
            poem.comment.isNullOrBlank()) {
            Text(
                text = "暂无注释、译文和赏析数据",
                style = MaterialTheme.typography.bodyMedium,
                color = PoetryColors.MediumGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun ExtensionItem(
    title: String,
    content: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = PoetryColors.CinnabarRed
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = PoetryColors.DarkBrown,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3
        )
    }
}
