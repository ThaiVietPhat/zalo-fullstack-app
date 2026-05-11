package com.example.zalo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BlueZalo = Color(0xFF0068FF)

private val LightColorScheme = lightColorScheme(
    primary = BlueZalo,
    secondary = Color.Gray,
    tertiary = Color.LightGray
)

@Composable
fun ZaloCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
