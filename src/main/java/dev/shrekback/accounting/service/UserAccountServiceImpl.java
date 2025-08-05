package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dao.UserTokenRepository;
import dev.shrekback.accounting.dto.*;
import dev.shrekback.accounting.dto.exceptions.InvalidEmailExeption;
import dev.shrekback.accounting.dto.exceptions.TokenExpiredExeption;
import dev.shrekback.accounting.dto.exceptions.UserExistsException;
import dev.shrekback.accounting.dto.exceptions.UserNotFoundException;
import dev.shrekback.accounting.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService, CommandLineRunner {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountRepository userAccountRepository;
    private final UserTokenRepository userTokenRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.secret}")
    private String secret;

    @Value("${paypal.base-url}")
    private String baseUrl;

    // PayPal Integration

    @Override
    public void changePassword(String login, String newPassword) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        userAccount.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(userAccount);
    }

    @Override
    public void changeRolesList(String login, String role, boolean isAddRole) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        boolean updated = isAddRole ? userAccount.addRole(role) : userAccount.removeRole(role);
        if (updated) userAccountRepository.save(userAccount);
    }

    private void checkEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!Pattern.matches(emailRegex, email)) {
            throw new InvalidEmailExeption();
        }
    }

    @Override
    public List<UserDto> getAllUsers() {
        return StreamSupport.stream(userAccountRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UserDto.class))
                .toList();
    }

    @Override
    public UserDto getUser(String login) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public void recoveryPassword(String token, String newPassword) {
        UserToken userToken = userTokenRepository.findById(token)
                .orElseThrow(TokenExpiredExeption::new);

        if (LocalDateTime.now().isAfter(userToken.getExpirationDate())) {
            userTokenRepository.delete(userToken);
            throw new TokenExpiredExeption();
        }

        changePassword(userToken.getLogin(), newPassword);
    }

    @Override
    public void recoveryPasswordLink(String login) {
        userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        UserToken userToken = new UserToken(login);
        userTokenRepository.save(userToken);
        // Optionally sendRecoveryEmail(login, userToken.getToken());
    }

    @Override
    public UserDto register(UserRegisterDto userRegisterDto) {
        if (userAccountRepository.existsById(userRegisterDto.getLogin())) {
            throw new UserExistsException();
        }
        checkEmail(userRegisterDto.getLogin());

        UserAccount userAccount = modelMapper.map(userRegisterDto, UserAccount.class);
        userAccount.setPassword(passwordEncoder.encode(userRegisterDto.getPassword()));
        userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto removeUser(String login) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);
        userAccountRepository.delete(userAccount);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public void run(String... args) {
        if (!userAccountRepository.existsById("admin")) {
            String password = passwordEncoder.encode("admin");
            UserAccount userAccount = new UserAccount("admin", password, "", "");
            userAccount.addRole("ADMINISTRATOR");
            userAccount.addRole("MODERATOR");
            userAccountRepository.save(userAccount);
        }
    }

    @Override
    public UserDto updateUser(String login, UserEditDto userEditDto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        if (userEditDto.getFirstName() != null) {
            userAccount.setFirstName(userEditDto.getFirstName());
        }
        if (userEditDto.getLastName() != null) {
            userAccount.setLastName(userEditDto.getLastName());
        }

        userAccountRepository.save(userAccount);
        return modelMapper.map(userAccount, UserDto.class);
    }

    public UserDto updatePaymentMethod(String login, PaymentMethodDto dto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        PaymentMethod payment = new PaymentMethod();
        payment.setCardtype(dto.getCardtype());                    // map cardtype to type
        payment.setCardname(dto.getCardname());                // assume name on card = provider
        payment.setCardno(maskCardNumber(dto.getCardno())); // mask function
        payment.setExdate(dto.getExdate());
        payment.setCvv(dto.getCvv());

        userAccount.setPaymentMethod(payment);
        userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    private String maskCardNumber(String cardno) {
        if (cardno == null || cardno.length() < 4) return "****";
        return "**** **** **** " + cardno.substring(cardno.length() - 4);
    }


    @Override
    public UserDto updateAddress(String login, AddressDto dto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        Address address = new Address();
        address.setFullName(dto.getFullName());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setCountry(dto.getCountry());
        address.setPhone(dto.getPhone());

        userAccount.setAddress(address);
        userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto changeWishList(String login, String productId, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        boolean updated = isAdd ? userAccount.addWishList(productId) : userAccount.removeWishList(productId);
        if (updated) userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto changeCartList(String login, Item item, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        boolean updated = isAdd ?
                userAccount.getCart().addCartEntry(item) :
                userAccount.getCart().removeCartEntry(item);

        if (updated) userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto updateCartList(String login, String cartItemId, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        boolean updated = userAccount.getCart().updateCartList(cartItemId, isAdd);
        if (updated) userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }

//    @Override
//    public List<OrderDto> createOrder(String login, OrderRequestDto request, boolean isAdd) {
//        UserAccount userAccount = userAccountRepository.findById(login)
//                .orElseThrow(UserNotFoundException::new);
//
//        if (!isAdd || request.getOrderItems() == null) {
//            return List.of();
//        }
//
//        List<OrderDto> createdOrders = new ArrayList<>();
//
//        for (OrderItem item : request.getOrderItems()) {
//            Post post = postRepository.findById(item.getProductId())
//                    .orElseThrow(PostNotFoundException::new);
//
//            Adjustment adjustment = new Adjustment(1, true, login);
//            post.adjust(adjustment);
//            postRepository.save(post);
//
//            Order order = modelMapper.map(request, Order.class);
//            orderRepository.save(order);
//            userAccount.addOrder(order);
//
//            createdOrders.add(modelMapper.map(order, OrderDto.class));
//        }
//
//        userAccountRepository.save(userAccount);
//        return userAccount.getOrders().stream()
//                .map(o -> modelMapper.map(o, OrderDto.class))
//                .toList();
//    }

//    @Override
//    public boolean captureOrder(String orderId) {
//        return  true;
//

    /// /        try {
    /// /            String accessToken = getAccessToken();
    /// /            if (accessToken == null) {
    /// /                log.error("Failed to retrieve PayPal access token");
    /// /                return false;
    /// /            }
    /// /
    /// /            String captureUrl = baseUrl + "/v2/checkout/orders/" + orderId + "/capture";
    /// /            HttpHeaders headers = new HttpHeaders();
    /// /            headers.setBearerAuth(accessToken);
    /// /            headers.setContentType(MediaType.APPLICATION_JSON);
    /// /
    /// /            HttpEntity<String> request = new HttpEntity<>(null, headers);
    /// /            ResponseEntity<String> response = restTemplate.postForEntity(captureUrl, request, String.class);
    /// /
    /// /            if (response.getStatusCode().is2xxSuccessful()) {
    /// /                log.info("Order captured successfully: {}", response.getBody());
    /// /                return true;
    /// /            } else {
    /// /                log.error("Failed to capture order: {}", response.getBody());
    /// /                return false;
    /// /            }
    /// /        } catch (Exception e) {
    /// /            log.error("Exception while capturing order: ", e);
    /// /            return false;
    /// /        }
//    }
    private String getAccessToken() {
        try {
            String tokenUrl = baseUrl + "/v1/oauth2/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(clientId, secret, StandardCharsets.UTF_8);

            HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

            ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }

            log.error("Failed to get PayPal token: {}", response.getBody());
        } catch (Exception e) {
            log.error("Exception while getting PayPal token: ", e);
        }
        return null;
    }

}
