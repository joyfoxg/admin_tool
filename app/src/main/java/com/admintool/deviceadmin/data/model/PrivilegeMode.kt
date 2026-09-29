package com.admintool.deviceadmin.data.model

enum class PrivilegeMode(val displayName: String, val badgeColorHex: Long) {
    STANDARD("Standard (기본 모드)", 0xFF64748B),
    SHIZUKU("Shizuku (무선 ADB 모드)", 0xFF0284C7)
}
