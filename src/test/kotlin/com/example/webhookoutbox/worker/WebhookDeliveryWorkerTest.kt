package com.example.webhookoutbox.worker

import com.example.webhookoutbox.config.TestPostgresConfig
import com.example.webhookoutbox.entity.WebhookOutbox
import com.example.webhookoutbox.enums.Status
import com.example.webhookoutbox.repository.WebhookOutboxRepository
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.reactive.function.client.WebClient
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestPostgresConfig::class]
)
@ActiveProfiles("test")
class WebhookDeliveryWorkerTest {

    @Autowired
    lateinit var repository: WebhookOutboxRepository

    @LocalServerPort
    private var port: Int = 0

    lateinit var worker: WebhookDeliveryWorker
    lateinit var webClient: WebClient

    @BeforeEach
    fun setup() {
        repository.deleteAll()

        webClient = WebClient.builder()
            .baseUrl("http://localhost:$port") // hit test receiver controller
            .build()

        worker = WebhookDeliveryWorker(repository, webClient)
        worker.hmacSecret = "test-secret"
        worker.maxAttempts = 5
        worker.baseMs = 100
        worker.factor = 1.0
        worker.jitterPercent = 0
    }

    private fun createWebhook(
        payload: String,
        aggregateId: String,
        seq: Int = 0,
        url: String = "/receiver"
    ): WebhookOutbox {
        val now = OffsetDateTime.now().minusSeconds(1)
        return WebhookOutbox(
            id = UUID.randomUUID(),
            aggregateId = aggregateId,
            seq = seq,
            payload = payload,
            targetUrl = url,
            status = Status.pending,
            attempts = 0,
            nextAttemptAt = now
        ).also { repository.save(it) }
    }

    // 1️⃣ Retry then succeed (flaky)
    @Test
    fun `flaky webhook should eventually succeed`() {
        val webhook = createWebhook("{\"foo\":\"bar\"}", "agg-flaky")

        await.atMost(10, TimeUnit.SECONDS).untilCallTo {
            worker.tick("flaky")
            repository.findById(webhook.id).get().status
        } matches { it == Status.delivered }

        val delivered = repository.findById(webhook.id).get()
        assertEquals(Status.delivered, delivered.status)
        assertEquals(3, delivered.attempts)
    }

    // 2️⃣ Fail-fast 400
    @Test
    fun `fail-fast 400 should go dead immediately`() {
        val webhook = createWebhook("{\"foo\":\"bad\"}", "agg-bad")

        worker.tick("fail-400")

        val updated = repository.findById(webhook.id).get()
        assertEquals(Status.dead, updated.status)
        assertEquals(1, updated.attempts)
    }

    // 3️⃣ Rate-limit with Retry-After header (simulated ~2s)
    @Test
    fun `rate-limit webhook should retry and eventually deliver`() {
        val webhook = createWebhook("{\"foo\":\"rl\"}", "agg-rl")

        await.atMost(10, TimeUnit.SECONDS).untilCallTo {
            worker.tick("rate-limit")
            repository.findById(webhook.id).get().status
        } matches { it == Status.delivered }

        val delivered = repository.findById(webhook.id).get()
        assertEquals(Status.delivered, delivered.status)
        assertEquals(true, delivered.attempts > 1)
    }

    // 4️⃣ Per-aggregate ordering
    @Test
    fun `webhooks of same aggregate should deliver sequentially`() {
        val w0 = createWebhook("{\"foo\":\"A0\"}", "agg-seq", seq = 0)
        val w1 = createWebhook("{\"foo\":\"A1\"}", "agg-seq", seq = 1)

        repeat(5) { worker.tick("success") }

        val first = repository.findById(w0.id).get()
        val second = repository.findById(w1.id).get()

        assertEquals(Status.delivered, first.status)
        assertEquals(Status.delivered, second.status)
        assertEquals(true, second.updatedAt!!.isAfter(first.updatedAt))
    }

    // 5️⃣ Replay DLQ
    @Test
    fun `dead webhook can be replayed and delivered`() {
        val webhook = createWebhook("{\"foo\":\"dead\"}", "agg-replay")
        webhook.status = Status.dead
        repository.save(webhook)

        // Simulate replay endpoint setting to pending
        webhook.status = Status.pending
        webhook.nextAttemptAt = OffsetDateTime.now().minusSeconds(1)
        repository.save(webhook)

        worker.tick("success")

        val delivered = repository.findById(webhook.id).get()
        assertEquals(Status.delivered, delivered.status)
    }

    // 6️⃣ Success path
    @Test
    fun `successful webhook should deliver immediately`() {
        val webhook = createWebhook("{\"foo\":\"ok\"}", "agg-ok")

        worker.tick("success")

        val delivered = repository.findById(webhook.id).get()
        assertEquals(Status.delivered, delivered.status)
        assertEquals(1, delivered.attempts)
    }
}
