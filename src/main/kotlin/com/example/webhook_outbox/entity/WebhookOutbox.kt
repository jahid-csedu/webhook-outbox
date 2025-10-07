package com.example.webhook_outbox.entity

import com.example.webhook_outbox.enums.Status
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "webhooks_outbox",
    uniqueConstraints = [UniqueConstraint(columnNames = ["aggregate_id", "seq"])]
)
data class WebhookOutbox(
    @Id
    var id: UUID = UUID.randomUUID(),

    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: String,

    @Column(nullable = false)
    var seq: Int,

    @Column(name = "target_url", nullable = false)
    var targetUrl: String,

    @Column(columnDefinition = "text", nullable = false)
    var payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Status = Status.pending,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "next_attempt_at")
    var nextAttemptAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "http_code")
    var httpCode: Int? = null,

    @Column(name = "last_error")
    var lastError: String? = null,

    @Column(name = "created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
