package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dto.Cart;
import dev.shrekback.accounting.dto.OrderDto;
import dev.shrekback.accounting.dto.UserDto;
import dev.shrekback.accounting.dto.exceptions.UserNotFoundException;
import dev.shrekback.accounting.model.Item;
import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.OrderStatus;
import dev.shrekback.accounting.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.StreamSupport;


@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

     final ModelMapper modelMapper;
     final OrderRepository orderRepository;
    final UserAccountRepository  userAccountRepository;
    final UserAccountService  userAccountService;


    @Override
    public List<OrderDto> getAllOrders() {
        return StreamSupport.stream(orderRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, OrderDto.class))
                .toList();
    }

    @Override
    public OrderDto checkOut(OrderDto orderDto) {
//        orderDto.getOrderItems().stream().forEach(item -> {
//            Item it = new Item(orderDto.getOrderId(),item.getProduct(),item.getProduct().gw)
//
//            userAccountService.changeCartList(orderDto.getUserId(),)
//            UserAccount userAccount = userAccountRepository.findById(login)
//                    .orElseThrow(UserNotFoundException::new);
//
//            boolean updated = isAdd ?
//                    userAccount.getCart().addCartEntry(item) :
//                    userAccount.getCart().removeCartEntry(item);
//
//            if (updated) userAccountRepository.save(userAccount);
//
//            return modelMapper.map(userAccount, UserDto.class);
//        });
//        Item it = new Item(orderDto.getOrderId(),)
//        userAccountService.changeCartList(orderDto.getUserId(),)
//        changeCartList
        Cart c= new Cart();
        UserAccount userAccount = userAccountRepository.findById(orderDto.getUserId())
                .orElseThrow(UserNotFoundException::new);
        userAccount.setCart(c);
        userAccountRepository.save(userAccount);
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
