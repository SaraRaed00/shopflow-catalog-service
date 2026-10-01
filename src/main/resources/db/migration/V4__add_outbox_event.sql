CREATE TABLE outbox_event (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              aggregate_type VARCHAR(50) NOT NULL,
                              aggregate_id VARCHAR(50) NOT NULL,
                              event_type VARCHAR(50) NOT NULL,
                              payload TEXT NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              created_at TIMESTAMP NOT NULL,
                              processed_at TIMESTAMP NULL
);

CREATE INDEX ix_outbox_status_created ON outbox_event (status, created_at);
