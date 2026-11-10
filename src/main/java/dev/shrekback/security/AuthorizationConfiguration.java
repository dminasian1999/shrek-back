package dev.shrekback.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AuthorizationConfiguration {
    private static WebExpressionAuthorizationManager owner(String variable) {
        return new WebExpressionAuthorizationManager("#" + variable + " == authentication.name");
    }

    @Bean
    public SecurityFilterChain web(HttpSecurity http) throws Exception {
        http.httpBasic(Customizer.withDefaults())
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                // The SPA sends explicit Authorization headers. Never create a session cookie.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts", "/post/{id}", "/posts/category/**",
                                "/posts/criteria/**", "/posts/type/**", "/parser/countries",
                                "/shippingCost/{country}/{weight}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/register", "/posts/search", "/posts/wishList").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/password/recovery/{username}").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/users/password/recovery/{token}").permitAll()
                        .requestMatchers("/users/{username}/roles/{role}", "/orders-all",
                                "/parser/**", "/parser-ems/**", "/parser-eco/**").hasRole("ADMINISTRATOR")
                        .requestMatchers(HttpMethod.GET, "/users").hasRole("ADMINISTRATOR")
                        .requestMatchers("/users/{username}/wishList/**", "/users/{username}/cartList/**")
                            .access(owner("username"))
                        .requestMatchers("/users/address/{login}", "/users/payment-method/{login}")
                            .access(owner("login"))
                        .requestMatchers(HttpMethod.DELETE, "/users/{login}")
                            .access(new WebExpressionAuthorizationManager("#login == authentication.name or hasRole('ADMINISTRATOR')"))
                        .requestMatchers("/ordersByUser/{userId}")
                            .access(new WebExpressionAuthorizationManager("#userId == authentication.name or hasRole('ADMINISTRATOR')"))
                        .requestMatchers(HttpMethod.POST, "/post/file/upload", "/post/{author}").hasAnyRole("MODERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/post/{id}").hasAnyRole("MODERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.DELETE, "/post/{id}", "/post/file/delete/{file}").hasAnyRole("MODERATOR", "ADMINISTRATOR")
                        // Legacy persistence/capture bypasses must never bypass the verified checkout flow.
                        .requestMatchers("/saveOrder/**", "/users/payment/capture").denyAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,https://shrek-front.vercel.app,https://gejekoushiangifts.vercel.app}") String origins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Password", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
