package dev.shrekback.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AuthorizationConfigurationTest.Application.class)
@AutoConfigureMockMvc
class AuthorizationConfigurationTest {
    @Autowired MockMvc mvc;

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
    @Import({AuthorizationConfiguration.class, Probe.class})
    static class Application {
        @Bean UserDetailsService users() { return new InMemoryUserDetailsManager(); }
    }

    @RestController
    static class Probe {
        @RequestMapping({"/posts", "/post/{id}", "/users/{username}/wishList/{productId}", "/users/{username}/cartList",
                "/orders-all", "/ordersByUser/{userId}", "/users", "/parser/{index}/{csv}", "/users/payment-method/{login}",
                "/users/payment/capture", "/saveOrder/{login}"})
        String ok() { return "ok"; }
    }

    @Test void anonymousCanBrowseButCannotEditCatalog() throws Exception {
        mvc.perform(get("/posts")).andExpect(status().isOk());
        mvc.perform(get("/post/product")).andExpect(status().isOk());
        mvc.perform(put("/post/product")).andExpect(status().isUnauthorized());
        mvc.perform(put("/post/product").with(user("buyer").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(put("/post/product").with(user("seller").roles("MODERATOR"))).andExpect(status().isOk());
    }

    @Test void wishlistAndCartBelongToAuthenticatedUser() throws Exception {
        mvc.perform(put("/users/alice/wishList/product")).andExpect(status().isUnauthorized());
        mvc.perform(put("/users/alice/wishList/product").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(put("/users/alice/wishList/product").with(user("alice"))).andExpect(status().isOk());
        mvc.perform(put("/users/alice/cartList").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(put("/users/payment-method/alice").with(user("bob"))).andExpect(status().isForbidden());
    }

    @Test void personalDataAndImportAreNeverPublic() throws Exception {
        mvc.perform(get("/orders-all")).andExpect(status().isUnauthorized());
        mvc.perform(get("/orders-all").with(user("buyer"))).andExpect(status().isForbidden());
        mvc.perform(get("/orders-all").with(user("admin").roles("ADMINISTRATOR"))).andExpect(status().isOk());
        mvc.perform(get("/ordersByUser/alice").with(user("bob"))).andExpect(status().isForbidden());
        mvc.perform(get("/ordersByUser/alice").with(user("alice"))).andExpect(status().isOk());
        mvc.perform(get("/users").with(user("buyer"))).andExpect(status().isForbidden());
        mvc.perform(post("/parser/1/EMS.csv")).andExpect(status().isUnauthorized());
    }

    @Test void legacyPaymentBypassesAreBlockedEvenForAuthenticatedUsers() throws Exception {
        mvc.perform(post("/users/payment/capture").with(user("buyer"))).andExpect(status().isForbidden());
        mvc.perform(post("/saveOrder/alice").with(user("alice"))).andExpect(status().isForbidden());
    }

    @Test void untrustedCorsOriginCannotReadApi() throws Exception {
        mvc.perform(options("/posts").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET")).andExpect(status().isForbidden());
        mvc.perform(options("/posts").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(get("/posts")).andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}
