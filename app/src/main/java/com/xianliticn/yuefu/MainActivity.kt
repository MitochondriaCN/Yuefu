package com.xianliticn.yuefu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.xianliticn.yuefu.pages.HomePage
import com.xianliticn.yuefu.pages.ScanStudioPage
import com.xianliticn.yuefu.pages.SheetPage
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
            NavItem("home", R.string.home, Icons.Filled.Home),
            NavItem("sheet", R.string.sheet, Icons.Filled.MusicNote)
        )
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = navBackStackEntry?.destination?.route == item.route,
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
            }) { innerPadding ->
            NavHost(
                modifier = Modifier
                    .padding(innerPadding),
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomePage(
                        hiltViewModel(),
                        onImageSelected = {
                            // 将Uri对象编码成字符串，以安全地作为URL的一部分
                            val encodedUri = Uri.encode(it.toString())
                            navController.navigate("studio/$encodedUri")
                        })
                }

                composable("sheet") { SheetPage(hiltViewModel()) }

                composable(
                    route = "studio/{imageUri}",
                    arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                )
                { backStackEntry ->
                    val imageUriString = backStackEntry.arguments?.getString("imageUri")
                    val imageUri = imageUriString?.let { Uri.decode(it).toUri() }
                    if (imageUri == null) {
                        navController.popBackStack()
                        return@composable
                    }
                    ScanStudioPage(hiltViewModel(), imageUri)
                }
            }
        }
    }
}
