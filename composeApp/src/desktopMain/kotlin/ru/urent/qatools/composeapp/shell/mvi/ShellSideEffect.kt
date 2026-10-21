package ru.urent.qatools.composeapp.shell.mvi

import ru.urent.qatools.navigation.Screens

sealed interface ShellSideEffect {
    data class OpenSection(val screen: Screens) : ShellSideEffect
}
