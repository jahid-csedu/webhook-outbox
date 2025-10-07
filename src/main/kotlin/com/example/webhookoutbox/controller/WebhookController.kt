package com.example.webhookoutbox.controller

import com.example.webhookoutbox.dto.EnqueueRequest
import com.example.webhookoutbox.dto.EnqueueResponse
import com.example.webhookoutbox.dto.OutboxItemResponse
import com.example.webhookoutbox.service.WebhookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/webhooks")
class WebhookController(private val webhookService: WebhookService) {

    @PostMapping("/enqueue")
    fun enqueue(@RequestBody request: EnqueueRequest): ResponseEntity<EnqueueResponse> {
        val response = webhookService.enqueue(request)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/outbox")
    fun find(@RequestParam status: String, @RequestParam limit: Int = 50): ResponseEntity<List<OutboxItemResponse>> {
        val response = webhookService.find(status, limit)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/outbox/{id}/replay")
    fun replay(@PathVariable id: UUID): ResponseEntity<OutboxItemResponse> {
        val response = webhookService.replay(id)
        return ResponseEntity.ok(response)
    }
}
