package com.example.webhookoutbox.worker

import com.example.webhookoutbox.entity.WebhookOutbox
import com.example.webhookoutbox.enums.Status
import com.example.webhookoutbox.repository.WebhookOutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow
import kotlin.random.Random

@Service
class WebhookDeliveryWorker(
    private val repository: WebhookOutboxRepository,
    private val webClient: WebClient
) {

    @Value("\${webhook.hmac-secret}")
    lateinit var hmacSecret: String

    @Value("\${webhook.max-attempts}")
    var maxAttempts: Int = 10

    @Value("\${webhook.backoff.base-ms}")
    var baseMs: Long = 1000

    @Value("\${webhook.backoff.factor}")
    var factor: Double = 2.0

    @Value("\${webhook.backoff.max-ms}")
    private var maxMs: Long = 300_000

    @Value("\${webhook.backoff.jitter-percent}")
    var jitterPercent: Int = 10

    fun tick(mode: String? = null) {
        val now = OffsetDateTime.now()
        val dueWebhooks = repository.findDueForDelivery(now)
        println("Running tick at $now, found ${dueWebhooks.size} webhooks")

        for (webhook in dueWebhooks) {
            val prevSeq = webhook.seq - 1
            val prevDelivered = if (prevSeq >= 0) {
                repository.findFirstByAggregateIdAndSeq(webhook.aggregateId, prevSeq)?.status == Status.delivered
            } else true

            if (!prevDelivered) continue
            println("trying to delivery webhook")
            tryDeliver(webhook, mode)
        }
    }

    private fun tryDeliver(webhook: WebhookOutbox, mode: String?) {
        webhook.status = Status.delivering
        webhook.updatedAt = OffsetDateTime.now()
        repository.save(webhook)

        val timestamp = Instant.now().toEpochMilli()
        val signature = hmacSha256("$timestamp.${webhook.payload}", hmacSecret)

        try {
            val response = callWebClient(webhook, timestamp, signature, mode)
            println("Response $response")
            when {
                response.statusCode.is2xxSuccessful -> {
                    handleSuccess(webhook, response)
                }

                response.statusCode == HttpStatus.TOO_MANY_REQUESTS -> {
                    retryWebhook(
                        webhook,
                        retryAfter = response.headers[HttpHeaders.RETRY_AFTER]?.firstOrNull()?.toLong()?.times(1000)
                    )
                }

                response.statusCode.is5xxServerError || response.statusCode.value() == 408 -> {
                    retryWebhook(webhook)
                }

                else -> {
                    handleFailure(webhook, response)
                }
            }
        } catch (ex: Exception) {
            retryWebhook(webhook, lastError = ex.message)
        }

        webhook.updatedAt = OffsetDateTime.now()
        repository.save(webhook)
        println("Delivery result for webhook ${webhook.id}: ${webhook.status}")
    }


    private fun callWebClient(
        webhook: WebhookOutbox,
        timestamp: Long,
        signature: String,
        mode: String?
    ): ResponseEntity<String?> {
        return webClient.post()
            .uri(webhook.targetUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-Webhooks-Signature", "t=$timestamp,s=$signature")
            .apply {
                if (!mode.isNullOrBlank()) header("X-Mode", mode)
                header("X-Aggregate-Id", webhook.aggregateId)
            }
            .bodyValue(webhook.payload)
            .exchangeToMono { response ->
                response.toEntity(String::class.java)
            }
            .block()!!
    }

    private fun handleSuccess(
        webhook: WebhookOutbox,
        response: ResponseEntity<String?>
    ) {
        webhook.status = Status.delivered
        webhook.attempts += 1
        webhook.httpCode = response.statusCode.value()
        webhook.lastError = null
    }

    private fun handleFailure(
        webhook: WebhookOutbox,
        response: ResponseEntity<String?>
    ) {
        webhook.status = Status.dead
        webhook.attempts += 1
        webhook.httpCode = response.statusCode.value()
        webhook.lastError = "non-retryable error"
    }

    private fun retryWebhook(webhook: WebhookOutbox, retryAfter: Long? = null, lastError: String? = null) {
        webhook.attempts += 1
        webhook.lastError = lastError
        if (webhook.attempts >= maxAttempts) {
            webhook.status = Status.dead
        } else {
            webhook.status = Status.pending
            val backoff = retryAfter ?: (baseMs * factor.pow(webhook.attempts.toDouble())).toLong()
            val nextAttempt = Instant.now().plusMillis(if (retryAfter != null) backoff else jitter(backoff))
            webhook.nextAttemptAt = OffsetDateTime.ofInstant(nextAttempt, ZoneId.systemDefault())
            println("Next Attempt: ${webhook.nextAttemptAt}")
        }
    }

    private fun jitter(ms: Long): Long {
        val jitterAmount = (ms * jitterPercent / 100.0).toLong()
        if (jitterAmount <= 0) return ms // no jitter possible for very small ms

        val min = ms - jitterAmount
        val max = ms + jitterAmount
        // Ensure max > min, otherwise fallback to ms
        return if (max > min) Random.nextLong(min, max) else ms
    }

    private fun hmacSha256(data: String, secret: String): String {
        val hmacKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(hmacKey)
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
