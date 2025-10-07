package com.example.webhookoutbox.dto

import com.example.webhookoutbox.enums.Status
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
