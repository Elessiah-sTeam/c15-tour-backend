package com.c15tour.backend.service.osrm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient restClient(RestClient.Builder builder,
                          @Value("${OSRM_API_URL:http://localhost:5000}") String osrmUrl) {
        return builder
                .baseUrl(osrmUrl)
                .build();
    }
}

