package com.example.store.controller;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.request.CreateProductRequest;
import com.example.store.mapper.ProductMapper;
import com.example.store.service.ProductService;
import com.example.store.support.Factory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ComponentScan(basePackageClasses = ProductMapper.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    void testCreateProduct_validRequest_returnCreatedProduct() throws Exception {
        CreateProductRequest request = Factory.buildCreateProductRequest("New Product");
        ProductDTO createdProduct = Factory.buildProductDTO(100L, "New Product");

        when(productService.createProduct(anyString())).thenReturn(createdProduct);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.description").value("New Product"));
    }

    @Test
    void testCreateProduct_emptyDescription_returnBadRequest() throws Exception {
        CreateProductRequest request = new CreateProductRequest("");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_nullDescription_returnBadRequest() throws Exception {
        CreateProductRequest request = new CreateProductRequest(null);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateProduct_blankDescription_returnBadRequest() throws Exception {
        CreateProductRequest request = new CreateProductRequest("   ");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllProducts_returnPageOfProducts() throws Exception {

        List<ProductDTO> productList = Factory.buildProductDTOList();
        Pageable pageable = PageRequest.of(0, 50);
        Page<ProductDTO> productPage = new PageImpl<>(productList, pageable, productList.size());

        when(productService.getAllProducts(any(Pageable.class))).thenReturn(productPage);


        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Product 1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].description").value("Product 2"))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.size").value(50));
    }

    @Test
    void testGetAllProducts_emptyResult_returnEmptyPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        Page<ProductDTO> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(productService.getAllProducts(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void testGetProductByID_existingProduct_returnProduct() throws Exception {
        Long productId = 1L;
        ProductDTO product = Factory.buildProductDTO(productId, "Laptop - Gaming Edition");

        when(productService.getProductByID(productId)).thenReturn(product);

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Laptop - Gaming Edition"));
    }

    @Test
    void testGetProductByID_nonExistingProduct_returnNotFound() throws Exception {
        Long productId = 999L;
        when(productService.getProductByID(productId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found Product by ID " + productId));

        mockMvc.perform(get("/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetProductByID_invalidIdFormat_returnBadRequest() throws Exception {
        mockMvc.perform(get("/products/{id}", "invalid"))
                .andExpect(status().isBadRequest());
    }
}