package com.xianliticn.yuefu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Blender
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xianliticn.yuefu.modules.SettingsManager
import com.xianliticn.yuefu.pages.about.AboutPage
import com.xianliticn.yuefu.pages.composition.CompositionPage
import com.xianliticn.yuefu.pages.home.HomePage
import com.xianliticn.yuefu.pages.scanstudio.ScanStudioPage
import com.xianliticn.yuefu.pages.settings.SettingsPage
import com.xianliticn.yuefu.pages.sheet.SheetPage
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YuefuTheme {
                MainActivityFramework(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    fun MainActivityFramework(modifier: Modifier = Modifier) {
        val navItems = listOf(
            NavItem("home", R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
            NavItem("sheet", R.string.sheet, Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
            NavItem("composition", R.string.composition, Icons.Filled.Blender, Icons.Outlined.Blender),
            NavItem("settings", R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
        )
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        fun bottomBarNavigate(targetRoute: String) {
            navController.navigate(targetRoute) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    tonalElevation = 3.dp
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { bottomBarNavigate(item.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(item.labelStringRes)) }
                        )
                    }
                }
            }) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = navController,
                startDestination = "home",
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 4 },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 6 },
                        animationSpec = tween(200)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 4 },
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth / 6 },
                        animationSpec = tween(200)
                    )
                }
            ) {
                composable("home") {
                    HomePage(
                        hiltViewModel(),
                        onImageSelected = {
                            val encodedUri = Uri.encode(it.toString())
                            navController.navigate("studio/$encodedUri")
                        })
                }

                composable("sheet") { SheetPage(hiltViewModel()) }

                composable(
                    route = "studio/{imageUri}",
                    arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val imageUriString = backStackEntry.arguments?.getString("imageUri")
                    val imageUri = imageUriString?.let { Uri.decode(it).toUri() }
                    if (imageUri == null) {
                        navController.popBackStack()
                        return@composable
                    }
                    ScanStudioPage(
                        viewModel = hiltViewModel(),
                        imageUri = imageUri,
                        onFinished = {
                            navController.popBackStack()
                            bottomBarNavigate("sheet")
                        }
                    )
                }

                composable("composition") { CompositionPage(hiltViewModel()) }

                composable("about") { AboutPage(hiltViewModel()) }

                composable("settings") {
                    SettingsPage(
                        viewModel = hiltViewModel(),
                        onAboutClick = { navController.navigate("about") }
                    )
                }
            }
        }
    }
}
