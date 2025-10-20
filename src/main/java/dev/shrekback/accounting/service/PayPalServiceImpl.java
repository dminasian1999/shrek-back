package dev.shrekback.accounting.service;

import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PayPalServiceImpl {

    final PayPalHttpClient client;


    /**
     * Server-trusted order creation
     */
    public String createOrder(String idempotencyKey, String currency, String value,
                              String referenceId, String returnUrl, String cancelUrl) throws IOException {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        // amount breakdown (optional but recommended)
        AmountWithBreakdown amt = new AmountWithBreakdown()
                .currencyCode(currency)
                .value(value);

        PurchaseUnitRequest pu = new PurchaseUnitRequest()
                .referenceId(referenceId)
                .amountWithBreakdown(amt);

        ApplicationContext ctx = new ApplicationContext()
                .brandName("Your Store")
                .landingPage("NO_PREFERENCE")
                .userAction("PAY_NOW")
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl);

        orderRequest.purchaseUnits(List.of(pu));
        orderRequest.applicationContext(ctx);

        OrdersCreateRequest req = new OrdersCreateRequest();
        req.header("PayPal-Request-Id", idempotencyKey); // idempotency
        req.requestBody(orderRequest);

        HttpResponse<Order> response = client.execute(req);
        return response.result().id(); // pass this back to client to approve
    }

    /**
     * Server-side capture after buyer approval
     */
    public Order captureOrder(String orderId, String idempotencyKey) throws IOException {
        OrdersCaptureRequest req = new OrdersCaptureRequest(orderId);
        req.header("PayPal-Request-Id", idempotencyKey);
        req.requestBody(new OrderActionRequest());

        HttpResponse<Order> response = client.execute(req);
        return response.result();
    }
}
