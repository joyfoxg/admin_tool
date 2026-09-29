package com.admintool.deviceadmin.data.shell

import com.admintool.deviceadmin.data.model.PrivilegeMode

interface ShellExecutor {
    val mode: PrivilegeMode
    fun isAvailable(): Boolean
    suspend fun execute(command: String): ShellResult
    suspend fun executeBatch(commands: List<String>): List<ShellResult>
}

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}
