package com.example.webhookoutbox.dto

data class EnqueueRequest(
    val aggregateId: String,
    val seq: Int,
    val targetUrl: String,
    val payload: Any
)
