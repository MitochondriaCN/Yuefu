package com.xianliticn.yuefu.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xianliticn.yuefu.R
import com.xianliticn.yuefu.ui.theme.Amber800
import com.xianliticn.yuefu.ui.theme.Blue800
import com.xianliticn.yuefu.ui.theme.Clouds

@Composable
fun HomePage(viewModel: HomePageViewModel) {
    HomePageContent()
}

@Composable
fun HomePageContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ButtonCard(
                icon = Icons.Default.Camera,
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.shot_sheet),
                bgColor = Blue800
            )
            ButtonCard(
                icon = Icons.Default.AttachFile,
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.import_file),
                bgColor = Amber800
            )
        }
    }
}

@Composable
private fun ButtonCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit = {}
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = bgColor,
            contentColor = Clouds
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                modifier = Modifier.size(40.dp),
                contentDescription = null
            )
            Spacer(Modifier.height(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Preview(showBackground = true, locale = "en")
@Composable
fun HomePagePreview() {
    HomePageContent()
}