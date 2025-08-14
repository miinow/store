package com.example.store.service;

import com.example.store.dto.OrderDTO;
import com.example.store.dto.request.CreateOrderRequest;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product product1;
    private Product product2;
    private Order order;
    private OrderDTO orderDTO;
    private CreateOrderRequest createOrderRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customer = Factory.buildCustomer(1L, "Customer 1");
        product1 = Factory.buildProduct(1L, "Product 1");
        product2 = Factory.buildProduct(2L, "Product 2");

        order = Factory.buildOrder(1L, "Order 1");

        orderDTO = Factory.buildOrderDTO(1L, "Order 1");

        createOrderRequest = Factory.buildCreateOrderRequest();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void testGetAllOrders_returnPageOfOrderDTOs() {
        List<Order> orderList = List.of(order);
        Page<Order> orderPage = new PageImpl<>(orderList, pageable, orderList.size());

        when(orderRepository.findAll(pageable)).thenReturn(orderPage);
        when(orderMapper.orderToOrderDTO(order)).thenReturn(orderDTO);

        Page<OrderDTO> result = orderService.getAllOrders(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDescription()).isEqualTo("Order 1");
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(orderRepository).findAll(pageable);
        verify(orderMapper, times(1)).orderToOrderDTO(any(Order.class));
    }

    @Test
    void testGetAllOrders_emptyResult_returnEmptyPage() {
        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(orderRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<OrderDTO> result = orderService.getAllOrders(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(orderRepository).findAll(pageable);
        verify(orderMapper, never()).orderToOrderDTO(any(Order.class));
    }

    @Test
    void testGetAllOrders_withPagination_returnCorrectPage() {
        Pageable customPageable = PageRequest.of(2, 5);
        List<Order> orderList = List.of(order);
        Page<Order> orderPage = new PageImpl<>(orderList, customPageable, 11);

        when(orderRepository.findAll(customPageable)).thenReturn(orderPage);
        when(orderMapper.orderToOrderDTO(order)).thenReturn(orderDTO);

        Page<OrderDTO> result = orderService.getAllOrders(customPageable);

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(11);
        assertThat(result.getTotalPages()).isEqualTo(3);

        verify(orderRepository).findAll(customPageable);
    }

    @Test
    void testGetAllOrders_hasTransactionalReadOnlyAnnotation() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("getAllOrders", Pageable.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
    }

    @Test
    void testCreateOrder_validRequest_returnCreatedOrder() {
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(product1, product2));

        Order savedOrder = Order.builder()
                .id(10L)
                .description("New Order")
                .customer(customer)
                .products(List.of(product1, product2))
                .build();

        OrderDTO expectedDTO = new OrderDTO();
        expectedDTO.setId(10L);
        expectedDTO.setDescription("New Order");

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.orderToOrderDTO(savedOrder)).thenReturn(expectedDTO);

        OrderDTO result = orderService.createOrder(createOrderRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("New Order");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getDescription()).isEqualTo("New Order");
        assertThat(capturedOrder.getCustomer()).isEqualTo(customer);
        assertThat(capturedOrder.getProducts()).containsExactly(product1, product2);

        verify(customerRepository).findById(100L);
        verify(productRepository).findAllById(List.of(1L, 2L));
        verify(orderMapper).orderToOrderDTO(savedOrder);
    }

    @Test
    void testCreateOrder_customerNotFound_throwNotFoundException() {
        when(customerRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Customer by ID 100")
                .extracting("status.value")
                .isEqualTo(404);

        verify(customerRepository).findById(100L);
        verify(productRepository, never()).findAllById(anyList());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_someProductsNotFound_throwNotFoundException() {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "New Order", 1L, List.of(1L, 2L, 99L)
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(1L, 2L, 99L))).thenReturn(List.of(product1, product2));

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Product IDs: [99]")
                .extracting("status.value")
                .isEqualTo(404);

        verify(customerRepository).findById(1L);
        verify(productRepository).findAllById(List.of(1L, 2L, 99L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testCreateOrder_allProductsNotFound_throwNotFoundException() {
        CreateOrderRequest createOrderRequest = new CreateOrderRequest(
                "Order 1", 100L, List.of(10L, 20L)
        );
        when(customerRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllById(List.of(10L, 20L)))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> orderService.createOrder(createOrderRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Product IDs: [10, 20]")
                .extracting("status.value")
                .isEqualTo(404);

        verify(customerRepository).findById(100L);
        verify(productRepository).findAllById(List.of(10L, 20L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testGetOrderByID_existingOrder_returnOrderDTO() {
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.orderToOrderDTO(order)).thenReturn(orderDTO);

        OrderDTO result = orderService.getOrderByID(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDescription()).isEqualTo("Order 1");

        verify(orderRepository).findById(orderId);
        verify(orderMapper).orderToOrderDTO(order);
    }

    @Test
    void testGetOrderByID_nonExistingOrder_throwNotFoundException() {
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByID(orderId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Order by ID 999")
                .extracting("status.value")
                .isEqualTo(404);

        verify(orderRepository).findById(orderId);
        verify(orderMapper, never()).orderToOrderDTO(any());
    }

    @Test
    void testGetOrderByID_nullId_handleCorrectly() {
        when(orderRepository.findById(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByID(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not Found Order by ID null")
                .extracting("status.value")
                .isEqualTo(404);

        verify(orderRepository).findById(null);
    }

    @Test
    void testGetOrderByID_hasCacheableAnnotation() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("getOrderByID", Long.class);

        assertTrue(method.isAnnotationPresent(Cacheable.class));

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable.cacheNames()).contains("orderById");
        assertThat(cacheable.key()).isEqualTo("#id");
    }

    @Test
    void testGetOrderByID_hasTransactionalReadOnlyAnnotation() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("getOrderByID", Long.class);

        assertTrue(method.isAnnotationPresent(Transactional.class));

        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional.readOnly());
    }

    @Test
    void testGetAllOrders_hasCacheableAnnotation() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("getAllOrders", Pageable.class);
        assertTrue(method.isAnnotationPresent(Cacheable.class));
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        assertThat(cacheable.cacheNames()).contains("ordersPage");
        assertThat(cacheable.key()).isEqualTo("'p=' + #pageable.pageNumber + '|' + 's=' + #pageable.pageSize + '|' + 'sort=' + #pageable.sort");
    }

    @Test
    void testCreateOrder_hasCacheEvictAnnotation() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("createOrder", CreateOrderRequest.class);
        assertTrue(method.isAnnotationPresent(CacheEvict.class));
        CacheEvict evict = method.getAnnotation(CacheEvict.class);
        assertThat(evict.cacheNames()).contains("ordersPage");
        assertTrue(evict.allEntries());
    }
}