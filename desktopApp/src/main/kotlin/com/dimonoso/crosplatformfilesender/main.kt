package com.dimonoso.crosplatformfilesender

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
        title = "CrosplatformFileSender",
    ) {
        App()
    }
}
