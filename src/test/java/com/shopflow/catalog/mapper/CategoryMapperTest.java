package com.shopflow.catalog.mapper;

import com.shopflow.catalog.domain.model.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapperImpl();

    @Test
    void toResponse_shouldMapParentId_whenParentExists() {
        Category parent = new Category();
        parent.setId(10L);

        Category category = new Category();
        category.setId(1L);
        category.setName("Phones");
        category.setSlug("phones");
        category.setParent(parent);

        var response = mapper.toResponse(category);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Phones");
        assertThat(response.slug()).isEqualTo("phones");
        assertThat(response.parentId()).isEqualTo(10L);
        assertThat(response.children()).isNull(); // mapper deliberately leaves this - filled by service
    }

    @Test
    void toResponse_shouldMapNullParentId_whenNoParent() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setSlug("electronics");
        // no parent set - stays null

        var response = mapper.toResponse(category);

        assertThat(response.parentId()).isNull();
    }
}
