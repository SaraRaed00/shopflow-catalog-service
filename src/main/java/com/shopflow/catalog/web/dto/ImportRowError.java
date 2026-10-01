package com.shopflow.catalog.web.dto;

public record ImportRowError (
    Long row,
    String sku,
    String code,
    String message
){
}
