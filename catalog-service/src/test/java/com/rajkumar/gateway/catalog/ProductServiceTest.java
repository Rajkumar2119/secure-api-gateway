package com.rajkumar.gateway.catalog;

import com.rajkumar.gateway.catalog.api.ProductRequest;
import com.rajkumar.gateway.catalog.api.ProductResponse;
import com.rajkumar.gateway.catalog.domain.Category;
import com.rajkumar.gateway.catalog.domain.CategoryRepository;
import com.rajkumar.gateway.catalog.domain.Product;
import com.rajkumar.gateway.catalog.domain.ProductRepository;
import com.rajkumar.gateway.catalog.service.CatalogException;
import com.rajkumar.gateway.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository products;

    @Mock
    private CategoryRepository categories;

    @InjectMocks
    private ProductService service;

    private static Category electronics() {
        Category c = new Category("Electronics", "electronics");
        ReflectionTestUtils.setField(c, "id", 1L);
        return c;
    }

    private static Product headphones() {
        Product p = new Product("ELEC-0001", "Headphones", "Over-ear", new BigDecimal("129.99"), 5, electronics());
        ReflectionTestUtils.setField(p, "id", 10L);
        return p;
    }

    private static ProductRequest request(String sku, Long categoryId) {
        return new ProductRequest(sku, "Headphones", "Over-ear", new BigDecimal("129.99"), 5, categoryId, null);
    }

    @Test
    void getMapsEntityToResponse() {
        when(products.findWithCategoryById(10L)).thenReturn(Optional.of(headphones()));

        ProductResponse response = service.get(10L);

        assertThat(response.sku()).isEqualTo("ELEC-0001");
        assertThat(response.categoryName()).isEqualTo("Electronics");
    }

    @Test
    void getUnknownProductIs404() {
        when(products.findWithCategoryById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(CatalogException.class)
                .extracting(e -> ((CatalogException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createRejectsDuplicateSku() {
        when(products.existsBySku("ELEC-0001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("ELEC-0001", 1L)))
                .isInstanceOf(CatalogException.class)
                .extracting(e -> ((CatalogException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(products, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownCategory() {
        when(categories.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("NEW-1", 42L)))
                .isInstanceOf(CatalogException.class)
                .extracting(e -> ((CatalogException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createSavesProduct() {
        when(categories.findById(1L)).thenReturn(Optional.of(electronics()));
        when(products.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse created = service.create(request("NEW-1", 1L));

        assertThat(created.sku()).isEqualTo("NEW-1");
        assertThat(created.active()).isTrue();
    }

    @Test
    void deleteIsASoftDelete() {
        Product product = headphones();
        when(products.findById(10L)).thenReturn(Optional.of(product));

        service.delete(10L);

        assertThat(product.isActive()).isFalse();
    }

    @Test
    void searchByCategoryUsesTheIndexedQuery() {
        PageRequest pageable = PageRequest.of(0, 20);
        Page<Product> page = new PageImpl<>(List.of(headphones()), pageable, 1);
        when(products.findByActiveTrueAndCategoryId(1L, pageable)).thenReturn(page);

        Page<ProductResponse> result = service.search(1L, null, pageable);

        assertThat(result.getContent()).extracting(ProductResponse::sku).containsExactly("ELEC-0001");
    }
}
