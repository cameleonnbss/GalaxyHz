package com.example.galaxyhz.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Runs shell commands as root through Magisk's `su`.
 *
 * Commands are piped into a single interactive `su` session instead of being
 * passed through `su -c`, so multi-line batches and sysfs writes are not
 * mangled by an extra level of shell quoting. All invocations are serialized
 * with a mutex: Magisk denies concurrent requests from the same uid.
 */
object RootHelper {

    data class CommandResult(
        val isSuccess: Boolean,
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        /** Merged command output, useful for logs. */
        val output: String
            get() = listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n")
    }

    private val suMutex = Mutex()

    @Volatile
    private var cachedRoot: Boolean? = null

    /** True once a root `id` check succeeded (result is cached). */
    suspend fun isRootAvailable(): Boolean {
        cachedRoot?.let { return it }
        val result = runCommand("id")
        val hasRoot = result.isSuccess && result.stdout.contains("uid=0")
        if (hasRoot) cachedRoot = true
        return hasRoot
    }

    /**
     * Runs one or more shell commands (one per line) in a single root shell.
     * The returned exit code is the exit code of the last command.
     */
    suspend fun runCommand(command: String, timeoutMs: Long = 12_000): CommandResult =
        withContext(Dispatchers.IO) {
            runCatching {
                suMutex.withLock {
                    val process = ProcessBuilder("su")
                        .redirectErrorStream(false)
                        .start()

                    val stdin = DataOutputStream(process.outputStream)
                    stdin.writeBytes(command.trimEnd('\n'))
                    stdin.writeBytes("\nexit\n")
                    stdin.flush()
                    stdin.close()

                    val stdout = StringBuilder()
                    val stderr = StringBuilder()

                    val stdoutThread = Thread {
                        runCatching { drain(BufferedReader(InputStreamReader(process.inputStream)), stdout) }
                    }
                    val stderrThread = Thread {
                        runCatching { drain(BufferedReader(InputStreamReader(process.errorStream)), stderr) }
                    }
                    stdoutThread.start()
                    stderrThread.start()

                    val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                    if (!finished) process.destroyForcibly()
                    stdoutThread.join(2_000)
                    stderrThread.join(2_000)

                    val exitCode = if (finished) process.exitValue() else -1
                    CommandResult(
                        isSuccess = finished && exitCode == 0,
                        exitCode = exitCode,
                        stdout = stdout.toString().trim(),
                        stderr = stderr.toString().trim()
                    )
                }
            }.getOrElse { e ->
                CommandResult(
                    isSuccess = false,
                    exitCode = -1,
                    stdout = "",
                    stderr = when (e) {
                        is IOException -> "su binary not available (is Magisk installed?)"
                        else -> e.message ?: "Exception running command"
                    }
                )
            }
        }

    /** Runs a list of commands as one root shell batch. */
    suspend fun runCommands(commands: List<String>, timeoutMs: Long = 12_000): CommandResult =
        runCommand(commands.joinToString("\n"), timeoutMs)

    /**
     * Runs commands with retries: when other apps hammer Magisk's su daemon,
     * requests can transiently fail (crashed su worker). One retry after a
     * short backoff recovers most of those without the caller noticing.
     */
    suspend fun runCommandsRetried(
        commands: List<String>,
        timeoutMs: Long = 12_000,
        attempts: Int = 3
    ): CommandResult {
        var last = runCommands(commands, timeoutMs)
        var delayMs = 400L
        var attempt = 1
        while (!last.isSuccess && attempt < attempts) {
            kotlinx.coroutines.delay(delayMs)
            delayMs *= 2
            last = runCommands(commands, timeoutMs)
            attempt++
        }
        return last
    }

    private fun drain(reader: BufferedReader, into: StringBuilder) {
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            into.append(line).append('\n')
        }
    }
}
