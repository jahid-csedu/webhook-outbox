package com.example.webhook_outbox.dto

data class EnqueueRequest(
    val aggregateId: String,
    val seq: Int,
    val targetUrl: String,
    val payload: Any
)
