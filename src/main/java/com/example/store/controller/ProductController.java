package com.example.store.controller;

import com.example.store.dto.ProductDTO;
import com.example.store.dto.request.CreateProductRequest;
import com.example.store.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(@RequestBody @Valid CreateProductRequest request) {
        return productService.createProduct(request.description());
    }

    @GetMapping
    public Page<ProductDTO> getAllProducts(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return productService.getAllProducts(pageable);
    }

    @GetMapping("/{id}")
    public ProductDTO getProductByID(@PathVariable Long id) {
        return productService.getProductByID(id);
    }
}
