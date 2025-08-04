//package dev.shrekback.accounting.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PayPalServiceImpl implements PayPalService {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Value("${paypal.client-id}")
//    private String clientId;
//
//    @Value("${paypal.secret}")
//    private String secret;
//
//    @Value("${paypal.base-url}")
//    private String baseUrl;
//
//    public boolean captureOrder(String orderId) {
//        try {
//            // 1. Получаем access_token
//            String accessToken = getAccessToken();
//
//            if (accessToken == null) {
//                log.error("Failed to retrieve PayPal access token");
//                return false;
//            }
//
//            // 2. Делаем запрос capture
//            String captureUrl = baseUrl + "/v2/checkout/orders/" + orderId + "/capture";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setBearerAuth(accessToken);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<String> request = new HttpEntity<>(null, headers);
//
//            ResponseEntity<String> response = restTemplate.postForEntity(captureUrl, request, String.class);
//
//            if (response.getStatusCode().is2xxSuccessful()) {
//                log.info("Order captured successfully: {}", response.getBody());
//                return true;
//            } else {
//                log.error("Failed to capture order: {}", response.getBody());
//                return false;
//            }
//
//        } catch (Exception e) {
//            log.error("Exception while capturing order: ", e);
//            return false;
//        }
//    }
//
//    private String getAccessToken() {
//        try {
//            String tokenUrl = baseUrl + "/v1/oauth2/token";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//            headers.setBasicAuth(clientId, secret, StandardCharsets.UTF_8);
//
//            HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
//
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    tokenUrl,
//                    HttpMethod.POST,
//                    request,
//                    Map.class
//            );
//
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                return (String) response.getBody().get("access_token");
//            } else {
//                log.error("Failed to get PayPal token: {}", response.getBody());
//                return null;
//            }
//        } catch (Exception e) {
//            log.error("Exception while getting PayPal token: ", e);
//            return null;
//        }
//    }
//}
