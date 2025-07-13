package dev.shrekback.accounting.service;

import dev.shrekback.accounting.dto.*;
import dev.shrekback.accounting.model.CartItem;
import dev.shrekback.accounting.model.PaymentMethod;

import java.util.List;

public interface UserAccountService {

	UserDto updatePaymentMethod(String login, PaymentMethodDto paymentMethod);

	UserDto updateAddress(String login, AddressDto addressDto);

	void changePassword(String login, String newPassword);

	void changeRolesList(String login, String role, boolean isAddRole);

	List<UserDto> getAllUsers();

	UserDto getUser(String login);

	void recoveryPassword(String token, String newPassword);
	void recoveryPasswordLink(String login);

	UserDto register(UserRegisterDto userRegisterDto);

	UserDto removeUser(String login);

	UserDto updateUser(String login, UserEditDto userEditDto);

	UserDto changeWishList(String login, String productId, boolean isAdd);

	UserDto changeCartList(String login, CartItem cartItem, boolean isAdd);

	UserDto updateCartList(String login, String cartItemId, boolean isAdd);


}
