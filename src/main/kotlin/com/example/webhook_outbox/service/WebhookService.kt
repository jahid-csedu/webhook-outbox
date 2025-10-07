package com.example.webhook_outbox.service

import com.example.webhook_outbox.dto.EnqueueRequest
import com.example.webhook_outbox.dto.EnqueueResponse
import com.example.webhook_outbox.dto.OutboxItemResponse
import com.example.webhook_outbox.entity.WebhookOutbox
import com.example.webhook_outbox.enums.Status
import com.example.webhook_outbox.repository.WebhookOutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class WebhookService(
    private val webhookRepository: WebhookOutboxRepository
) {

    fun enqueue(request: EnqueueRequest): EnqueueResponse {
        val entity = WebhookOutbox(
            aggregateId = request.aggregateId,
            seq = request.seq,
            targetUrl = request.targetUrl,
            payload = ObjectMapper().writeValueAsString(request.payload),
            status = Status.pending,
            nextAttemptAt = OffsetDateTime.now()
        )
        val response = webhookRepository.save(entity)

        return EnqueueResponse(response.id, response.aggregateId, response.seq, response.status)
    }

    fun find(status: String, limit: Int): List<OutboxItemResponse> {
        val pageable = PageRequest.of(0, limit)
        val list = webhookRepository.findByStatus(Status.valueOf(status.lowercase()), pageable)

        return list.map { webhook ->
            OutboxItemResponse(
                id = webhook.id,
                aggregateId = webhook.aggregateId,
                seq = webhook.seq,
                status = webhook.status,
                attempts = webhook.attempts,
                nextAttemptAt = webhook.nextAttemptAt,
                httpCode = webhook.httpCode
            )
        }
    }

    fun replay(id: UUID): OutboxItemResponse {
        val webhook = webhookRepository.findById(id)
            .orElseThrow { IllegalArgumentException("Webhook not found: $id") }

        if (webhook.status != Status.dead) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Webhook $id is not in dead state, cannot replay")
        }

        webhook.status = Status.pending
        webhook.attempts = 0
        webhook.nextAttemptAt = OffsetDateTime.now()
        webhook.updatedAt = OffsetDateTime.now()

        val updated = webhookRepository.save(webhook)

        return OutboxItemResponse(
            id = updated.id,
            aggregateId = updated.aggregateId,
            seq = updated.seq,
            status = updated.status,
            attempts = updated.attempts,
            nextAttemptAt = updated.nextAttemptAt,
            httpCode = updated.httpCode
        )
    }
}