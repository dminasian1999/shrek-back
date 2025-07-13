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
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService, CommandLineRunner {

    final ModelMapper modelMapper;
    final PasswordEncoder passwordEncoder;
    final UserAccountRepository userAccountRepository;
    final UserTokenRepository userTokenRepository;

    @Override
    public void changePassword(String login, String newPassword) {
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        userAccount.setPassword(passwordEncoder.encode(newPassword));
        userAccountRepository.save(userAccount);
    }

    @Override
    public void changeRolesList(String login, String role, boolean isAddRole) {
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        boolean res;
        if (isAddRole) {
            res = userAccount.addRole(role);
        } else {
            res = userAccount.removeRole(role);
        }
        if (res) {
            userAccountRepository.save(userAccount);
        }
//		return modelMapper.map(userAccount, RolesDto.class);
    }

    private void checkEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        if (!matcher.matches()) {
            throw new InvalidEmailExeption();
        }

    }

    @Override
    public List<UserDto> getAllUsers() {

//		return postRepository.findByAuthorIgnoreCase(publisherName).map(p -> modelMapper.map(p, PostDto.class))
        return StreamSupport.stream(userAccountRepository.findAll().spliterator(), false)
                .map(u -> modelMapper.map(u, UserDto.class))
                .toList();
    }

    @Override
    public UserDto getUser(String login) {
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public void recoveryPassword(String token, String newPassword) {
        UserToken userToken = userTokenRepository.findById(token).orElseThrow(TokenExpiredExeption::new);
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
//		sendRecoveryEmail(login, userToken.getToken());

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
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        userAccountRepository.delete(userAccount);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public void run(String... args) throws Exception {
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
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        if (userEditDto.getFirstName() != null) {
            userAccount.setFirstName(userEditDto.getFirstName());
        }
        if (userEditDto.getLastName() != null) {
            userAccount.setLastName(userEditDto.getLastName());
        }

        userAccountRepository.save(userAccount);
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto updatePaymentMethod(String login, PaymentMethodDto paymentMethodDto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        PaymentMethod payment = new PaymentMethod();

        if (paymentMethodDto.getType() != null) {
            payment.setType(paymentMethodDto.getType());
        }
        if (paymentMethodDto.getProvider() != null) {
            payment.setProvider(paymentMethodDto.getProvider());
        }
        if (paymentMethodDto.getAccountNumberMasked() != null) {
            payment.setAccountNumberMasked(paymentMethodDto.getAccountNumberMasked());
        }
        if (paymentMethodDto.getExpiryDate() != null) {
            payment.setExpiryDate(paymentMethodDto.getExpiryDate());
        }

        userAccount.setPaymentMethod(payment);
        userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);    }

    @Override
    public UserDto updateAddress(String login, AddressDto addressDto) {
        UserAccount userAccount = userAccountRepository.findById(login)
                .orElseThrow(UserNotFoundException::new);

        Address address = new Address();

        if (addressDto.getFullName() != null) {
            address.setFullName(addressDto.getFullName());
        }
        if (addressDto.getStreet() != null) {
            address.setStreet(addressDto.getStreet());
        }
        if (addressDto.getCity() != null) {
            address.setCity(addressDto.getCity());
        }
        if (addressDto.getState() != null) {
            address.setState(addressDto.getState());
        }
        if (addressDto.getZipCode() != null) {
            address.setZipCode(addressDto.getZipCode());
        }
        if (addressDto.getCountry() != null) {
            address.setCountry(addressDto.getCountry());
        }
        if (addressDto.getPhone() != null) {
            address.setPhone(addressDto.getPhone());
        }

        userAccount.setAddress(address);
        userAccountRepository.save(userAccount);

        return modelMapper.map(userAccount, UserDto.class);
    }


    @Override
    public UserDto changeWishList(String login, String productId, boolean isAdd) {
        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        boolean res;
        if (isAdd) {
            res = userAccount.addWishList(productId);
        } else {
            res = userAccount.removeWishList(productId);
        }
        if (res) {
            userAccountRepository.save(userAccount);
        }
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto changeCartList(String login, CartItem cartItem, boolean isAdd) {

        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        boolean res;
        if (isAdd) {
            res = userAccount.getCart().addCartEntry(cartItem);
        } else {
            res = userAccount.getCart().removeCartEntry(cartItem);
        }
        if (res) {
            userAccountRepository.save(userAccount);
        }
        return modelMapper.map(userAccount, UserDto.class);
    }

    @Override
    public UserDto updateCartList(String login, String cartItemId, boolean isAdd) {

        UserAccount userAccount = userAccountRepository.findById(login).orElseThrow(UserNotFoundException::new);
        boolean res;
//        if (isAdd) {
//            res = userAccount.getCart().updateCartList(cartItemId);
//        } else {
            res = userAccount.getCart().updateCartList(cartItemId,isAdd);
//        }
        if (res) {
            userAccountRepository.save(userAccount);
        }
//        if (isAdd) {
//            res = userAccount.getCart().getItems().stream()
//                    .filter(item -> item.getCartItemId().equals(cartItemId))
//                    .findFirst()
//                    .orElse(null).increment();
//        } else {
//            res = userAccount.getCart().getItems().stream()
//                    .filter(item -> item.getCartItemId().equals(cartItemId))
//                    .findFirst()
//                    .orElse(null).decrement();
//        }
        if (res) {
            userAccountRepository.save(userAccount);
        }
        return modelMapper.map(userAccount, UserDto.class);

    }
}
