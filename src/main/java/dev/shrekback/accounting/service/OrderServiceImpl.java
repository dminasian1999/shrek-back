package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dao.UserTokenRepository;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.OrderStatus;
import dev.shrekback.post.dao.PostRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl  implements   OrderService{

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;
    private final UserTokenRepository userTokenRepository;
    private final PostRepository postRepository;
    private final OrderRepository orderRepository;



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
                .map(order -> modelMapper.map(order,OrderDto.class))
                .toList();
    }
}
