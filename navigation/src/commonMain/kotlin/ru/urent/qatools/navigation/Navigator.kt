package ru.urent.qatools.navigation

import androidx.compose.runtime.staticCompositionLocalOf

interface Navigator {
    fun push(screen: Screens)
    fun replaceAll(screen: Screens)
    fun pop()
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator не передан в композицию") }
