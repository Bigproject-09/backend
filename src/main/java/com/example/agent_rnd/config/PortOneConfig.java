//package com.example.agent_rnd.config;
//
//import com.siot.IamportRestClient.IamportClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class PortOneConfig {
//
//    @Value("${payment.portone.api-key}")
//    private String apiKey;
//
//    @Value("${payment.portone.api-secret}")
//    private String apiSecret;
//
//    @Bean
//    public IamportClient iamportClient() {
//        return new IamportClient(apiKey, apiSecret);
//    }
//}