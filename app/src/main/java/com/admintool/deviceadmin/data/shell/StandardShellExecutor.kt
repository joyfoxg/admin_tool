package com.admintool.deviceadmin.data.shell

import com.admintool.deviceadmin.data.model.PrivilegeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandardShellExecutor @Inject constructor() : ShellExecutor {
    override val mode: PrivilegeMode = PrivilegeMode.STANDARD

    override fun isAvailable(): Boolean = true

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(command)
            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Execution failed")
        }
    }

    override suspend fun executeBatch(commands: List<String>): List<ShellResult> {
        return commands.map { execute(it) }
    }
}
