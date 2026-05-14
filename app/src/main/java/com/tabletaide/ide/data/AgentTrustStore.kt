package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class AgentToolPolicyMode(
    val id: String,
    val displayName: String,
) {
    AUTO("auto", "Auto"),
    ASK("ask", "Ask"),
    DENY("deny", "Deny"),
}

enum class AgentToolRiskClass(val displayName: String) {
    READ_ONLY("Read-only"),
    FILE_MUTATION("File changes"),
    DESTRUCTIVE("Destructive ops"),
    COMMAND("Shell commands"),
}

data class AgentTrustPolicyState(
    val fileMutationMode: AgentToolPolicyMode = AgentToolPolicyMode.ASK,
    val destructiveMode: AgentToolPolicyMode = AgentToolPolicyMode.ASK,
    val commandMode: AgentToolPolicyMode = AgentToolPolicyMode.ASK,
) {
    fun modeFor(riskClass: AgentToolRiskClass): AgentToolPolicyMode = when (riskClass) {
        AgentToolRiskClass.READ_ONLY -> AgentToolPolicyMode.AUTO
        AgentToolRiskClass.FILE_MUTATION -> fileMutationMode
        AgentToolRiskClass.DESTRUCTIVE -> destructiveMode
        AgentToolRiskClass.COMMAND -> commandMode.coerceForCommands()
    }

    fun withMode(
        riskClass: AgentToolRiskClass,
        mode: AgentToolPolicyMode,
    ): AgentTrustPolicyState = when (riskClass) {
        AgentToolRiskClass.READ_ONLY -> this
        AgentToolRiskClass.FILE_MUTATION -> copy(fileMutationMode = mode)
        AgentToolRiskClass.DESTRUCTIVE -> copy(destructiveMode = mode)
        AgentToolRiskClass.COMMAND -> copy(commandMode = mode.coerceForCommands())
    }

    fun normalized(): AgentTrustPolicyState = copy(
        commandMode = commandMode.coerceForCommands(),
    )

    private fun AgentToolPolicyMode.coerceForCommands(): AgentToolPolicyMode {
        return if (this == AgentToolPolicyMode.AUTO) AgentToolPolicyMode.ASK else this
    }
}

@Singleton
class AgentTrustStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val prefs get() = context.getSharedPreferences("kinetic_agent_trust_settings", Context.MODE_PRIVATE)

    fun load(): AgentTrustPolicyState {
        return AgentTrustPolicyState(
            fileMutationMode = loadMode(FILE_MUTATION_MODE_KEY, AgentToolPolicyMode.ASK),
            destructiveMode = loadMode(DESTRUCTIVE_MODE_KEY, AgentToolPolicyMode.ASK),
            commandMode = loadMode(COMMAND_MODE_KEY, AgentToolPolicyMode.ASK),
        ).normalized()
    }

    fun save(state: AgentTrustPolicyState) {
        prefs.edit()
            .putString(FILE_MUTATION_MODE_KEY, state.fileMutationMode.id)
            .putString(DESTRUCTIVE_MODE_KEY, state.destructiveMode.id)
            .putString(COMMAND_MODE_KEY, state.commandMode.id)
            .apply()
    }

    private fun loadMode(
        key: String,
        fallback: AgentToolPolicyMode,
    ): AgentToolPolicyMode {
        val stored = prefs.getString(key, null) ?: return fallback
        return AgentToolPolicyMode.entries.find { it.id == stored } ?: fallback
    }

    private companion object {
        const val FILE_MUTATION_MODE_KEY = "file_mutation_mode"
        const val DESTRUCTIVE_MODE_KEY = "destructive_mode"
        const val COMMAND_MODE_KEY = "command_mode"
    }
}
