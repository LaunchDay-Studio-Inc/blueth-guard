package com.blueth.guard.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val versionName: String,
    val installSource: String,
    val permissions: List<String>,
    val cacheSize: Long,
    val dataSize: Long,
    val apkSize: Long,
    val isSystem: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdkVersion: Int,
    val riskScore: Int = 0
)
