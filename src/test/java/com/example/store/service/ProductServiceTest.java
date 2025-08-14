package com.example.store.service;

import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;

import com.example.store.support.Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;
    private ProductDTO productDTO1;
    private ProductDTO productDTO2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        product1 = Factory.buildProduct(1L, "Product 1");
        product2 = Factory.buildProduct(2L, "Product 2");

        productDTO1 = Factory.buildProductDTO(1L, "Product 1");
        productDTO2 = Factory.buildProductDTO(2L, "Product 2");

        pageable = PageRequest.of(0, 10);
    }
    
    @Test
    void testCreateProduct_withValidDescription_returnCreatedProduct() {
        String description = "New Product";

        Product savedProduct = Product.builder()
                .id(3L)
                .description(description)
                .build();

        ProductDTO expectedDTO = new ProductDTO();
        expectedDTO.setId(3L);
        expectedDTO.setDescription(description);

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        when(productMapper.productToProductDTO(savedProduct)).thenReturn(expectedDTO);

        ProductDTO result = productService.createProduct(description);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getDescription()).isEqualTo(description);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product capturedProduct = productCaptor.getValue();
        assertThat(capturedProduct.getDescription()).isEqualTo(description);
        assertThat(capturedProduct.getId()).isNull(); 

        verify(productMapper).productToProductDTO(savedProduct);
    }

    @Test
    void testGetAllProducts_withProducts_returnPageOfProductDTOs() {
        List<Product> productList = List.of(product1, product2);
        Page<Product> productPage = new PageImpl<>(productList, pageable, productList.size());

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productMapper.productToProductDTO(product1)).thenReturn(productDTO1);
        when(productMapper.productToProductDTO(product2)).thenReturn(productDTO2);

        Page<ProductDTO> result = productService.getAllProducts(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Product 1");
        assertThat(result.getContent().get(1).getDescription()).isEqualTo("Product 2");
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(productRepository).findAll(pageable);
        verify(productMapper, times(2)).productToProductDTO(any(Product.class));
    }

    @Test
    void testGetAllProducts_emptyResult_returnEmptyPage() {
        Page<Product> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<ProductDTO> result = productService.getAllProducts(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.isEmpty()).isTrue();

        verify(productRepository).findAll(pageable);
        verify(productMapper, never()).productToProductDTO(any(Product.class));
    }

    @Test
    void testGetAllProducts_hasTransactionalReadOnlyAnnotation() throws NoSuchMethodException {
        Method method = ProductService.class.getMethod("getAllProducts", Pageable.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
    }

    @Test
    void testGetProductByID_existingProduct_returnProductDTO() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));
        when(productMapper.productToProductDTO(product1)).thenReturn(productDTO1);

        ProductDTO result = productService.getProductByID(productId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Product 1");

        verify(productRepository).findById(productId);
        verify(productMapper).productToProductDTO(product1);
    }

    @Test
    void testGetProductByID_nonExistingProduct_throwNotFoundException() {
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductByID(productId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Product by ID 999")
                .extracting("status.value")
                .isEqualTo(404);

        verify(productRepository).findById(productId);
        verify(productMapper, never()).productToProductDTO(any());
    }

    @Test
    void testGetProductByID_nullId_handleCorrectly() {
        when(productRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductByID(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Product by ID null")
                .extracting("status.value")
                .isEqualTo(404);

        verify(productRepository).findById(null);
    }

    @Test
    void testGetProductByID_hasCacheableAnnotation() throws NoSuchMethodException {
        Method method = ProductService.class.getMethod("getProductByID", Long.class);

        assertTrue(method.isAnnotationPresent(Cacheable.class));

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable.cacheNames()).contains("productById");
        assertThat(cacheable.key()).isEqualTo("#id");
    }

    @Test
    void testGetProductByID_hasTransactionalReadOnlyAnnotation() throws NoSuchMethodException {
        Method method = ProductService.class.getMethod("getProductByID", Long.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
    }

    @Test
    void testGetAllProducts_hasCacheableAnnotation() throws NoSuchMethodException {
        Method method = ProductService.class.getMethod("getAllProducts", Pageable.class);
        assertTrue(method.isAnnotationPresent(Cacheable.class));
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable.cacheNames()).contains("productPage");
        assertThat(cacheable.key()).isEqualTo("'p=' + #pageable.pageNumber + '|' + 's=' + #pageable.pageSize + '|' + 'sort=' + #pageable.sort");
    }

    @Test
    void testCreateProduct_hasCacheEvictAnnotation() throws NoSuchMethodException {
        Method method = ProductService.class.getMethod("createProduct", String.class);
        assertTrue(method.isAnnotationPresent(CacheEvict.class));
        CacheEvict evict = method.getAnnotation(CacheEvict.class);
        assertThat(evict.cacheNames()).contains("productPage");
        assertTrue(evict.allEntries());
    }

    @Test
    void testGetProductByID_multipleCallsWithSameId_shouldBeCacheable() {
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));
        when(productMapper.productToProductDTO(product1)).thenReturn(productDTO1);

        ProductDTO result1 = productService.getProductByID(productId);
        ProductDTO result2 = productService.getProductByID(productId);

        assertThat(result1).isEqualTo(result2);
    }
}