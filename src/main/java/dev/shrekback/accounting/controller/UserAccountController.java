package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.dto.*;
import dev.shrekback.accounting.model.CartItem;
import dev.shrekback.accounting.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j

public class UserAccountController {

    final UserAccountService userAccountService;

    @PostMapping(value = "/payment/capture", consumes = "application/json")
    public ResponseEntity<String> capturePayment(@RequestBody PayPalCaptureDto dto) {
        log.info("Attempting to capture PayPal order: {}", dto.getOrderId());

        if (dto.getOrderId() == null || dto.getOrderId().isBlank()) {
            return ResponseEntity.badRequest().body("Missing orderId");
        }

        boolean success = userAccountService.captureOrder(dto.getOrderId());

        return success
                ? ResponseEntity.ok("Payment captured successfully")
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to capture payment");
    }

    @PutMapping("/payment-method/{login}")
    public UserDto updatePaymentMethod(@PathVariable String login,@RequestBody PaymentMethodDto paymentMethodDto) {
        return userAccountService.updatePaymentMethod(login,paymentMethodDto);
    }

    // Login typically done via POST to /login (can be outside users resource)
    @PostMapping("/login")
    public UserDto login(@RequestHeader("Authorization") String token) {
        token = token.split(" ")[1];
        System.out.println("con login");
        String credentials = new String(Base64.getDecoder().decode(token));
        return userAccountService.getUser(credentials.split(":")[0]);
    }

    // Register new user
    @PostMapping("/register")
    public UserDto register(@RequestBody UserRegisterDto userRegisterDto) {
        System.out.println("con register");
        return userAccountService.register(userRegisterDto);
    }

    @PutMapping("/{username}/wishList/{productId}")
    public UserDto addWishList(@PathVariable String username, @PathVariable String productId) {
        return userAccountService.changeWishList(username, productId, true);
    }

    // Remove role from user
    @DeleteMapping("/{username}/wishList/{productId}")
    public UserDto deleteWishList(@PathVariable String username, @PathVariable String productId) {
        return userAccountService.changeWishList(username, productId, false);
    }

    // Add role to user
    @PutMapping("/{username}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addRole(@PathVariable String username, @PathVariable String role) {
        userAccountService.changeRolesList(username, role, true);
    }

    // Remove role from user
    @DeleteMapping("/{username}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable String username, @PathVariable String role) {
        userAccountService.changeRolesList(username, role, false);
    }

    // Change password for authenticated user
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Principal principal, @RequestHeader("X-Password") String newPassword) {
        userAccountService.changePassword(principal.getName(), newPassword);
    }

    // Request password recovery link by username
    @PostMapping("/password/recovery/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recoveryPasswordLink(@PathVariable String username) {
        userAccountService.recoveryPasswordLink(username);
    }

    // Reset password by recovery token
    @PutMapping("/password/recovery/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recoveryPassword(@PathVariable String token, @RequestHeader("X-Password") String newPassword) {
        userAccountService.recoveryPassword(token, newPassword);
    }

    // Get all users
    @GetMapping
    public List<UserDto> getAllUsers() {
        return userAccountService.getAllUsers();
    }


    // Remove a user
    @DeleteMapping("/{login}")
    public UserDto removeUser(@PathVariable String login) {
        return userAccountService.removeUser(login);
    }

    // Update authenticated user's data
    @PutMapping
    public UserDto updateUser(Principal principal, @RequestBody UserEditDto userEditDto) {
        return userAccountService.updateUser(principal.getName(), userEditDto);
    }

    @PostMapping("/address/{login}")
    public UserDto updateAddress(@PathVariable String login, @RequestBody AddressDto addressDto) {
        return userAccountService.updateAddress(login, addressDto);
    }


    @PutMapping("/{username}/cartList")
    public UserDto addCartList(@PathVariable String username, @RequestBody CartItem cartItem) {
        return userAccountService.changeCartList(username, cartItem, true);
    }

    @DeleteMapping("/{username}/cartList")
    public UserDto deleteCartList(@PathVariable String username, @RequestBody CartItem cartItem) {
        return userAccountService.changeCartList(username, cartItem, false);
    }

    @PutMapping("/{username}/cartList/{productId}/update/{isAdd}")
    public UserDto updateCartList(@PathVariable String username, @PathVariable String productId,@PathVariable boolean isAdd) {
        return userAccountService.updateCartList(username, productId, isAdd);
    }
}
