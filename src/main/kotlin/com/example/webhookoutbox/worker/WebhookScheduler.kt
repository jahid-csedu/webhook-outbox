package com.example.webhookoutbox.worker

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WebhookScheduler(
    private val worker: WebhookDeliveryWorker
) {

    @Scheduled(fixedDelay = 1000)
    fun processPendingWebhooks() {
        worker.tick()
    }
}