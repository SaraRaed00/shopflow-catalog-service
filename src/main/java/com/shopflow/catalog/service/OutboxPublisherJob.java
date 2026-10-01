package com.shopflow.catalog.service;

import com.shopflow.catalog.domain.model.OutboxEvent;
import com.shopflow.catalog.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository
            .findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, BATCH_SIZE));

        for (OutboxEvent event : pending) {
            // here is where the event would be sent to Kafka or "SQS"
            log.info("Publishing outbox event: type={} aggregateId={} payload={}",
                event.getEventType(), event.getAggregateId(), event.getPayload());

            event.setStatus("PUBLISHED");
            event.setProcessedAt(Instant.now(clock));
        }

        if (!pending.isEmpty()) {
            log.info("Published {} outbox events", pending.size());
        }
    }
}
