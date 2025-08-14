package com.example.store.controller;

import com.example.store.dto.CustomerDTO;
import com.example.store.dto.request.CreateCustomerRequest;
import com.example.store.mapper.CustomerMapper;
import com.example.store.service.CustomerService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@ComponentScan(basePackageClasses = CustomerMapper.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void testCreateCustomer_validRequest_returnCreatedCustomer() throws Exception {
        String customerName = "New Customer";
        CreateCustomerRequest request = Factory.buildCreateCustomerRequest(customerName);
        CustomerDTO customerDTO = Factory.buildCustomerDTO(100L, customerName);
        when(customerService.createCustomer(customerName)).thenReturn(customerDTO);


        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.name").value("New Customer"));
    }

    @Test
    void testCreateCustomer_emptyName_returnBadRequest() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest("");

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateCustomer_nullName_returnBadRequest() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(null);

        mockMvc.perform(post("/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCustomers_withoutNameFilter_returnAll() throws Exception {
        List<CustomerDTO> customerDTOList = Factory.buildCustomerDtoList();
        Pageable pageable = PageRequest.of(0, 20);
        Page<CustomerDTO> customerDTOPage = new PageImpl<>(customerDTOList, pageable, customerDTOList.size());

        when(customerService.getAllCustomers(any(), any(Pageable.class))).thenReturn(customerDTOPage);

        mockMvc.perform(get("/customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("First Customer"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Second Customer"));
    }

    @Test
    void testGetAllCustomers_withNameFilter_returnFilteredCustomers() throws Exception {
        List<CustomerDTO> customerDTOList = Factory.buildCustomerDtoList();
        Pageable pageable = PageRequest.of(0, 20);

        Page<CustomerDTO> customerDTOPage = new PageImpl<>(customerDTOList, pageable, customerDTOList.size());

        when(customerService.getAllCustomers(anyString(), any(Pageable.class))).thenReturn(customerDTOPage);

        mockMvc.perform(get("/customer").param("name", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("First Customer"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Second Customer"));
    }
}
