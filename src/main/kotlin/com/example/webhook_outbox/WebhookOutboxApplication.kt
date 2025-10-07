package com.example.webhook_outbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebhookOutboxApplication

fun main(args: Array<String>) {
	runApplication<WebhookOutboxApplication>(*args)
}
