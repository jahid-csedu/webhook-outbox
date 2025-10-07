package com.example.webhook_outbox.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RestController
class ReceiverController {
    private val flakyCounter = ConcurrentHashMap<String, AtomicInteger>()

    @PostMapping("/receiver")
    fun receive(
        @RequestHeader("X-Mode", required = false) mode: String?,
        @RequestHeader("X-Aggregate-Id", required = false) aggregateId: String?,
        @RequestBody payload: Any
    ): ResponseEntity<String> {
        return when (mode?.lowercase()) {
            "success" -> ResponseEntity.ok("ok")

            "flaky" -> {
                val id = aggregateId ?: "default"
                val counter = flakyCounter.computeIfAbsent(id) { AtomicInteger(0) }
                if (counter.incrementAndGet() <= 2) {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("temporary error")
                } else {
                    ResponseEntity.ok("ok")
                }
            }

            "rate-limit" -> {
                ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, "2")
                    .body("rate limit")
            }

            "fail-400" -> {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad request")
            }

            else -> ResponseEntity.ok("ok") // default
        }
    }
}