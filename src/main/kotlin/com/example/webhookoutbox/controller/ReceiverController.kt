package com.example.webhookoutbox.controller

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
    private val rateLimitTracker = ConcurrentHashMap<String, Long>()

    @PostMapping("/receiver")
    fun receive(
        @RequestHeader("X-Mode", required = false) mode: String?,
        @RequestHeader("X-Aggregate-Id", required = false) aggregateId: String?,
        @RequestBody payload: Any
    ): ResponseEntity<String> {
        println("Received webhook with mode=$mode")
        val id = aggregateId ?: "default"
        val now = System.currentTimeMillis()
        return when (mode?.lowercase()) {
            "success" -> ResponseEntity.ok("ok")

            "flaky" -> {
                val counter = flakyCounter.computeIfAbsent(id) { AtomicInteger(0) }
                if (counter.incrementAndGet() <= 2) {
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("temporary error")
                } else {
                    ResponseEntity.ok("ok")
                }
            }

            "rate-limit" -> {
                val lastAttempt = rateLimitTracker[id]

                if (lastAttempt == null) {
                    rateLimitTracker[id] = now
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, "2")
                        .body("rate limit")
                } else if (now - lastAttempt >= 2000) {
                    rateLimitTracker.remove(id)
                    ResponseEntity.ok("ok")
                } else {
                    ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, "2")
                        .body("rate limit")
                }
            }

            "fail-400" -> {
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad request")
            }

            else -> ResponseEntity.ok("ok") // default
        }
    }
}