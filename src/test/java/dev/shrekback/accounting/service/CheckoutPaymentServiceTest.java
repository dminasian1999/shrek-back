package dev.shrekback.accounting.service;

import com.mongodb.client.result.UpdateResult;
import com.paypal.orders.*;
import dev.shrekback.accounting.dao.OrderRepository;
import dev.shrekback.accounting.model.OrderItem;
import dev.shrekback.accounting.model.OrderStatus;
import dev.shrekback.post.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CheckoutPaymentServiceTest {
    PayPalService gateway = mock(PayPalService.class);
    OrderRepository orders = mock(OrderRepository.class);
    MongoTemplate mongo = mock(MongoTemplate.class);
    TransactionTemplate transactions = mock(TransactionTemplate.class);
    CheckoutPaymentService service = new CheckoutPaymentService(gateway, orders, mongo, transactions, new ModelMapper());
    dev.shrekback.accounting.model.Order order;

    @BeforeEach
    void setup() throws Exception {
        ReflectionTestUtils.setField(service, "paymentsEnabled", true);
        order = new dev.shrekback.accounting.model.Order();
        order.setOrderId("local-id"); order.setPaypalOrderId("paypal-id"); order.setUserId("owner");
        order.setTotalAmount(new BigDecimal("27.18")); order.setCurrencyCode("USD");
        order.setDateCreated(LocalDateTime.now());
        order.setOrderItems(List.of(new OrderItem("product", null, 2, 12.34, "One size", "cart-1")));
        when(gateway.getOrder("paypal-id")).thenReturn(new com.paypal.orders.Order().id("paypal-id").status("APPROVED"));
        when(orders.findByPaypalOrderId("paypal-id")).thenReturn(Optional.of(order));
        when(orders.findById("local-id")).thenReturn(Optional.of(order));
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class)); return null;
        }).when(transactions).executeWithoutResult(any());
        when(mongo.updateFirst(any(Query.class), any(UpdateDefinition.class), eq(dev.shrekback.accounting.model.Order.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Post.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
    }

    com.paypal.orders.Order captured(String currency, String amount) {
        return new com.paypal.orders.Order().id("paypal-id").status("COMPLETED").purchaseUnits(List.of(
                new PurchaseUnit().referenceId("local-id").payments(new PaymentCollection().captures(List.of(
                        new Capture().id("capture-id").status("COMPLETED").amount(new Money().currencyCode(currency).value(amount)))))));
    }

    @Test
    void verifiesCurrencyAndAmountBeforeMarkingPaid() {
        assertDoesNotThrow(() -> CheckoutPaymentService.verifyCapture(order, captured("USD", "27.18")));
        assertThrows(ResponseStatusException.class, () -> CheckoutPaymentService.verifyCapture(order, captured("USD", "0.01")));
        assertThrows(ResponseStatusException.class, () -> CheckoutPaymentService.verifyCapture(order, captured("ILS", "27.18")));
        assertThrows(ResponseStatusException.class, () -> CheckoutPaymentService.verifyCapture(order, captured("USD", "27.18").status("APPROVED")));
    }

    @Test
    void strangerCannotCaptureOrCreateOwnerPayment() throws Exception {
        assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "stranger"));
        assertThrows(ResponseStatusException.class, () -> service.createPayment("local-id", "stranger"));
        verify(gateway, never()).captureOrder(anyString(), anyString());
        verifyNoInteractions(mongo);
    }

    @Test
    void soldOutStockFailsBeforeCharging() throws Exception {
        when(mongo.updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Post.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        var failure = assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        assertEquals(409, failure.getStatusCode().value());
        verify(gateway, never()).captureOrder(anyString(), anyString());
        verify(orders, never()).save(any());
    }

    @Test
    void validCaptureIsRecordedAndRetryDoesNotChargeTwice() throws Exception {
        when(gateway.captureOrder(eq("paypal-id"), anyString())).thenReturn(captured("USD", "27.18"));
        var result = service.capturePayment("paypal-id", "owner");
        assertEquals(OrderStatus.PAID, result.getStatus());
        service.capturePayment("paypal-id", "owner");
        verify(gateway, times(1)).captureOrder(eq("paypal-id"), anyString());
        verify(orders, times(1)).save(order);
    }

    @Test
    void unapprovedPaymentCannotReserveInventory() throws Exception {
        when(gateway.getOrder("paypal-id")).thenReturn(new com.paypal.orders.Order().id("paypal-id").status("CREATED"));
        assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        verify(mongo, never()).updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Post.class));
        verify(gateway, never()).captureOrder(anyString(), anyString());
    }

    @Test
    void expiredUnreservedCheckoutCannotCharge() throws Exception {
        order.setDateCreated(LocalDateTime.now().minusHours(1));
        assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        verify(mongo, never()).updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Post.class));
        verify(gateway, never()).captureOrder(anyString(), anyString());
    }

    @Test
    void providerCompletedPaymentRecoversLostCaptureResponseWithoutChargingAgain() throws Exception {
        when(gateway.getOrder("paypal-id")).thenReturn(captured("USD", "27.18"));
        assertEquals(OrderStatus.PAID, service.capturePayment("paypal-id", "owner").getStatus());
        verify(gateway, never()).captureOrder(anyString(), anyString());
    }

    @Test
    void definitiveDeclineRestoresStockButUncertainFailureKeepsReservation() throws Exception {
        when(gateway.captureOrder(eq("paypal-id"), anyString())).thenThrow(
                new com.paypal.http.exceptions.HttpException("INSTRUMENT_DECLINED", 422, new com.paypal.http.Headers()));
        assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        var updates = org.mockito.ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongo, times(2)).updateFirst(any(Query.class), updates.capture(), eq(Post.class));
        assertEquals(-2, ((org.bson.Document) updates.getAllValues().get(0).getUpdateObject().get("$inc")).get("sizeQuantities.$.quantity"));
        assertEquals(2, ((org.bson.Document) updates.getAllValues().get(1).getUpdateObject().get("$inc")).get("sizeQuantities.$.quantity"));
        verify(orders, never()).save(any());

        clearInvocations(mongo);
        doThrow(new java.io.IOException("timeout")).when(gateway).captureOrder(eq("paypal-id"), anyString());
        assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        verify(mongo, times(1)).updateFirst(any(Query.class), any(UpdateDefinition.class), eq(Post.class));
    }

    @Test
    void paymentsDisabledByDefault() {
        ReflectionTestUtils.setField(service, "paymentsEnabled", false);
        var failure = assertThrows(ResponseStatusException.class, () -> service.capturePayment("paypal-id", "owner"));
        assertEquals(503, failure.getStatusCode().value());
        verifyNoInteractions(gateway, orders, mongo);
    }
}
