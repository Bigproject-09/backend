package com.example.agent_rnd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.data-go")
public record DataGoProperties(
        String serviceKey,
        String baseUrl
) {}
