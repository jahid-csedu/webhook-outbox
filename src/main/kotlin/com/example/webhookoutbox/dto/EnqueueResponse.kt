package com.example.webhookoutbox.dto


import com.example.webhookoutbox.enums.Status
import java.util.UUID

data class EnqueueResponse(
    val id: UUID,
    val aggregateId: String,
    val seq: Int,
    val status: Status
)
