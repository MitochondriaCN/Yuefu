package com.xianliticn.yuefu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xianliticn.yuefu.pages.SheetOverviewPage
import com.xianliticn.yuefu.pages.SheetPlayPage
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

        Scaffold(
            modifier = modifier,
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
                startDestination = "overview"
            ) {
                composable("overview") { SheetOverviewPage(hiltViewModel(), sheetId) }
                composable("play") { SheetPlayPage(hiltViewModel(), sheetId) }
            }
        }
    }
}

