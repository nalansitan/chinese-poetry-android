package com.nalansitan.chinesepoetry.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 导航路由定义
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    // 底部导航
    data object Home : Screen("home", "首页", Icons.Default.Home)
    data object Explore : Screen("explore", "发现", Icons.Default.Explore)
    data object Favorites : Screen("favorites", "收藏", Icons.Default.Favorite)
    data object Profile : Screen("profile", "我的", Icons.Default.Person)
    
    // 其他页面
    data object PoemDetail : Screen("poem/{poemId}", "诗词详情", Icons.Default.Book) {
        fun createRoute(poemId: String) = "poem/$poemId"
    }
    
    data object AuthorDetail : Screen("author/{authorId}", "作者详情", Icons.Default.Person) {
        fun createRoute(authorId: String) = "author/$authorId"
    }
    
    data object PoemList : Screen("poems?type={type}&dynasty={dynasty}&author={author}", "诗词列表", Icons.Default.Book) {
        fun createRoute(type: String? = null, dynasty: String? = null, author: String? = null): String {
            val params = mutableListOf<String>()
            type?.let { params.add("type=$it") }
            dynasty?.let { params.add("dynasty=$it") }
            author?.let { params.add("author=$it") }
            return if (params.isEmpty()) "poems" else "poems?${params.joinToString("&")}"
        }
    }
    
    data object Search : Screen("search", "搜索", Icons.Default.Explore)
    data object Settings : Screen("settings", "设置", Icons.Default.Person)
    data object History : Screen("history", "阅读历史", Icons.Default.Book)
    
    companion object {
        val bottomNavItems = listOf(Home, Explore, Favorites, Profile)
    }
}
