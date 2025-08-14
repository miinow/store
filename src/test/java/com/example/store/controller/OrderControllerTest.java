package com.example.store.controller;

import com.example.store.dto.OrderDTO;
import com.example.store.dto.request.CreateOrderRequest;
import com.example.store.mapper.OrderMapper;
import com.example.store.service.OrderService;
import com.example.store.support.Factory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ComponentScan(basePackageClasses = OrderMapper.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderDTO order1;
    private OrderDTO order2;

    @BeforeEach
    void setUp() {
        order1 = Factory.buildOrderDTO(1L, "First Order");
        order2 = Factory.buildOrderDTO(2L, "Second Order");
    }

    @Test
    void testGetAllOrders_returnPageOfOrders() throws Exception {
        List<OrderDTO> orderList = List.of(order1, order2);

        Pageable pageable = PageRequest.of(0, 50);
        Page<OrderDTO> orderPage = new PageImpl<>(orderList, pageable, orderList.size());

        when(orderService.getAllOrders(any(Pageable.class))).thenReturn(orderPage);

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].description").value("First Order"))
                .andExpect(jsonPath("$.content[0].customer.id").value(100))
                .andExpect(jsonPath("$.content[0].products[0].description").value("Product 1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].description").value("Second Order"))
                .andExpect(jsonPath("$.content[1].customer.id").value(100))
                .andExpect(jsonPath("$.content[1].products[1].description").value("Product 2"))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void testGetOrderByID_existingOrder_returnOrder() throws Exception {
        Long orderId = 1L;
        when(orderService.getOrderByID(anyLong())).thenReturn(order1);

        mockMvc.perform(get("/order/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("First Order"))
                .andExpect(jsonPath("$.customer.name").value("Order Customer"))
                .andExpect(jsonPath("$.products[0].id").value(1));
    }

    @Test
    void testGetOrderByID_nonExistingOrder_returnNotFound() throws Exception {
        Long orderId = 999L;

        when(orderService.getOrderByID(orderId)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found Order by ID " + orderId)
        );

        mockMvc.perform(get("/order/{id}", orderId)).andExpect(status().isNotFound());
    }

    @Test
    void testGetOrderByID_invalidIdFormat_returnBadRequest() throws Exception {
        mockMvc.perform(get("/order/{id}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_validRequest_returnCreatedOrder() throws Exception {
        CreateOrderRequest request = Factory.buildCreateOrderRequest();
        OrderDTO createdOrder = Factory.buildOrderDTO(1L, "First Order");

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(createdOrder);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("First Order"))
                .andExpect(jsonPath("$.customer.id").value(100))
                .andExpect(jsonPath("$.products[0].id").value(1));

    }

    @Test
    void testCreateOrder_nullCustomerId_returnBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("Order", null, List.of(1L));

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void testCreateOrder_emptyItems_returnBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("Order", 100L, List.of());

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateOrder_nullItems_returnBadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("Order", 100L, null);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}