package com.example.webhook_outbox.dto

import com.example.webhook_outbox.enums.Status
import java.time.OffsetDateTime
import java.util.UUID

data class OutboxItemResponse(
    val id: UUID,
    val aggregateId: String,
    val seq: Int,
    val status: Status,
    val attempts: Int,
    val nextAttemptAt: OffsetDateTime?,
    val httpCode: Int?
)
