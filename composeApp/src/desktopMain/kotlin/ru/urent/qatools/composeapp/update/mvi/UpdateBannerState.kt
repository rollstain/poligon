package ru.urent.qatools.composeapp.update.mvi

import ru.urent.qatools.base_update.entities.UpdateStatus

data class UpdateBannerState(
    val status: UpdateStatus,
    val isHidden: Boolean,
)
