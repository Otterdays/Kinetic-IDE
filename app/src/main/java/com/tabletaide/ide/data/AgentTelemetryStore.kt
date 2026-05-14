package com.tabletaide.ide.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

enum class TelemetryEventType(val wireName: String) {
    SESSION_STARTED("session_started"),
    TURN_STARTED("turn_started"),
    CONTEXT_BUILT("context_built"),
    PROMPT_SENT("prompt_sent"),
    MODEL_DELTA("model_delta"),
    MODEL_COMPLETED("model_completed"),
    TOOL_REQUESTED("tool_requested"),
    APPROVAL_REQUESTED("approval_requested"),
    APPROVAL_DECIDED("approval_decided"),
    TOOL_STARTED("tool_started"),
    TOOL_COMPLETED("tool_completed"),
    MUTATION_SNAPSHOT_CREATED("mutation_snapshot_created"),
    MUTATION_APPLIED("mutation_applied"),
    MUTATION_REVERTED("mutation_reverted"),
    CHECKPOINT_CREATED("checkpoint_created"),
    CHECKPOINT_RESTORED("checkpoint_restored"),
    COST_ESTIMATED("cost_estimated"),
    ERROR_RECORDED("error_recorded"),
    SESSION_EXPORTED("session_exported"),
}

data class TelemetryEvent(
    val id: String = UUID.randomUUID().toString(),
    val schemaVersion: Int = AgentTelemetryCodec.SCHEMA_VERSION,
    val sequence: Long = 0L,
    val epochMs: Long = System.currentTimeMillis(),
    val sessionId: String,
    val turnId: String? = null,
    val spanId: String? = null,
    val parentSpanId: String? = null,
    val type: TelemetryEventType,
    val redactionClass: String = "metadata",
    val payload: JSONObject = JSONObject(),
    val payloadHash: String = AgentTelemetryCodec.sha256(payload.toString()),
)

data class TelemetrySummary(
    val eventCount: Int = 0,
    val turnCount: Int = 0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val estimatedCostUsd: Double = 0.0,
    val averageFirstTokenMs: Long? = null,
    val p95ToolMs: Long? = null,
    val failureCount: Int = 0,
    val lastEventLabel: String = "No telemetry yet",
) {
    val totalTokens: Long
        get() = promptTokens + completionTokens
}

data class TelemetryCostEstimate(
    val inputTokens: Long,
    val outputTokens: Long,
    val estimatedUsd: Double,
    val source: String,
)

object AgentTelemetryCodec {
    const val SCHEMA_VERSION = 1
    private const val ESTIMATED_INPUT_PER_MILLION = 3.0
    private const val ESTIMATED_OUTPUT_PER_MILLION = 15.0

    fun estimateTokens(text: String): Long {
        if (text.isBlank()) return 0L
        return (text.length / 4.0).roundToLong().coerceAtLeast(1L)
    }

    fun estimateCost(inputTokens: Long, outputTokens: Long): TelemetryCostEstimate {
        val cost =
            (inputTokens / 1_000_000.0 * ESTIMATED_INPUT_PER_MILLION) +
                (outputTokens / 1_000_000.0 * ESTIMATED_OUTPUT_PER_MILLION)
        return TelemetryCostEstimate(
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            estimatedUsd = cost,
            source = "rough_char_estimator_v1",
        )
    }

    fun toJson(event: TelemetryEvent): JSONObject = JSONObject().apply {
        put("schemaVersion", event.schemaVersion)
        put("sequence", event.sequence)
        put("id", event.id)
        put("epochMs", event.epochMs)
        put("sessionId", event.sessionId)
        put("turnId", event.turnId)
        put("spanId", event.spanId)
        put("parentSpanId", event.parentSpanId)
        put("type", event.type.wireName)
        put("redactionClass", event.redactionClass)
        put("payloadHash", event.payloadHash)
        put("payload", event.payload)
    }

    fun fromJson(json: JSONObject): TelemetryEvent? {
        val typeName = json.optString("type")
        val type = TelemetryEventType.entries.firstOrNull { it.wireName == typeName } ?: return null
        return TelemetryEvent(
            id = json.optString("id"),
            schemaVersion = json.optInt("schemaVersion", SCHEMA_VERSION),
            sequence = json.optLong("sequence"),
            epochMs = json.optLong("epochMs"),
            sessionId = json.optString("sessionId"),
            turnId = json.optString("turnId").takeIf { it.isNotBlank() && it != "null" },
            spanId = json.optString("spanId").takeIf { it.isNotBlank() && it != "null" },
            parentSpanId = json.optString("parentSpanId").takeIf { it.isNotBlank() && it != "null" },
            type = type,
            redactionClass = json.optString("redactionClass", "metadata"),
            payload = json.optJSONObject("payload") ?: JSONObject(),
            payloadHash = json.optString("payloadHash"),
        )
    }

    fun summarize(events: List<TelemetryEvent>): TelemetrySummary {
        if (events.isEmpty()) return TelemetrySummary()
        val promptTokens = events.sumOf { it.payload.optLong("promptTokens", 0L) }
        val completionTokens = events.sumOf { it.payload.optLong("completionTokens", 0L) }
        val cost = events.sumOf { it.payload.optDouble("estimatedCostUsd", 0.0) }
        val firstTokenValues = events
            .mapNotNull { it.payload.optLong("firstTokenMs", -1L).takeIf { value -> value >= 0L } }
        val toolDurations = events
            .filter { it.type == TelemetryEventType.TOOL_COMPLETED }
            .mapNotNull { it.payload.optLong("durationMs", -1L).takeIf { value -> value >= 0L } }
            .sorted()
        val failures = events.count {
            it.type == TelemetryEventType.ERROR_RECORDED ||
                it.payload.optString("status").equals("FAILED", ignoreCase = true)
        }
        val last = events.maxByOrNull { it.sequence } ?: events.last()
        return TelemetrySummary(
            eventCount = events.size,
            turnCount = events.mapNotNull { it.turnId }.distinct().size,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            estimatedCostUsd = cost,
            averageFirstTokenMs = firstTokenValues.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            p95ToolMs = percentile95(toolDurations),
            failureCount = failures,
            lastEventLabel = "${last.type.wireName} · ${formatTime(last.epochMs)}",
        )
    }

    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun percentile95(sortedValues: List<Long>): Long? {
        if (sortedValues.isEmpty()) return null
        val index = ((sortedValues.size - 1) * 0.95).roundToLong().toInt()
        return sortedValues[index.coerceIn(sortedValues.indices)]
    }

    private fun formatTime(epochMs: Long): String =
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(epochMs))
}

@Singleton
class AgentTelemetryStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var cachedEvents: MutableList<TelemetryEvent>? = null

    private val file: File
        get() = File(context.filesDir, TELEMETRY_FILE_NAME)

    suspend fun append(event: TelemetryEvent): TelemetryEvent = mutex.withLock {
        withContext(Dispatchers.IO) {
            val events = eventsUnsafe()
            val next = event.copy(sequence = ((events.maxOfOrNull { it.sequence } ?: 0L) + 1L))
            events += next
            file.appendText(AgentTelemetryCodec.toJson(next).toString() + "\n")
            trimIfNeededUnsafe(events)
            next
        }
    }

    suspend fun loadRecent(limit: Int = MAX_EVENTS): List<TelemetryEvent> = mutex.withLock {
        withContext(Dispatchers.IO) {
            eventsUnsafe().takeLast(limit.coerceAtLeast(1))
        }
    }

    suspend fun summary(): TelemetrySummary = AgentTelemetryCodec.summarize(loadRecent())

    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) {
            file.delete()
            cachedEvents = mutableListOf()
        }
    }

    private fun eventsUnsafe(): MutableList<TelemetryEvent> {
        cachedEvents?.let { return it }
        val f = file
        val loaded = if (!f.exists()) {
            mutableListOf()
        } else {
            f.readLines()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    try {
                        AgentTelemetryCodec.fromJson(JSONObject(line))
                    } catch (_: Exception) {
                        null
                    }
                }
                .toMutableList()
        }
        cachedEvents = loaded
        return loaded
    }

    private fun trimIfNeededUnsafe(events: MutableList<TelemetryEvent>) {
        if (events.size <= MAX_EVENTS) return
        val keep = events.takeLast(MAX_EVENTS)
        events.clear()
        events.addAll(keep)
        file.writeText(keep.joinToString("\n") { AgentTelemetryCodec.toJson(it).toString() } + "\n")
    }

    private companion object {
        const val TELEMETRY_FILE_NAME = "agent_telemetry.jsonl"
        const val MAX_EVENTS = 10_000
    }
}
