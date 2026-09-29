package com.admintool.deviceadmin.data.model

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class DeviceAdminApp(
    val packageName: String,
    val className: String,
    val componentName: ComponentName,
    val appName: String,
    val icon: Drawable?,
    val isActiveAdmin: Boolean,
    val isSystemApp: Boolean,
    val isCriticalSystemAdmin: Boolean,
    val isGreyedOutOrLocked: Boolean = false, // 설정에서 비활성화(회색/잠김)되어 해제 불가한 앱 여부
    val isDeviceOrProfileOwner: Boolean = false,
    val requestedPolicies: List<AdminPolicyInfo> = emptyList(),
    val permissions: List<AppPermission> = emptyList(),
    val hasOverlayPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false
)

data class AdminPolicyInfo(
    val policyTag: String,
    val description: String
)
