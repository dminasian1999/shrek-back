package dev.shrekback.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.shrekback.accounting.dao.UserAccountRepository;
import dev.shrekback.accounting.dao.UserTokenRepository;
import dev.shrekback.accounting.dto.*;
import dev.shrekback.accounting.dto.exceptions.UserExistsException;
import dev.shrekback.accounting.model.*;
import dev.shrekback.configuration.EComBackConfiguration;
import dev.shrekback.post.dao.PostRepository;
import dev.shrekback.post.dto.SizeQuantityDto;
import dev.shrekback.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserAccountServiceSecurityTest {
    UserAccountRepository users = mock(UserAccountRepository.class);
    UserTokenRepository tokens = mock(UserTokenRepository.class);
    MongoTemplate mongo = mock(MongoTemplate.class);
    PostRepository products = mock(PostRepository.class);
    ObjectMapper json = new ObjectMapper();
    UserAccountServiceImpl service;

    @BeforeEach void setup() {
        service = new UserAccountServiceImpl(new EComBackConfiguration().getModelMapper(), new BCryptPasswordEncoder(), users, tokens, mongo, products);
    }

    @Test void weakAndOversizedPasswordsNeverWriteUser() throws Exception {
        for (String password : List.of("short", "a".repeat(73), "🧅".repeat(19))) {
            UserRegisterDto dto = json.readValue("{\"login\":\"buyer@example.com\",\"password\":\"" + password + "\",\"firstName\":\"Test\",\"lastName\":\"Buyer\"}", UserRegisterDto.class);
            assertThrows(ResponseStatusException.class, () -> service.register(dto));
        }
        verifyNoInteractions(users);
    }

    @Test void registrationUsesInsertAndHandlesDuplicateRace() throws Exception {
        UserRegisterDto dto = json.readValue("{\"login\":\"buyer@example.com\",\"password\":\"safe-password\",\"firstName\":\"Test\",\"lastName\":\"Buyer\"}", UserRegisterDto.class);
        when(users.insert(any(UserAccount.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThrows(UserExistsException.class, () -> service.register(dto));
        verify(users, never()).save(any());
    }

    @Test void cartUsesServerProductAndValidatesStock() {
        UserAccount buyer = new UserAccount("buyer", "hash", "Test", "Buyer");
        when(users.findById("buyer")).thenReturn(Optional.of(buyer));
        Post real = new Post(); real.setId("product"); real.setPrice(45);
        SizeQuantityDto variant = new SizeQuantityDto(); variant.setSize("One size"); variant.setQuantity(2);
        real.setSizeQuantities(List.of(variant));
        when(products.findById("product")).thenReturn(Optional.of(real));
        Post forged = new Post(); forged.setId("product"); forged.setPrice(0.01);
        service.changeCartList("buyer", new Item("line", forged, "One size", 1), true);
        assertEquals(45, buyer.getCart().getTotalPrice());
        assertSame(real, buyer.getCart().getItems().iterator().next().getProduct());
        assertThrows(ResponseStatusException.class, () -> service.changeCartList("buyer", new Item("line", forged, "One size", 2), true));
        assertEquals(1, buyer.getCart().getItems().iterator().next().getQuantity());
        assertThrows(ResponseStatusException.class, () -> service.changeCartList("buyer", new Item("bad", forged, "One size", -1), true));
    }

    @Test void cardSecretsAreNeitherAcceptedNorSerialized() throws Exception {
        PaymentMethodDto dto = json.readValue("{\"cardno\":\"4111111111111111\",\"cvv\":\"123\"}", PaymentMethodDto.class);
        assertNull(dto.getCvv());
        dto.setCvv("legacy-secret");
        assertFalse(json.writeValueAsString(dto).contains("legacy-secret"));
        assertThrows(ResponseStatusException.class, () -> service.updatePaymentMethod("buyer", dto));
        verifyNoInteractions(users);
    }

    @Test void recoveryTokenIsConsumedAtomicallyBeforePasswordChanges() {
        when(mongo.findAndRemove(any(Query.class), eq(UserToken.class))).thenReturn(new UserToken("buyer")).thenReturn(null);
        when(users.findById("buyer")).thenReturn(Optional.of(new UserAccount("buyer", "old", "Test", "Buyer")));
        service.recoveryPassword("token", "new-password");
        assertThrows(dev.shrekback.accounting.dto.exceptions.TokenExpiredExeption.class, () -> service.recoveryPassword("token", "new-password"));
        verify(users, times(1)).save(any());
    }
}
