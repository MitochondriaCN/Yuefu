package com.xianliticn.yuefu.pages

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomePage() {
    val viewModel: HomePageViewModel = hiltViewModel()

    Text("Hello, world!")
}