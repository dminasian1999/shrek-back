package dev.shrekback.security;

import dev.shrekback.accounting.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
@RequiredArgsConstructor
public class AuthorizationConfiguration {

    @Bean
    public SecurityFilterChain web(HttpSecurity http) throws Exception {
        http
                .httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .authorizeHttpRequests(authorize -> authorize

                                // Public endpoints
                                .requestMatchers(
                                        "/users/register",
                                        "/posts",
                                        "/posts/wishList",
                                        "/posts/receipts",
                                        "/posts/criteria/**",
                                        "/posts/type/**",
                                        "/post/{id}"
                                ).permitAll()
                                .requestMatchers(HttpMethod.GET, "/users/recovery/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/users/recovery/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/users/recovery/**").permitAll()

                                // Admin-only endpoints
                                .requestMatchers(HttpMethod.POST, "/posts/search").permitAll() // Optional: restrict this
                                .requestMatchers("/users/{username}/roles/{role}")
                                .hasRole(Role.ADMINISTRATOR.name())
                                .requestMatchers(HttpMethod.POST, "/checkOut").permitAll() // Optional: restrict this

                                .requestMatchers( "/ordersByUser/{userId}").permitAll()

                                // User-specific endpoints (authorization by path variable)
                                .requestMatchers(HttpMethod.PUT, "/users/{username}/wishList/{productId}")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))
                                .requestMatchers(HttpMethod.DELETE, "/users/{username}/wishList/{productId}")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))
                                .requestMatchers(HttpMethod.POST, "/users/payment/capture")
                                .permitAll()
                                .requestMatchers(HttpMethod.POST, "/users/{username}/payment/createOrder")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))
                                .requestMatchers(HttpMethod.PUT, "/payment-method/{login}")
                                .access(new WebExpressionAuthorizationManager("#login == authentication.name"))

                                .requestMatchers(HttpMethod.PUT, "/{username}/cartList/{productId}/update/{isAdd}")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))

                                .requestMatchers(HttpMethod.PUT, "/users/{username}/cartList")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))
                                .requestMatchers(HttpMethod.DELETE, "/users/{username}/cartList")
                                .access(new WebExpressionAuthorizationManager("#username == authentication.name"))

                                .requestMatchers(HttpMethod.POST, "/users/address/{login}")
                                .access(new WebExpressionAuthorizationManager("#login == authentication.name"))

                                .requestMatchers(HttpMethod.DELETE, "/users/{login}")
                                .access(new WebExpressionAuthorizationManager(
                                        "#login == authentication.name or hasRole('ADMINISTRATOR')"))

                                // Post management endpoints (moderators)
                                .requestMatchers(HttpMethod.POST, "/post/{author}")
                                .hasRole(Role.MODERATOR.name())
                                .requestMatchers(HttpMethod.POST, "/post/file/upload")
                                .hasRole(Role.MODERATOR.name())
                                .requestMatchers("/post/file/delete/{file}")
                                .hasRole(Role.MODERATOR.name())
                                .requestMatchers(HttpMethod.DELETE, "/post/{id}")
                                .hasRole(Role.MODERATOR.name())

                                // Update post (optional, currently open to all)
                                .requestMatchers(HttpMethod.PUT, "/post/{id}").permitAll()

                                // User's own posts
                                .requestMatchers("/posts/{author}")
                                .access(new WebExpressionAuthorizationManager("#author == authentication.name"))

                                // Comments
                                .requestMatchers(HttpMethod.PUT, "/post/{id}/comment/{author}")
                                .access(new WebExpressionAuthorizationManager("#author == authentication.name"))
                                .requestMatchers(HttpMethod.DELETE, "/post/{id}/comment")
                                .hasRole(Role.MODERATOR.name())

                                // Catch-all: everything else must be authenticated
                                .anyRequest().authenticated()
                );

        return http.build();
    }
}
