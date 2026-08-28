package com.logistics.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class LoadBalancedClientConfig {

    /**
     * Load-Balanced RestTemplate that dynamically resolves Eureka service virtual hostnames.
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3 seconds connect timeout
        factory.setReadTimeout(5000);    // 5 seconds read timeout
        return new RestTemplate(factory);
    }

    /**
     * Load-Balanced WebClient Builder for reactive and non-blocking inter-service calls.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
