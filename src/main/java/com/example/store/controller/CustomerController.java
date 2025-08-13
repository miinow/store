package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.request.CreateCustomerRequest;

import com.example.store.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public Page<CustomerDTO> getAllCustomers(
            @RequestParam(value="name", required = false) String name,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return customerService.getAllCustomers(name, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO createCustomer(@RequestBody @Valid CreateCustomerRequest request) {
        return customerService.createCustomer(request.name());
    }
}
