package com.xianliticn.yuefu.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanningTutorialBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {}
) {
    var currentIndex by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val steps = listOf(
        TutorialStep(
            title = stringResource(R.string.shoot_vertically),
            description = stringResource(R.string.shoot_vertically_desc),
            imageResInt = null
        ),
        TutorialStep(
            title = stringResource(R.string.align_staff_left),
            description = stringResource(R.string.align_staff_left_desc),
            imageResInt = null
        )
    )

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = { onDismissRequest() }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.scanning_tutorial_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(40.dp))
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInHorizontally { fullWidth -> fullWidth } togetherWith
                            slideOutHorizontally { fullWidth -> -fullWidth }
                }
            ) { targetIndex ->
                TutorialItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = steps[targetIndex].title,
                    description = steps[targetIndex].description,
                    imageResInt = steps[targetIndex].imageResInt,
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                if (currentIndex >= steps.size - 1) {
                    onDismissRequest()
                } else {
                    currentIndex++
                }
            }) { Text(stringResource(R.string.next_step)) }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TutorialItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    imageResInt: Int?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        imageResInt?.let {
            Image(
                modifier = Modifier.height(200.dp),
                painter = painterResource(it),
                contentDescription = null,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

private data class TutorialStep(
    val title: String,
    val description: String,
    val imageResInt: Int?
)