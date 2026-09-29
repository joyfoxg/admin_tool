package com.admintool.deviceadmin.data.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val versionName: String,
    val versionCode: Long,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val isSystemApp: Boolean,
    val isDeviceAdmin: Boolean,
    val installTimeFormatted: String,
    val lastUpdateTimeFormatted: String,
    val sourceDir: String,
    val permissions: List<AppPermission> = emptyList(),
    val hasOverlayPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val dangerousPermissionCount: Int = 0
)
