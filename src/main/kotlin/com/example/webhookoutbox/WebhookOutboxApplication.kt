package com.example.webhookoutbox

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WebhookOutboxApplication

fun main(args: Array<String>) {
	runApplication<WebhookOutboxApplication>(*args)
}
