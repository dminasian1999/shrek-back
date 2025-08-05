package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dto.OrderDto;

import java.util.List;

public interface OrderService {

     OrderDto checkOut(OrderDto orderDto);
     List<OrderDto> getOrdersByUser(String userId);
     OrderDto getOrdersById(String orderId);
}
