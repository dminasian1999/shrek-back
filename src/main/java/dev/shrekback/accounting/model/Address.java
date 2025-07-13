package dev.shrekback.accounting.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address  {
	String fullName;
	String street;
	String city;
	String state;
	String zipCode;
	String country;
	String phone;
}
