package dev.shrekback.accounting.service;

import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/** The gateway only receives amounts calculated by the checkout service. */
@RequiredArgsConstructor
@Service
public class PayPalService {
    private final PayPalHttpClient client;

    public String createOrder(String idempotencyKey, String currency, String value, String referenceId) throws IOException {
        OrderRequest body = new OrderRequest().checkoutPaymentIntent("CAPTURE")
                .purchaseUnits(List.of(new PurchaseUnitRequest().referenceId(referenceId)
                        .amountWithBreakdown(new AmountWithBreakdown().currencyCode(currency).value(value))))
                .applicationContext(new ApplicationContext().brandName("Gejekoushian Gifts").userAction("PAY_NOW"));
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.header("PayPal-Request-Id", idempotencyKey);
        request.requestBody(body);
        HttpResponse<Order> response = client.execute(request);
        return response.result().id();
    }

    public Order getOrder(String orderId) throws IOException {
        return client.execute(new OrdersGetRequest(orderId)).result();
    }

    public Order captureOrder(String orderId, String idempotencyKey) throws IOException {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        request.header("PayPal-Request-Id", idempotencyKey);
        request.requestBody(new OrderActionRequest());
        return client.execute(request).result();
    }
}
