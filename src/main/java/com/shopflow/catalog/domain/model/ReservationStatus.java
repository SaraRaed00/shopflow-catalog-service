package com.shopflow.catalog.domain.model;

public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    RELEASED,
    EXPIRED;

    public boolean canTransitionTo(ReservationStatus target){
        return switch(this){
            case PENDING -> (target == CONFIRMED) ||
                target == RELEASED ||
                target == EXPIRED;
            case CONFIRMED, RELEASED, EXPIRED -> false;
        };
    }
}

