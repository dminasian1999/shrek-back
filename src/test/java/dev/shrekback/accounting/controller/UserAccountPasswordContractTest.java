package dev.shrekback.accounting.controller;

import dev.shrekback.accounting.service.UserAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccountPasswordContractTest {
    UserAccountService accounts = mock(UserAccountService.class);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new UserAccountController(accounts)).build();

    @Test void acceptsUnicodePasswordInJson() throws Exception {
        mvc.perform(put("/users/password").principal(() -> "buyer@example.com")
                        .contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8)
                        .content("{\"newPassword\":\"Գաղտնաբառ12🔐\"}"))
                .andExpect(status().isNoContent());
        verify(accounts).changePassword("buyer@example.com", "Գաղտնաբառ12🔐");
    }

    @Test void retainsLegacyHeaderCompatibility() throws Exception {
        mvc.perform(put("/users/password").principal(() -> "buyer@example.com")
                        .header("X-Password", "new-password"))
                .andExpect(status().isNoContent());
        verify(accounts).changePassword("buyer@example.com", "new-password");
    }
}
