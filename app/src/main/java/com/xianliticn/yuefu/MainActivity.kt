package com.xianliticn.yuefu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xianliticn.yuefu.pages.HomePage
import com.xianliticn.yuefu.ui.theme.YuefuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YuefuTheme {
                AppFramework(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun AppFramework(modifier: Modifier = Modifier) {
    val navItems = listOf(
        NavItem("home", R.string.home, Icons.Filled.Home)
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
                        onClick = { navController.navigate(item.route){
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        } },
                        icon = { Icon(item.icon, null) },
                        label = { Text(stringResource(item.labelStringRes)) }
                    )
                }
            }
        }) { innerPadding ->
        NavHost(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") { HomePage(hiltViewModel()) }
        }
    }
}

data class NavItem(
    val route: String,
    val labelStringRes: Int,
    val icon: ImageVector
)