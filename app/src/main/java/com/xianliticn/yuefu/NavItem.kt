package com.xianliticn.yuefu

import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val route: String,
    val labelStringRes: Int,
    val icon: ImageVector
)