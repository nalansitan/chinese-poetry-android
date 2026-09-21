package com.nalansitan.chinesepoetry.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.nalansitan.chinesepoetry.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nalansitan.chinesepoetry.presentation.navigation.Screen
import com.nalansitan.chinesepoetry.presentation.ui.screens.ExploreScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.FavoritesScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.HomeScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.HistoryScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.InitialDownloadScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.PoemDetailScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.PoemListScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.ProfileScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.SearchScreen
import com.nalansitan.chinesepoetry.presentation.ui.screens.SettingsScreen
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryColors
import com.nalansitan.chinesepoetry.presentation.ui.theme.PoetryTheme
import com.nalansitan.chinesepoetry.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Poetry)
        super.onCreate(savedInstanceState)
        setContent {
            PoetryTheme {
                PoetryAppEntry()
            }
        }
    }
}

@Composable
fun PoetryAppEntry(
    viewModel: MainViewModel = hiltViewModel()
) {
    var isDatabaseReady by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(true) }
    val hasPendingUpdate by viewModel.hasPendingUpdate.collectAsState()

    LaunchedEffect(Unit) {
        isDatabaseReady = viewModel.isDatabaseExists()
        isChecking = false
        if (isDatabaseReady) {
            viewModel.checkForUpdateSilently()
        }
    }

    when {
        isChecking -> {
            StartupLoadingScreen()
        }
        !isDatabaseReady -> {
            // 首次启动，需要下载数据
            InitialDownloadScreen(
                onDownloadComplete = {
                    isDatabaseReady = true
                }
            )
        }
        else -> {
            // 数据库已就绪，进入主应用
            PoetryApp(hasPendingUpdate = hasPendingUpdate)
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFA61E22),
                        Color(0xFFC74335),
                        Color(0xFFF7F1E7)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PoetryColors.PaperWhite,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "古诗词",
                style = MaterialTheme.typography.headlineMedium,
                color = PoetryColors.PaperWhite,
                modifier = Modifier.padding(top = 20.dp)
            )
            Text(
                text = "正在准备诗词库",
                style = MaterialTheme.typography.bodyLarge,
                color = PoetryColors.PaperWhite,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "首次进入或数据库变更时会稍等片刻",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFF8F1),
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Text(
            text = "作者：纳兰斯坦、爱因容若",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFFF8F1),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun PoetryApp(hasPendingUpdate: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomNavRoutes = Screen.bottomNavItems.map { it.route }.toSet()
    val shouldShowBottomBar = currentDestination
        ?.hierarchy
        ?.mapNotNull { it.route }
        ?.any(bottomNavRoutes::contains) == true
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(
                    navController = navController,
                    hasPendingUpdate = hasPendingUpdate
                )
            }
        },
        containerColor = PoetryColors.PaperWhite
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    },
                    onCategoryClick = { type ->
                        navController.navigate(Screen.PoemList.createRoute(type = type))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onAuthorsClick = {
                        navController.navigate(Screen.Explore.route)
                    },
                    onHistoryClick = {
                        navController.navigate(Screen.History.route)
                    }
                )
            }
            
            composable(Screen.Explore.route) {
                ExploreScreen(
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    },
                    onAuthorClick = { authorName ->
                        navController.navigate(Screen.PoemList.createRoute(author = authorName))
                    }
                )
            }
            
            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    }
                )
            }
            
            composable(Screen.Profile.route) {
                ProfileScreen(
                    hasPendingUpdate = hasPendingUpdate,
                    onHistoryClick = {
                        navController.navigate(Screen.History.route)
                    },
                    onFavoritesClick = {
                        navController.navigate(Screen.Favorites.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }
            
            // 搜索页
            composable(Screen.Search.route) {
                SearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    }
                )
            }
            
            // 设置页
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onBackClick = { navController.popBackStack() },
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    }
                )
            }
            
            // 诗词详情页
            composable(
                route = Screen.PoemDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("poemId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val poemId = backStackEntry.arguments?.getString("poemId") ?: return@composable
                PoemDetailScreen(
                    poemId = poemId,
                    onBackClick = { navController.popBackStack() },
                    onAuthorClick = { authorName ->
                        navController.navigate(Screen.PoemList.createRoute(author = authorName))
                    }
                )
            }
            
            // 诗词列表页
            composable(
                route = Screen.PoemList.route,
                arguments = listOf(
                    androidx.navigation.navArgument("type") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument("dynasty") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument("author") {
                        type = androidx.navigation.NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type")
                val dynasty = backStackEntry.arguments?.getString("dynasty")
                val author = backStackEntry.arguments?.getString("author")
                
                val title = when {
                    type != null -> {
                        when (type) {
                            "tang_shi" -> "唐诗"
                            "song_shi" -> "宋诗"
                            "song_ci" -> "宋词"
                            "shi_jing" -> "诗经"
                            "lun_yu" -> "论语"
                            else -> "诗词列表"
                        }
                    }
                    dynasty != null -> "${dynasty}诗"
                    author != null -> "$author 的诗"
                    else -> "诗词列表"
                }
                
                PoemListScreen(
                    title = title,
                    onBackClick = { navController.popBackStack() },
                    onPoemClick = { poemId ->
                        navController.navigate(Screen.PoemDetail.createRoute(poemId))
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    hasPendingUpdate: Boolean
) {
    NavigationBar(
        containerColor = PoetryColors.PaperWhite,
        contentColor = PoetryColors.InkBlack
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        Screen.bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            val showUpdateBadge = screen == Screen.Profile && hasPendingUpdate
            
            NavigationBarItem(
                icon = {
                    if (showUpdateBadge) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = PoetryColors.CinnabarRed)
                            }
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title
                        )
                    }
                },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PoetryColors.CinnabarRed,
                    selectedTextColor = PoetryColors.CinnabarRed,
                    unselectedIconColor = PoetryColors.MediumGray,
                    unselectedTextColor = PoetryColors.MediumGray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
