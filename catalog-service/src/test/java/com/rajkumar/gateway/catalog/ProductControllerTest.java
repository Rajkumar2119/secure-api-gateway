package com.rajkumar.gateway.catalog;

import com.rajkumar.gateway.catalog.api.ProductController;
import com.rajkumar.gateway.catalog.api.ProductResponse;
import com.rajkumar.gateway.catalog.config.SecurityConfig;
import com.rajkumar.gateway.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RBAC rules of the catalogue API: anyone signed in can read, only ADMIN can write. */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"sku":"NEW-1","name":"Desk Lamp","price":19.99,"stockQuantity":3,"categoryId":2}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService productService;

    private static RequestPostProcessor user() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static ProductResponse lamp(long id) {
        return new ProductResponse(id, "NEW-1", "Desk Lamp", null, new BigDecimal("19.99"), 3, 2L, "Home", true,
                Instant.parse("2026-01-01T00:00:00Z"), 0);
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/products/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadProducts() throws Exception {
        when(productService.get(1L)).thenReturn(lamp(1));

        mvc.perform(get("/api/v1/products/1").with(user()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Desk Lamp"))
                .andExpect(jsonPath("$.price").value(19.99));
    }

    @Test
    void userCannotCreateProducts() throws Exception {
        mvc.perform(post("/api/v1/products").with(user()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotDeleteProducts() throws Exception {
        mvc.perform(delete("/api/v1/products/1").with(user())).andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateProducts() throws Exception {
        when(productService.create(any())).thenReturn(lamp(6));

        mvc.perform(post("/api/v1/products").with(admin()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/6"))
                .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    void adminCanDeleteProducts() throws Exception {
        mvc.perform(delete("/api/v1/products/1").with(admin())).andExpect(status().isNoContent());
        verify(productService).delete(1L);
    }

    @Test
    void invalidBodyIs400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/products").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"bad sku\",\"name\":\"\",\"price\":-1,\"stockQuantity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stockQuantity").exists())
                .andExpect(jsonPath("$.errors.categoryId").exists());
    }
}
