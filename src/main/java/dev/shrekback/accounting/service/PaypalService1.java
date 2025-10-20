package dev.shrekback.accounting.service;

import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.exceptions.HttpException;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor

public class PaypalService1 {


    final PayPalHttpClient payPalClient;

    /**
     * Create a PayPal order (for one-time payment).
     * @param amount  the total amount to charge (in USD for this example).
     * @return the created PayPal Order ID.
     */
    public String createOrder(String amount) throws IOException {
        // Construct order request
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");  // intent to capture funds immediately

        // Set order amount and currency
        AmountWithBreakdown amountBreakdown = new AmountWithBreakdown()
                .currencyCode("USD")
                .value(amount);
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .amountWithBreakdown(amountBreakdown);
        orderRequest.purchaseUnits(java.util.List.of(purchaseUnit));

        // Optional: Set application context (e.g., brand name, return/cancel URLs if doing redirect)
        orderRequest.applicationContext(new ApplicationContext().brandName("My Shop"));

        // Create the order request
        OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);

        // Call PayPal to create the order
        HttpResponse<Order> response = payPalClient.execute(request);
        Order order = response.result();
        // Return the order ID (to be sent to client)
        return order.id();
    }

    /**
     * Capture an approved PayPal order.
     * @param orderId the ID of the approved PayPal order to capture.
     * @return the captured Order (or null if failed).
     */
    public Order captureOrder(String orderId) throws IOException {
        OrdersCaptureRequest request = new OrdersCaptureRequest(orderId);
        // Call PayPal to capture the order
        HttpResponse<Order> response = payPalClient.execute(request);
        Order order = response.result();
        return order;
    }
}
