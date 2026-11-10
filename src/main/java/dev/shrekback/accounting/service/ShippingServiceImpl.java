package dev.shrekback.accounting.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

@Value("${israelpost.username:}")
    private String username;

@Value("${israelpost.password:}")
    private String password;

     final WebClient webClient;

    @Override
    public Mono<String> estimateShipping(String countryCode, int weightGrams, int serviceCode) {
        if (countryCode == null || !countryCode.matches("[A-Z]{2,3}") || weightGrams <= 0 || weightGrams > 30000 || serviceCode < 1 || serviceCode > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid shipping request.");
        }
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Shipping provider is not configured.");
        }
        String soapRequest = buildSoapRequest(username, password, weightGrams, serviceCode, countryCode);
        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, "text/xml;charset=UTF-8")
                .bodyValue(soapRequest)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(15))
                .onErrorMap(error -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Shipping provider is unavailable."));
    }

    private String buildSoapRequest(String user, String pass, int weight, int serviceCode, String countryCode) {
        return """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:cal="http://tempuri.org/">
               <soapenv:Header/>
               <soapenv:Body>
                  <cal:calculateDeliveryPrice>
                     <cal:userName>%s</cal:userName>
                     <cal:password>%s</cal:password>
                     <cal:itemType>2</cal:itemType>
                     <cal:itemWeight>%d</cal:itemWeight>
                     <cal:serviceCode>%d</cal:serviceCode>
                     <cal:destCountryCode>%s</cal:destCountryCode>
                  </cal:calculateDeliveryPrice>
               </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(HtmlUtils.htmlEscape(user), HtmlUtils.htmlEscape(pass), weight, serviceCode, countryCode);
    }
}
