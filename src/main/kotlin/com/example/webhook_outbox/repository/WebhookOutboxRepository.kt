package com.example.webhook_outbox.repository

import com.example.webhook_outbox.entity.WebhookOutbox
import com.example.webhook_outbox.enums.Status
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.*

interface WebhookOutboxRepository : JpaRepository<WebhookOutbox, UUID> {

    @Query("""
    select w from WebhookOutbox w
    where w.status = 'PENDING' and w.nextAttemptAt <= :now
    order by w.aggregateId asc, w.seq asc
  """)
    fun findDueForDelivery(@Param("now") now: Instant): List<WebhookOutbox>

    fun findFirstByAggregateIdAndSeq(aggregateId: String, seq: Int): WebhookOutbox?

    fun findByStatus(status: Status, pageable: Pageable): List<WebhookOutbox>
}
