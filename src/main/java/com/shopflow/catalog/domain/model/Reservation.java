package com.shopflow.catalog.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import com.shopflow.catalog.domain.exception.ConflictException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column (nullable = false, length = 100) private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false ) @JoinColumn(name = "warehouse_id", nullable = false) private Warehouse warehouse;

    @Column(nullable = false)private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void changeStatus(ReservationStatus target){
        if(!status.canTransitionTo(target)){
            throw new ConflictException("ILLEGAL_STATUS_TRANSITION", "CANNOT MOVE FROM: "+ status + " TO: " + target );
        }
        this.status = target;
    }
}
