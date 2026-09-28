package com.example.texlabinventory.data.utils

import android.content.Context
import android.os.Build
import com.example.texlabinventory.data.model.UpdateInfo
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object RemoteConfigHelper {

    fun checkAppUpdate(context: Context, onResult: (UpdateInfo) -> Unit) {
        val remoteConfig = Firebase.remoteConfig

        // Opsional: interval fetch (diubah jadi 0 untuk testing agar update langsung terasa)
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 1. Ambil versionCode lokal dari aplikasi yang terinstall saat ini
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        // 2. Fetch data dari Firebase Remote Config
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val latestVersionCode = remoteConfig.getLong("latest_version_code")
                val latestVersionName = remoteConfig.getString("latest_version_name")
                val updateUrl = remoteConfig.getString("update_url")
                val isForceUpdate = remoteConfig.getBoolean("is_force_update")

                // 3. Jika versi di server > versi lokal di HP
                if (latestVersionCode > currentVersionCode) {
                    onResult(
                        UpdateInfo(
                            isUpdateAvailable = true,
                            latestVersionName = latestVersionName,
                            updateUrl = updateUrl,
                            isForceUpdate = isForceUpdate
                        )
                    )
                } else {
                    onResult(UpdateInfo(isUpdateAvailable = false))
                }
            } else {
                onResult(UpdateInfo(isUpdateAvailable = false))
            }
        }
    }
}