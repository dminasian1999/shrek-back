package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.dto.exceptions.UserNotFoundException;
import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

     final ModelMapper modelMapper;
     final OrderRepository orderRepository;


    @Override
    public OrderDto checkOut(OrderDto orderDto) {
        Order order = modelMapper.map(orderDto, Order.class);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        return modelMapper.map(order, OrderDto.class);
    }

    @Override
    public List<OrderDto> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> modelMapper.map(order, OrderDto.class))
                .toList();
    }

    @Override
    public OrderDto getOrdersById(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(UserNotFoundException::new);
        return modelMapper.map(order, OrderDto.class);
    }
}
