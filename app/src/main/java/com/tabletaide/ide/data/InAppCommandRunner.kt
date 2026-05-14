package com.tabletaide.ide.data

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val SHELL_PATH = "/system/bin/sh"
private const val MAX_TERMINAL_CHARS = 24_000

@Singleton
class InAppCommandRunner @Inject constructor(
    private val workspaceExecutionResolver: WorkspaceExecutionResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val executionMutex = Mutex()

    private val _uiState = MutableStateFlow(CommandRunnerUiState())
    val uiState: StateFlow<CommandRunnerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var executionTarget: WorkspaceExecutionReady? = null
    private var activeProcess: Process? = null
    private var runJob: Job? = null
    private var cancelRequested = false
    private var cancelMessage = "Command cancelled."

    fun bindWorkspace(treeUri: Uri?) {
        val resolution = workspaceExecutionResolver.resolveWorkspace(treeUri)
        if (runJob?.isActive == true) {
            cancel("Workspace changed.")
        }
        when (resolution) {
            is WorkspaceExecutionReady -> {
                executionTarget = resolution
                _uiState.update { current ->
                    current.copy(
                        available = true,
                        workspaceLabel = resolution.workspaceLabel,
                        workspacePath = resolution.workTree.absolutePath,
                        busy = false,
                        currentCommand = "",
                        availabilityMessage = null,
                        errorMessage = null,
                        terminalText = defaultTerminalText(resolution),
                        outputText = "No command output yet.",
                        debugText = buildDebugLine(
                            "Runner ready in ${resolution.workTree.absolutePath}",
                        ),
                    )
                }
            }
            is WorkspaceExecutionUnavailable -> {
                executionTarget = null
                _uiState.update { current ->
                    current.copy(
                        available = false,
                        workspaceLabel = "",
                        workspacePath = "",
                        busy = false,
                        currentCommand = "",
                        availabilityMessage = resolution.userMessage,
                        errorMessage = null,
                        terminalText = "kinetic-syntax\n${resolution.userMessage}",
                        outputText = "No command output yet.",
                        debugText = buildDebugLine("Runner unavailable"),
                    )
                }
            }
        }
    }

    fun run(command: String): Result<Unit> {
        val prepared = prepareCommand(command).getOrElse { return Result.failure(it) }
        cancelRequested = false
        runJob = scope.launch {
            executionMutex.withLock {
                executeProcess(prepared.target, prepared.command)
            }
        }
        return Result.success(Unit)
    }

    suspend fun runForTool(command: String): Result<String> = withContext(Dispatchers.IO) {
        val prepared = prepareCommand(command).getOrElse { return@withContext Result.failure(it) }
        if (runJob?.isActive == true) {
            return@withContext Result.failure(
                IllegalStateException("A command is already running. Wait for it to finish first."),
            )
        }
        cancelRequested = false
        val result = executionMutex.withLock {
            executeProcess(prepared.target, prepared.command)
        }
        if (result.wasCancelled || result.exitCode != 0) {
            Result.failure(IllegalStateException(result.toToolResultText()))
        } else {
            Result.success(result.toToolResultText())
        }
    }

    fun rerunLastCommand(): Result<Unit> {
        val lastCommand = _uiState.value.lastCommand
        if (lastCommand.isBlank()) {
            return Result.failure(IllegalStateException("No previous command is available to rerun."))
        }
        return run(lastCommand)
    }

    fun cancel(reason: String = "Command cancelled.") {
        val process = activeProcess ?: return
        cancelRequested = true
        cancelMessage = reason
        appendDebug("Cancellation requested")
        process.destroy()
        scope.launch {
            if (!process.waitFor(1500, TimeUnit.MILLISECONDS)) {
                appendDebug("Process did not stop cleanly; forcing shutdown")
                process.destroyForcibly()
            }
        }
    }

    fun clearOutput() {
        val target = executionTarget
        _uiState.update { current ->
            current.copy(
                errorMessage = null,
                terminalText = target?.let(::defaultTerminalText) ?: current.terminalText,
                outputText = "No command output yet.",
                debugText = buildDebugLine("Output cleared"),
            )
        }
    }

    private fun prepareCommand(command: String): Result<PreparedCommand> {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter a command to run."))
        }
        val target = executionTarget
            ?: return Result.failure(
                IllegalStateException(
                    _uiState.value.availabilityMessage ?: "Open a supported workspace to run commands.",
                ),
            )
        if (!File(SHELL_PATH).exists()) {
            return Result.failure(
                IllegalStateException(
                    "This Android device does not expose /system/bin/sh for command execution.",
                ),
            )
        }
        return Result.success(
            PreparedCommand(
                target = target,
                command = trimmed,
            ),
        )
    }

    private suspend fun executeProcess(
        target: WorkspaceExecutionReady,
        command: String,
    ): CommandExecutionResult {
        val stdoutBuffer = StringBuilder()
        val stderrBuffer = StringBuilder()
        try {
            _uiState.update { current ->
                current.copy(
                    available = true,
                    workspaceLabel = target.workspaceLabel,
                    workspacePath = target.workTree.absolutePath,
                    busy = true,
                    currentCommand = command,
                    lastCommand = command,
                    errorMessage = null,
                    terminalText = appendBounded(
                        defaultTerminalText(target),
                        "$ $command",
                    ),
                    outputText = "Running: $command",
                    debugText = buildDebugLine("Launching $command"),
                )
            }
            _events.emit("Running: $command")
            val process = ProcessBuilder(SHELL_PATH, "-c", command)
                .directory(target.workTree)
                .start()
            activeProcess = process
            appendDebug("Process started in ${target.workTree.absolutePath}")
            val stdoutJob = scope.launch {
                readStream(process.inputStream) { line ->
                    appendLine(stdoutBuffer, line)
                    appendTerminal(line)
                    appendOutput(line)
                }
            }
            val stderrJob = scope.launch {
                readStream(process.errorStream) { line ->
                    appendLine(stderrBuffer, line)
                    appendTerminal("! $line")
                    appendOutput("[stderr] $line")
                }
            }
            val exitCode = process.waitFor()
            stdoutJob.join()
            stderrJob.join()
            val wasCancelled = cancelRequested
            val cancellationNotice = cancelMessage
            cancelRequested = false
            cancelMessage = "Command cancelled."
            activeProcess = null
            appendDebug("Process exited with code $exitCode")
            _uiState.update { current ->
                current.copy(
                    busy = false,
                    currentCommand = "",
                    errorMessage = when {
                        wasCancelled -> null
                        exitCode == 0 -> null
                        else -> "Command exited with code $exitCode."
                    },
                    terminalText = appendBounded(
                        current.terminalText,
                        if (wasCancelled) {
                            "[cancelled]"
                        } else {
                            "[exit $exitCode]"
                        },
                    ),
                    outputText = appendBounded(
                        current.outputText,
                        if (wasCancelled) {
                            "[cancelled]"
                        } else {
                            "[exit $exitCode]"
                        },
                    ),
                )
            }
            val event = when {
                wasCancelled -> cancellationNotice
                exitCode == 0 -> "Command finished successfully."
                else -> "Command failed with exit code $exitCode."
            }
            _events.emit(event)
            return CommandExecutionResult(
                command = command,
                exitCode = exitCode,
                wasCancelled = wasCancelled,
                stdout = stdoutBuffer.toString().trimEnd(),
                stderr = stderrBuffer.toString().trimEnd(),
            )
        } catch (e: Exception) {
            val message = e.message ?: "Command execution failed."
            activeProcess = null
            cancelRequested = false
            cancelMessage = "Command cancelled."
            appendDebug("Runner error: $message")
            _uiState.update { current ->
                current.copy(
                    busy = false,
                    currentCommand = "",
                    errorMessage = message,
                    terminalText = appendBounded(current.terminalText, "[error] $message"),
                    outputText = appendBounded(current.outputText, "[error] $message"),
                )
            }
            _events.emit(message)
            return CommandExecutionResult(
                command = command,
                exitCode = -1,
                wasCancelled = false,
                stdout = stdoutBuffer.toString().trimEnd(),
                stderr = (stderrBuffer.toString() + "\n" + message).trim(),
            )
        } finally {
            runJob = null
        }
    }

    private fun readStream(
        stream: InputStream,
        onLine: (String) -> Unit,
    ) {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                onLine(line)
            }
        }
    }

    private fun appendTerminal(line: String) {
        _uiState.update { current ->
            current.copy(
                terminalText = appendBounded(current.terminalText, line),
            )
        }
    }

    private fun appendOutput(line: String) {
        _uiState.update { current ->
            current.copy(
                outputText = appendBounded(current.outputText, line),
            )
        }
    }

    private fun appendDebug(line: String) {
        _uiState.update { current ->
            current.copy(
                debugText = appendBounded(current.debugText, buildDebugLine(line)),
            )
        }
    }

    private fun buildDebugLine(message: String): String = "[${clock.format(Date())}] $message"

    private fun defaultTerminalText(target: WorkspaceExecutionReady): String {
        return buildString {
            appendLine("kinetic-syntax")
            appendLine("Workspace: ${target.workspaceLabel}")
            append("Ready — run a command from the toolbar, command palette, or terminal panel.")
        }
    }

    private fun appendBounded(
        current: String,
        line: String,
    ): String {
        val joined = when {
            current.isBlank() -> line
            else -> "$current\n$line"
        }
        if (joined.length <= MAX_TERMINAL_CHARS) return joined
        val tail = joined.takeLast(MAX_TERMINAL_CHARS - 20)
        return "... output trimmed ...\n$tail"
    }

    private fun appendLine(buffer: StringBuilder, line: String) {
        if (buffer.isNotEmpty()) buffer.append('\n')
        buffer.append(line)
    }

    private data class PreparedCommand(
        val target: WorkspaceExecutionReady,
        val command: String,
    )
}
