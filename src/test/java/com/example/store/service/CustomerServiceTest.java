package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import com.example.store.support.Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer1;
    private Customer customer2;
    private CustomerDTO customerDTO1;
    private CustomerDTO customerDTO2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customer1 = Factory.buildCustomer(1L, "Customer 1");
        customer2 = Factory.buildCustomer(2L, "Customer 2");

        customerDTO1 = Factory.buildCustomerDTO(1L, "Customer 1");
        customerDTO2 = Factory.buildCustomerDTO(2L, "Customer 2");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void testGetAllCustomers_withNullName_returnAllCustomers() {
        List<Customer> customerList = List.of(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, customerList.size());

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);
        when(customerMapper.customerToCustomerDTO(customer2)).thenReturn(customerDTO2);

        Page<CustomerDTO> result = customerService.getAllCustomers(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Customer 1");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Customer 2");

        verify(customerRepository).findAll(pageable);
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
        verify(customerMapper, times(2)).customerToCustomerDTO(any(Customer.class));
    }

    @Test
    void testGetAllCustomers_withEmptyName_returnAllCustomers() {
        List<Customer> customerList = List.of(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, customerList.size());

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);
        when(customerMapper.customerToCustomerDTO(customer2)).thenReturn(customerDTO2);

        Page<CustomerDTO> result = customerService.getAllCustomers("", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(customerRepository).findAll(pageable);
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllCustomers_withBlankName_returnAllCustomers() {
        List<Customer> customerList = List.of(customer1, customer2);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, customerList.size());

        when(customerRepository.findAll(pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);
        when(customerMapper.customerToCustomerDTO(customer2)).thenReturn(customerDTO2);

        Page<CustomerDTO> result = customerService.getAllCustomers("   ", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(customerRepository).findAll(pageable);
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString(), any(Pageable.class));
    }

    @Test
    void testGetAllCustomers_withValidName_returnFilteredCustomers() {
        String searchName = "Customer 1";
        List<Customer> customerList = List.of(customer1);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, 1);

        when(customerRepository.findByNameContainingIgnoreCase(searchName, pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);

        Page<CustomerDTO> result = customerService.getAllCustomers(searchName, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Customer 1");

        verify(customerRepository).findByNameContainingIgnoreCase(searchName, pageable);
        verify(customerRepository, never()).findAll(any(Pageable.class));
        verify(customerMapper).customerToCustomerDTO(customer1);
    }

    @Test
    void testGetAllCustomers_withNameWithSpaces_trimAndSearch() {
        String searchName = "    Customer 1    ";
        String trimmedName = "Customer 1";
        List<Customer> customerList = List.of(customer1);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, 1);

        when(customerRepository.findByNameContainingIgnoreCase(trimmedName, pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);

        Page<CustomerDTO> result = customerService.getAllCustomers(searchName, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(customerRepository).findByNameContainingIgnoreCase(trimmedName, pageable);
        verify(customerRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void testGetAllCustomers_withEmptyResult_returnEmptyPage() {
        Page<Customer> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(customerRepository.findAll(pageable)).thenReturn(emptyPage);
        Page<CustomerDTO> result = customerService.getAllCustomers(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(customerRepository).findAll(pageable);
        verify(customerMapper, never()).customerToCustomerDTO(any(Customer.class));
    }

    @Test
    void testGetAllCustomers_hasCacheableAnnotation() throws NoSuchMethodException {
        Method method = CustomerService.class.getMethod("getAllCustomers", String.class, Pageable.class);

        assertTrue(method.isAnnotationPresent(Cacheable.class));

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable.cacheNames()).contains("customersPage");
        assertThat(cacheable.key()).isEqualTo("'name=' + (#name == null ? '' : #name.trim()) + '|' + 'p=' + #pageable.pageNumber + '|' + 's=' + #pageable.pageSize + '|' + 'sort=' + #pageable.sort");
    }


    @Test
    void testCreateCustomer_withValidName_returnCreatedCustomer() {
        String customerName = "New Customer";
        Customer savedCustomer = Customer.builder()
                .id(3L)
                .name(customerName)
                .orders(List.of())
                .build();

        CustomerDTO expectedDTO = new CustomerDTO();
        expectedDTO.setId(3L);
        expectedDTO.setName(customerName);

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.customerToCustomerDTO(savedCustomer)).thenReturn(expectedDTO);

        CustomerDTO result = customerService.createCustomer(customerName);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo(customerName);

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        Customer capturedCustomer = customerCaptor.getValue();
        assertThat(capturedCustomer.getName()).isEqualTo(customerName);
        assertThat(capturedCustomer.getOrders()).isEmpty();

        verify(customerMapper).customerToCustomerDTO(savedCustomer);
    }

    @Test
    void testCreateCustomer_hasCacheEvictAnnotation() throws NoSuchMethodException {
        Method method = CustomerService.class.getMethod("createCustomer", String.class);

        assertTrue(method.isAnnotationPresent(CacheEvict.class));

        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        assertThat(cacheEvict.cacheNames()).contains("customersPage");
        assertTrue(cacheEvict.allEntries());
    }

    @Test
    void testGetAllCustomers_multipleCallsWithSameName_shouldUseSameCache() {
        String searchName = "Test";
        List<Customer> customerList = List.of(customer1);
        Page<Customer> customerPage = new PageImpl<>(customerList, pageable, 1);

        when(customerRepository.findByNameContainingIgnoreCase(searchName, pageable)).thenReturn(customerPage);
        when(customerMapper.customerToCustomerDTO(customer1)).thenReturn(customerDTO1);

        Page<CustomerDTO> result1 = customerService.getAllCustomers(searchName, pageable);
        Page<CustomerDTO> result2 = customerService.getAllCustomers(searchName, pageable);

        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void testCreateCustomer_shouldEvictCache() {
        String customerName = "Cache Test Customer";
        Customer savedCustomer = Customer.builder()
                .id(9L)
                .name(customerName)
                .orders(List.of())
                .build();

        CustomerDTO expectedDTO = new CustomerDTO();
        expectedDTO.setId(9L);
        expectedDTO.setName(customerName);

        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);
        when(customerMapper.customerToCustomerDTO(savedCustomer)).thenReturn(expectedDTO);

        CustomerDTO result = customerService.createCustomer(customerName);

        assertThat(result).isNotNull();
    }
}