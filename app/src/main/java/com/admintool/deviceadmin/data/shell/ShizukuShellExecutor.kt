package com.admintool.deviceadmin.data.shell

import android.content.pm.PackageManager
import com.admintool.deviceadmin.data.model.PrivilegeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShizukuShellExecutor @Inject constructor() : ShellExecutor {
    override val mode: PrivilegeMode = PrivilegeMode.SHIZUKU

    override fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun execute(command: String): ShellResult = withContext(Dispatchers.IO) {
        if (!isAvailable()) {
            return@withContext ShellResult(-1, "", "Shizuku is not running or permission is denied")
        }

        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            ShellResult(exitCode, stdout.trim(), stderr.trim())
        } catch (e: Throwable) {
            ShellResult(-1, "", e.message ?: "Shizuku command execution failed")
        }
    }

    override suspend fun executeBatch(commands: List<String>): List<ShellResult> {
        return commands.map { execute(it) }
    }
}
