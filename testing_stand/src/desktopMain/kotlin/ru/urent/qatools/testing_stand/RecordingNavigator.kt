package ru.urent.qatools.testing_stand

import ru.urent.qatools.navigation.Navigator
import ru.urent.qatools.navigation.Screens

class RecordingNavigator : Navigator {
    val pushedScreens = mutableListOf<Screens>()
    var popCount = 0
        private set

    override fun push(screen: Screens) {
        pushedScreens.add(screen)
    }

    override fun replaceAll(screen: Screens) = Unit

    override fun pop() {
        popCount += 1
    }
}
