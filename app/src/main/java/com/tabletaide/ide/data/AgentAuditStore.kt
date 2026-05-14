package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class AuditEntry(
    val id: String,
    val timestamp: String,
    val epochMs: Long,
    val provider: String,
    val toolName: String,
    val target: String,
    val status: String,
    val riskClass: String,
    val policyMode: String,
    val approvalOutcome: String,
    val durationMs: Long,
    val mutationSummary: String,
)

@Singleton
class AgentAuditStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val file: File
        get() = File(context.filesDir, AUDIT_FILE_NAME)

    suspend fun append(entry: AuditEntry) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val json = entryToJson(entry)
            file.appendText(json.toString() + "\n")
            trimIfNeeded()
        }
    }

    suspend fun loadAll(): List<AuditEntry> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val f = file
            if (!f.exists()) return@withContext emptyList()
            f.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        jsonToEntry(JSONObject(line))
                    } catch (_: Exception) {
                        null
                    }
                }
        }
    }

    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) {
            file.delete()
        }
    }

    fun entryCount(): Int {
        val f = file
        if (!f.exists()) return 0
        return f.readLines().count { it.isNotBlank() }
    }

    private fun trimIfNeeded() {
        val f = file
        if (!f.exists()) return
        val lines = f.readLines().filter { it.isNotBlank() }
        if (lines.size <= MAX_ENTRIES) return
        val trimmed = lines.takeLast(MAX_ENTRIES)
        f.writeText(trimmed.joinToString("\n") + "\n")
    }

    private fun entryToJson(entry: AuditEntry): JSONObject = JSONObject().apply {
        put("id", entry.id)
        put("timestamp", entry.timestamp)
        put("epochMs", entry.epochMs)
        put("provider", entry.provider)
        put("toolName", entry.toolName)
        put("target", entry.target)
        put("status", entry.status)
        put("riskClass", entry.riskClass)
        put("policyMode", entry.policyMode)
        put("approvalOutcome", entry.approvalOutcome)
        put("durationMs", entry.durationMs)
        put("mutationSummary", entry.mutationSummary)
    }

    private fun jsonToEntry(json: JSONObject): AuditEntry = AuditEntry(
        id = json.optString("id"),
        timestamp = json.optString("timestamp"),
        epochMs = json.optLong("epochMs"),
        provider = json.optString("provider"),
        toolName = json.optString("toolName"),
        target = json.optString("target"),
        status = json.optString("status"),
        riskClass = json.optString("riskClass"),
        policyMode = json.optString("policyMode"),
        approvalOutcome = json.optString("approvalOutcome"),
        durationMs = json.optLong("durationMs"),
        mutationSummary = json.optString("mutationSummary"),
    )

    fun formatTimestamp(epochMs: Long): String = dateFormat.format(Date(epochMs))

    private companion object {
        const val AUDIT_FILE_NAME = "agent_audit.jsonl"
        const val MAX_ENTRIES = 1000
    }
}
