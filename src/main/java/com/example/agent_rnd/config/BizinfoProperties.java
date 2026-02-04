package com.example.agent_rnd.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bizinfo.api")
@Getter
@Setter
public class BizinfoProperties {
    private String key;
    private String url;
}