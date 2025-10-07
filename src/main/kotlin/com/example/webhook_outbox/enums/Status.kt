package com.example.webhook_outbox.enums

enum class Status {
    pending,
    delivering,
    delivered,
    dead
}