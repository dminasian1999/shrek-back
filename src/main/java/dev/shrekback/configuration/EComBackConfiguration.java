package dev.shrekback.configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EComBackConfiguration {
    @Value("${aws.s3.access.key}")
    private String accessKey;

    @Value("${aws.s3.secret.key}")
    private String secretKey;
    @Value("${paypal.client-id}")
    String clientId;
    @Value("${paypal.client-secret}")
    String clientSecret;
    //    @Value("${paypal.mode:sandbox}")
    String mode = "sandbox";
//    @Value("${paypal.http.timeout-ms:30000}")
//    int timeoutMs;

//    @Bean
//    public PaypalServerSdkClient getPaypalServerSdkClient() {
//        return new APICon.Builder()
//                .loggingConfig(builder -> builder
//                        .level(Level.DEBUG)
//                        .requestConfig(logConfigBuilder -> logConfigBuilder.body(true))
//                        .responseConfig(logConfigBuilder -> logConfigBuilder.headers(true)))
//                .httpClientConfig(configBuilder -> configBuilder
//                        .timeout(0))
//                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
//                        clientId,
//                        clientSecret
//                )
//                        .build())
//                .environment(Environment.SANDBOX)
//                .build();
//    }
//    @Bean
//    public PaypalServerSdkClient getPaypalServerSdkClient() {
//       return new PaypalServerSdkClient.Builder()
//                .loggingConfig(builder -> builder
//                        .level(Level.DEBUG)
//                        .requestConfig(logConfigBuilder -> logConfigBuilder.body(true))
//                        .responseConfig(logConfigBuilder -> logConfigBuilder.headers(true)))
//                .httpClientConfig(configBuilder -> configBuilder
//                        .timeout(0))
//                .clientCredentialsAuth(new ClientCredentialsAuthModel.Builder(
//                        clientId,
//                        clientSecret
//                )
//                        .build())
//                .environment(Environment.SANDBOX)
//                .build();
//    }

    //    @Bean
//    public PayPalHttpClient payPalHttpClient() {
//        PayPalEnvironment environment = mode.equalsIgnoreCase("live")
//                ? new PayPalEnvironment.Live(clientId, clientSecret)
//                : new PayPalEnvironment.Sandbox(clientId, clientSecret);
//        // Initialize PayPal HTTP client with environment
//        return new PayPalHttpClient(environment);
//    }
    @Bean
    public PayPalHttpClient payPalHttpClient() {
        PayPalEnvironment env = "live".equalsIgnoreCase(mode)
                ? new PayPalEnvironment.Live(clientId, clientSecret)
                : new PayPalEnvironment.Sandbox(clientId, clientSecret);

        return new PayPalHttpClient(env);
    }

    @Bean
    public AmazonS3 amazonS3Client() {
        BasicAWSCredentials creds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withRegion(Regions.US_EAST_1)
                .build();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create("https://services.israelpost.co.il/servicesCalculateDeliveryPriceWS/CalculateDeliveryPriceWS");
    }

    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
