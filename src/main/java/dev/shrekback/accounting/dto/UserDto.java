package dev.shrekback.accounting.dto;

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
	PaymentMethodDto paymentMethod; // ✅ Single method

	Set<String> wishList;

	List<OrderDto> orders;
}
