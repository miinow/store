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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(orderMapper::orderToOrderDTO);
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found Customer by ID " + request.customerId()));

        List<Product> products = productRepository.findAllById(request.productIds());

        Set<Long> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        List<Long> missing = request.productIds().stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found Product IDs: " + missing);
        }

        Order order = Order.builder()
                .description(request.description())
                .customer(customer)
                .products(products)
                .build();

        return orderMapper.orderToOrderDTO(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderByID(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not Found Order by ID " + id));
        return orderMapper.orderToOrderDTO(order);
    }
}
