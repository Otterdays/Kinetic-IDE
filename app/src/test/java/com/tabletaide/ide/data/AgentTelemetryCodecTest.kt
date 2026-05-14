package com.tabletaide.ide.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTelemetryCodecTest {

    @Test
    fun summarizeAggregatesTokensLatencyCostAndFailures() {
        val events = listOf(
            TelemetryEvent(
                sequence = 1,
                sessionId = "s",
                turnId = "t1",
                type = TelemetryEventType.PROMPT_SENT,
                payload = JSONObject().put("promptTokens", 100),
            ),
            TelemetryEvent(
                sequence = 2,
                sessionId = "s",
                turnId = "t1",
                type = TelemetryEventType.MODEL_COMPLETED,
                payload = JSONObject()
                    .put("completionTokens", 40)
                    .put("firstTokenMs", 250),
            ),
            TelemetryEvent(
                sequence = 3,
                sessionId = "s",
                turnId = "t1",
                type = TelemetryEventType.TOOL_COMPLETED,
                payload = JSONObject()
                    .put("durationMs", 120)
                    .put("status", "FAILED"),
            ),
            TelemetryEvent(
                sequence = 4,
                sessionId = "s",
                turnId = "t1",
                type = TelemetryEventType.COST_ESTIMATED,
                payload = JSONObject().put("estimatedCostUsd", 0.0012),
            ),
        )

        val summary = AgentTelemetryCodec.summarize(events)

        assertEquals(4, summary.eventCount)
        assertEquals(1, summary.turnCount)
        assertEquals(100L, summary.promptTokens)
        assertEquals(40L, summary.completionTokens)
        assertEquals(250L, summary.averageFirstTokenMs)
        assertEquals(120L, summary.p95ToolMs)
        assertEquals(1, summary.failureCount)
        assertEquals(0.0012, summary.estimatedCostUsd, 0.00001)
    }

    @Test
    fun eventJsonRoundTripPreservesCoreFields() {
        val payload = JSONObject()
            .put("toolName", "read_file")
            .put("target", "README.md")
        val event = TelemetryEvent(
            sequence = 7,
            sessionId = "session",
            turnId = "turn",
            spanId = "span",
            parentSpanId = "parent",
            type = TelemetryEventType.TOOL_COMPLETED,
            payload = payload,
            payloadHash = AgentTelemetryCodec.sha256(payload.toString()),
        )

        val decoded = AgentTelemetryCodec.fromJson(AgentTelemetryCodec.toJson(event))

        requireNotNull(decoded)
        assertEquals(event.sequence, decoded.sequence)
        assertEquals(event.sessionId, decoded.sessionId)
        assertEquals(event.turnId, decoded.turnId)
        assertEquals(event.spanId, decoded.spanId)
        assertEquals(event.parentSpanId, decoded.parentSpanId)
        assertEquals(event.type, decoded.type)
        assertEquals(event.payloadHash, decoded.payloadHash)
        assertEquals("read_file", decoded.payload.optString("toolName"))
    }

    @Test
    fun tokenAndCostEstimatorAreExplicitlyRough() {
        val tokens = AgentTelemetryCodec.estimateTokens("1234567890123456")
        val cost = AgentTelemetryCodec.estimateCost(inputTokens = tokens, outputTokens = tokens)

        assertEquals(4L, tokens)
        assertTrue(cost.estimatedUsd > 0.0)
        assertEquals("rough_char_estimator_v1", cost.source)
    }
}
