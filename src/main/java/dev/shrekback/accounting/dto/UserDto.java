package dev.shrekback.accounting.dto;

import dev.shrekback.accounting.model.Order;
import dev.shrekback.accounting.model.PaymentMethod;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
	String firstName;
	String lastName;
	String login;
//	AddressDto address;
	@Singular
	Set<String> roles;
	AddressDto address;
	Cart cart;
	PaymentMethod paymentMethod; // ✅ Single method

	Set<String> wishList;

	List<Order> orders;
}
