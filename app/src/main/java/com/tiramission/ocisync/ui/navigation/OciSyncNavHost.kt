package com.tiramission.ocisync.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tiramission.ocisync.R
import com.tiramission.ocisync.ui.history.HistoryScreen
import com.tiramission.ocisync.ui.home.HomeScreen
import com.tiramission.ocisync.ui.list.ListScreen
import com.tiramission.ocisync.ui.pull.PullScreen
import com.tiramission.ocisync.ui.push.PushScreen
import com.tiramission.ocisync.ui.settings.SettingsScreen

/** 路由定义。底部三 Tab(首页/仓库/历史)贯穿全部页面,子页保留返回键(ui-design/ 设计稿)。 */
object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PUSH = "push?ref={ref}"
    const val PUSH_ARG_REF = "ref"
    const val PULL = "pull?ref={ref}"
    const val PULL_ARG_REF = "ref"
    const val SHORTCUT = "shortcut/{name}/{repo}"
    const val SHORTCUT_ARG_NAME = "name"
    const val SHORTCUT_ARG_REPO = "repo"

    fun push(ref: String = ""): String = "push?ref=${java.net.URLEncoder.encode(ref, "UTF-8")}"
    fun pull(ref: String): String = "pull?ref=${java.net.URLEncoder.encode(ref, "UTF-8")}"
    fun shortcut(name: String, repo: String): String =
        "shortcut/${java.net.URLEncoder.encode(name, "UTF-8")}/${java.net.URLEncoder.encode(repo, "UTF-8")}"
}

private data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, R.string.tab_home, Icons.Filled.Home),
    BottomTab(Routes.BROWSE, R.string.tab_browse, Icons.Filled.Inventory),
    BottomTab(Routes.HISTORY, R.string.tab_history, Icons.Filled.History),
)

/** 应用根:底部导航(64dp,设计稿 首页.html nav)+ NavHost。 */
@Composable
fun OciSyncAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // 不消费系统栏 inset,让各页面根元素自行处理
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                tabs = bottomTabs,
                currentRoute = currentRoute,
                onTabClick = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenPush = { navController.navigate(Routes.push()) },
                    onOpenPull = { navController.navigate(Routes.pull("")) },
                    onOpenShortcut = { name, repo -> navController.navigate(Routes.shortcut(name, repo)) },
                )
            }
            composable(Routes.BROWSE) {
                ListScreen(
                    initialRef = null,
                    onPullArtifact = { ref -> navController.navigate(Routes.pull(ref)) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(
                route = Routes.PUSH,
                arguments = listOf(navArgument(Routes.PUSH_ARG_REF) {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val initialRef = entry.arguments?.getString(Routes.PUSH_ARG_REF).orEmpty()
                PushScreen(
                    initialRef = initialRef,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PULL,
                arguments = listOf(navArgument(Routes.PULL_ARG_REF) {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val initialRef = entry.arguments?.getString(Routes.PULL_ARG_REF).orEmpty()
                PullScreen(
                    initialRef = initialRef,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SHORTCUT,
                arguments = listOf(
                    navArgument(Routes.SHORTCUT_ARG_NAME) { type = NavType.StringType },
                    navArgument(Routes.SHORTCUT_ARG_REPO) { type = NavType.StringType },
                ),
            ) { entry ->
                val name = entry.arguments?.getString(Routes.SHORTCUT_ARG_NAME).orEmpty()
                val repo = entry.arguments?.getString(Routes.SHORTCUT_ARG_REPO).orEmpty()
                ShortcutDetailScreen(
                    name = name,
                    repo = repo,
                    onBack = { navController.popBackStack() },
                    onPullArtifact = { ref -> navController.navigate(Routes.pull(ref)) },
                    onPushNew = { navController.navigate(Routes.push(repo)) },
                    onEditShortcut = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** 底部导航(设计稿 首页.html nav):64dp 高,选中=主色 + 中等字重,未选=次要色。 */
@Composable
private fun BottomNavBar(
    tabs: List<BottomTab>,
    currentRoute: String?,
    onTabClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RectangleShape,
            ),
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            val contentColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(
                        onClick = { onTabClick(tab.route) },
                        indication = ripple(),
                        interactionSource = remember { MutableInteractionSource() },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
