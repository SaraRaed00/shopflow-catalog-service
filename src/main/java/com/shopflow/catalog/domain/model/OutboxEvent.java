package com.shopflow.catalog.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String aggregateType; // Reservation

    @Column(nullable = false, length = 50)
    private String aggregateId; //reservation reference

    @Column(nullable = false, length = 50)
    private String eventType; //ReservationConfirmed

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload; // event's data as JSON

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // poller flips it to PUBLISHED

    @Column(nullable = false)
    private Instant createdAt;

    private Instant processedAt; // when it was processed
 // we have index to let the poller efficiently ask give me the oldest pending rows without scanning the whole
}
