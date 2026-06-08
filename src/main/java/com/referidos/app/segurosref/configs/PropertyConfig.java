package com.referidos.app.segurosref.configs;

import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
@Configuration
@PropertySources(value = {
    @PropertySource(value = "classpath:info-messages.properties", encoding = "UTF-8"),
    @PropertySource(value = "classpath:own-env.properties", encoding = "UTF-8")
})
public class PropertyConfig {

    public static final Logger LOGGER_MESSAGES = Logger.getLogger(PropertyConfig.class.getName());

    @Data
    @Configuration
    @ConfigurationProperties(prefix = "moneyfy")
    public static class MoneyfyProperties {

        private String apiKey;
        private Jwt jwt = new Jwt();
        private Seeders seeders = new Seeders();

        @Data
        public static class Jwt {
            private String secret;
        }

        @Data
        public static class Seeders {
            private boolean autoEnabled;
        }
    }

}
