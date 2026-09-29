package com.admintool.deviceadmin.data.model

data class AppPermission(
    val permission: String,
    val label: String,
    val description: String,
    val isGranted: Boolean,
    val isRuntimePermission: Boolean,
    val isSpecialAccess: Boolean,
    val appOpName: String? = null
)
