package dev.shrekback.security;

import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	final UserAccountRepository userAccountRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserAccount userAccount = userAccountRepository.findById(username).orElseThrow(()->new UsernameNotFoundException(username+"UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUu"));
		Collection<String> authorities = userAccount.getRoles().stream()
													.map(r -> "ROLE_"+r.name())
													.collect(Collectors.toList());
		return new User(username, userAccount.getPassword(), AuthorityUtils.createAuthorityList(authorities));
	}

}
