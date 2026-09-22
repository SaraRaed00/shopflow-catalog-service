package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Warehouse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarehouseMapperTest {

    private final WarehouseMapper mapper = new WarehouseMapperImpl();

    @Test
    void toResponse_shouldMapAllFieldsCorrectly() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setCode("KWT-MAIN");
        warehouse.setName("Kuwait Main Warehouse");
        warehouse.setCountry("KW");

        var response = mapper.toResponse(warehouse);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("KWT-MAIN");
        assertThat(response.name()).isEqualTo("Kuwait Main Warehouse");
        assertThat(response.country()).isEqualTo("KW");
    }
}
