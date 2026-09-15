package com.shopflow.catalog.Repository;

import com.shopflow.catalog.domain.model.Category;
import com.shopflow.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void save_shouldReject_whenSlugAlreadyExists() {
        Category first = new Category();
        first.setName("Books");
        first.setSlug("duplicate-slug-test");
        categoryRepository.saveAndFlush(first);

        Category second = new Category();
        second.setName("Books Again");
        second.setSlug("duplicate-slug-test");

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
