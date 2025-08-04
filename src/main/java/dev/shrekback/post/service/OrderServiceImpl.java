//package dev.shrekback.post.service;
//
//import dev.shrekback.accounting.dao.UserAccountRepository;
//import dev.shrekback.accounting.dto.Cart;
//import dev.shrekback.accounting.model.CartItem;
//import dev.shrekback.accounting.model.UserAccount;
//import dev.shrekback.post.dao.OrdersRepository;
//import dev.shrekback.post.dto.OrderDto;
//import dev.shrekback.post.dto.exceptions.PostNotFoundException;
//import dev.shrekback.post.model.Order;
//import dev.shrekback.post.model.OrderItem;
//import lombok.RequiredArgsConstructor;
//import org.modelmapper.ModelMapper;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Set;
//import java.util.UUID;
//import java.util.stream.Collector;
//import java.util.stream.Collectors;
//
//@RequiredArgsConstructor
//@Service
//public class OrderServiceImpl {
//
//    private final UserAccountRepository userRepo;
//    private final OrdersRepository orderRepo;
//    private final ModelMapper modelMapper;
//
//    public OrderDto createOrder(String userId) {
//        UserAccount user = userRepo.findById(userId).orElseThrow(PostNotFoundException::new);
//
//        Set<CartItem> cartItems = user.getCart().getItems();
//
//        if (cartItems.isEmpty()) throw new IllegalStateException("Cart is empty");
//
//        Order order = new Order();
//        order.setId(UUID.randomUUID().toString());
//        order.setUserId(userId);
//        order.setOrderId(UUID.randomUUID().toString()); // Generate unique orderId
//        order.setDateCreated(LocalDateTime.now());
//        order.setStatus(Order.OrderStatus.PENDING); // Use enum
//        order.setPaymentMethod(Order.PaymentMethod.STRIPE.name()); // Default to Stripe
//        order.setPaymentStatus(Order.PaymentStatus.PENDING.name()); // Default to PENDING
//
//        Set<OrderItem> orderItems = cartItems.stream()
//                .map(item -> {
//                    OrderItem oi = new OrderItem();
//                    oi.setProductId(item.getProductId());
//                    oi.setQuantity(item.getQuantity());
//                    oi.setUnitPrice(item.getProduct().getPrice());
//                    oi.setProductName(item.getProduct().getName()); // Assuming CartItem has this
//                    return oi;  // important: return the new OrderItem
//                })
//                .collect(Collectors.toSet());
//
//
//        order.setOrderItems(orderItems);
//        order.setTotalAmount(orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum());
//
//        // Save order
//        orderRepo.save(order);
//
//        // Clear cart
//        user.setCart(new Cart());
//        userRepo.save(user);
//
//        return modelMapper.map(order, OrderDto.class);
//    }
//}
