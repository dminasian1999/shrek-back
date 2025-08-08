package dev.shrekback.accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

//    @Value("${israelpost.username}")
    private String username = "YOUR_USER";

//    @Value("${israelpost.password}")
    private String password = "YOUR_PASS";

     final WebClient webClient;

    @Override
    public Mono<String> estimateShipping(String countryCode, int weightGrams, int serviceCode) {
        String soapRequest = buildSoapRequest(username, password, weightGrams, serviceCode, countryCode);
        return webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, "text/xml;charset=UTF-8")
                .bodyValue(soapRequest)
                .retrieve()
                .bodyToMono(String.class);
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
            """.formatted(user, pass, weight, serviceCode, countryCode);
    }
}
