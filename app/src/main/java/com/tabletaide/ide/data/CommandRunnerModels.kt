package com.tabletaide.ide.data

import java.io.File

sealed interface WorkspaceExecutionResolution

data class WorkspaceExecutionReady(
    val workTree: File,
    val workspaceLabel: String,
) : WorkspaceExecutionResolution

data class WorkspaceExecutionUnavailable(
    val userMessage: String,
) : WorkspaceExecutionResolution

data class CommandRunnerUiState(
    val available: Boolean = false,
    val workspaceLabel: String = "",
    val workspacePath: String = "",
    val busy: Boolean = false,
    val currentCommand: String = "",
    val lastCommand: String = "",
    val availabilityMessage: String? = "Open a workspace to run commands.",
    val errorMessage: String? = null,
    val terminalText: String = "",
    val outputText: String = "",
    val debugText: String = "",
)

data class RunCommandDialogState(
    val visible: Boolean = false,
    val command: String = "",
    val errorMessage: String? = null,
)

data class AgentToolApprovalState(
    val visible: Boolean = false,
    val requestId: String = "",
    val toolName: String = "",
    val riskClass: AgentToolRiskClass = AgentToolRiskClass.READ_ONLY,
    val target: String = "",
    val workspacePath: String = "",
    val inputJson: String = "",
    val assistantExplanation: String = "",
    val policyMode: AgentToolPolicyMode = AgentToolPolicyMode.ASK,
)

data class CommandExecutionResult(
    val command: String,
    val exitCode: Int,
    val wasCancelled: Boolean,
    val stdout: String,
    val stderr: String,
) {
    fun toToolResultText(): String {
        return buildString {
            appendLine("command: $command")
            appendLine("exit_code: $exitCode")
            appendLine("cancelled: $wasCancelled")
            appendLine()
            appendLine("stdout:")
            appendLine(if (stdout.isBlank()) "(empty)" else stdout)
            appendLine()
            appendLine("stderr:")
            append(if (stderr.isBlank()) "(empty)" else stderr)
        }.trim()
    }
}
