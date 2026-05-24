package com.dimonoso.crosplatformfilesender

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        icon = painterResource("icons/app-icon.png"),
        onCloseRequest = ::exitApplication,
        state = rememberWindowState(width = 1180.dp, height = 760.dp),
        title = "CrosplatformFileSender",
    ) {
        App()
    }
}
