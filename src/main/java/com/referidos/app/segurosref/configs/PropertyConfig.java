package com.referidos.app.segurosref.configs;

import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@Configuration
@PropertySources(value = {
    @PropertySource(value = "classpath:info-messages.properties", encoding = "UTF-8"),
    @PropertySource(value = "classpath:own-env.properties", encoding = "UTF-8")
})
public class PropertyConfig {

    public static final Logger LOGGER_MESSAGES = Logger.getLogger(PropertyConfig.class.getName());

}
