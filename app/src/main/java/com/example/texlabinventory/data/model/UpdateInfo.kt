package com.example.texlabinventory.data.model

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String = "",
    val updateUrl: String = "",
    val isForceUpdate: Boolean = false
)