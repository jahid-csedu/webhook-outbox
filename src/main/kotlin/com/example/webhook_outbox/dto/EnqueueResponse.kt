package com.example.webhook_outbox.dto


import com.example.webhook_outbox.enums.Status
import java.util.UUID

data class EnqueueResponse(
    val id: UUID,
    val aggregateId: String,
    val seq: Int,
    val status: Status
)
