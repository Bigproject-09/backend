package com.example.agent_rnd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "external.data-go")
public class ExternalDataGoProperties {
    private String serviceKey;
    private String baseUrl;
}
