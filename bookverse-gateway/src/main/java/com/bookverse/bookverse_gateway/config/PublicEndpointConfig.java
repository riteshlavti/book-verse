package com.bookverse.bookverse_gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
@Component
@ConfigurationProperties(prefix = "bookverse-security")
public class PublicEndpointConfig {
    private Map<String, List<String>> publicEndpoints;

    @PostConstruct
    public void logConfig() {
        log.info("Public Endpoints Configuration: {}", publicEndpoints);
    }
}