package com.xianliticn.yuefu

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xianliticn.yuefu.pages.sheetoverview.SheetOverviewPage
import com.xianliticn.yuefu.pages.sheetplay.SheetPlayPage
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SheetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YuefuTheme {
                SheetActivityFramework(
                    modifier = Modifier.fillMaxSize(),
                    sheetId = intent.getIntExtra("sheetId", 0)
                )
            }
        }
    }

    @Composable
    fun SheetActivityFramework(
        modifier: Modifier = Modifier,
        sheetId: Int
    ) {
        val navItems = listOf(
            NavItem("overview", R.string.overview, Icons.Default.Info),
            NavItem("play", R.string.play, Icons.Default.Piano)
        )
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val isPlayRoute = currentRoute == "play"

        DisposableEffect(isPlayRoute) {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            if (isPlayRoute) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }

        Scaffold(
            modifier = modifier,
            bottomBar = {
                if (!isPlayRoute) {
                    NavigationBar {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, null) },
                                label = { Text(stringResource(item.labelStringRes)) }
                            )
                        }
                    }
                }
            }) { innerPadding ->
            NavHost(
                modifier = Modifier
                    .padding(innerPadding),
                navController = navController,
                startDestination = "overview",
                enterTransition = {
                    fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + slideInVertically(
                        initialOffsetY = { it / 8 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                },
                popEnterTransition = {
                    fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ) + slideInVertically(
                        initialOffsetY = { -it / 8 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                }
            ) {
                composable("overview") {
                    SheetOverviewPage(
                        hiltViewModel(),
                        sheetId,
                        onBackPress = { finish() }
                    )
                }
                composable("play") { SheetPlayPage(hiltViewModel(), sheetId) }
            }
        }
    }
}
