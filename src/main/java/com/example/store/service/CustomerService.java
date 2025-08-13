package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllCustomers(String name, Pageable pageable) {
        String trimmedName = name.trim();
        Page<Customer> customers = StringUtils.isBlank(trimmedName) ?
                customerRepository.findAll(pageable) : customerRepository.findByNameContainingIgnoreCase(trimmedName, pageable);
        return customers.map(customerMapper::customerToCustomerDTO);
    }

    public CustomerDTO createCustomer(Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
