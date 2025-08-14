package com.example.store.support;

import com.example.store.dto.*;
import com.example.store.dto.request.CreateCustomerRequest;
import com.example.store.dto.request.CreateOrderRequest;
import com.example.store.dto.request.CreateProductRequest;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;

import java.util.List;

public final class Factory {

    public static Customer buildCustomer(Long id, String name) {
        return Customer.builder()
                .id(id)
                .name(name)
                .build();
    }

    public static Order buildOrder(Long id, String description) {
        return Order.builder()
                .id(id)
                .description(description)
                .customer(buildCustomer(1L, "Customer 1"))
                .products(List.of(
                        buildProduct(1L, "Product 1"),
                        buildProduct(2L, "Product 2")
                ))
                .build();
    }

    public static Product buildProduct(Long id, String description) {
        return Product.builder()
                .id(id)
                .description(description)
                .orders(List.of())
                .build();
    }

    public static CreateCustomerRequest buildCreateCustomerRequest(String name) {
        return new CreateCustomerRequest(name);
    }

    public static CustomerDTO buildCustomerDTO(Long id, String name) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }

    public static List<CustomerDTO> buildCustomerDtoList() {
        return List.of(
                buildCustomerDTO(1L, "First Customer"),
                buildCustomerDTO(2L, "Second Customer")
        );
    }

    public static OrderCustomerDTO buildOrderCustomerDTO(Long id, String name) {
        OrderCustomerDTO customer = new OrderCustomerDTO();
        customer.setId(id);
        customer.setName(name);
        return customer;
    }

    public static List<OrderProductDTO> buildOrderProductDTOList() {
        return List.of(
                buildOrderProductDTO(1L, "Product 1"),
                buildOrderProductDTO(2L, "Product 2")
        );
    }

    public static OrderProductDTO buildOrderProductDTO(Long id, String description) {
        OrderProductDTO product = new OrderProductDTO();
        product.setId(id);
        product.setDescription(description);
        return product;
    }

    public static OrderDTO buildOrderDTO(Long id, String description) {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setId(id);
        orderDTO.setDescription(description);
        orderDTO.setCustomer(buildOrderCustomerDTO(100L, "Order Customer"));
        orderDTO.setProducts(buildOrderProductDTOList());
        return orderDTO;
    }

    public static CreateOrderRequest buildCreateOrderRequest() {
        return new CreateOrderRequest("New Order", 100L, List.of(1L, 2L));
    }

    public static ProductDTO buildProductDTO(Long id, String description) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(id);
        productDTO.setDescription(description);
        productDTO.setOrders(List.of(1L, 2L));
        return productDTO;
    }

    public static CreateProductRequest buildCreateProductRequest(String description) {
        return new CreateProductRequest(description);
    }

    public static List<ProductDTO> buildProductDTOList() {
        return List.of(
                buildProductDTO(1L, "Product 1"),
                buildProductDTO(2L, "Product 2")
        );
    }
}
