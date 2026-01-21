package com.example.agent_rnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import com.example.agent_rnd.config.ExternalDataGoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(ExternalDataGoProperties.class)
@ConfigurationPropertiesScan
@EnableJpaAuditing
@SpringBootApplication
public class AgentRndApplication {

    public static void main(String[] args) {

        SpringApplication.run(AgentRndApplication.class, args);
    }

}
