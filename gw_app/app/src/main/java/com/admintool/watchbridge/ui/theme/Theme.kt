package com.admintool.watchbridge.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun WatchBridgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WatchColorScheme,
        content = content
    )
}
