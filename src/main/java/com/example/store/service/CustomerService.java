package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "customersPage",
            key = "'name=' + (#name == null ? '' : #name.trim()) + '|' + 'p=' + #pageable.pageNumber + '|' + 's=' + #pageable.pageSize + '|' + 'sort=' + #pageable.sort")
    public Page<CustomerDTO> getAllCustomers(String name, Pageable pageable) {
        String trimmedName = name != null ? name.trim() : null;
        Page<Customer> customers = StringUtils.isBlank(trimmedName) ?
                customerRepository.findAll(pageable) : customerRepository.findByNameContainingIgnoreCase(trimmedName, pageable);
        return customers.map(customerMapper::customerToCustomerDTO);
    }

    @CacheEvict(cacheNames = "customersPage", allEntries = true)
    public CustomerDTO createCustomer(String name) {
        Customer customer = Customer.builder().name(name).orders(List.of()).build();
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
