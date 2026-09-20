package com.shopflow.catalog.domain.model;

public enum ProductStatus {
    DRAFT,
    ACTIVE,
    DISCONTINUED;

    public boolean canTransitionTo(ProductStatus target){
        return switch(this){
            case DRAFT -> target == ACTIVE;
            case ACTIVE -> target == DISCONTINUED;
            case DISCONTINUED -> false;
        };
    }
}
