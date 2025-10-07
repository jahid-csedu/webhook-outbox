package com.example.webhookoutbox.repository

import com.example.webhookoutbox.entity.WebhookOutbox
import com.example.webhookoutbox.enums.Status
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface WebhookOutboxRepository : JpaRepository<WebhookOutbox, UUID> {

    @Query("""
    select w from WebhookOutbox w
    where w.status = 'pending' and w.nextAttemptAt <= :now
    order by w.aggregateId asc, w.seq asc
  """)
    fun findDueForDelivery(@Param("now") now: OffsetDateTime): List<WebhookOutbox>

    fun findFirstByAggregateIdAndSeq(aggregateId: String, seq: Int): WebhookOutbox?

    fun findByStatus(status: Status, pageable: Pageable): List<WebhookOutbox>
}
