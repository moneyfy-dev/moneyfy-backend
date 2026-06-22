package com.referidos.app.segurosref;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class SegurosrefApplication extends SpringBootServletInitializer{

	@PostConstruct
	public void init() {
		// Establecer la zona horaria global de la aplicación a Santiago de Chile
		TimeZone.setDefault(TimeZone.getTimeZone("America/Santiago"));
	}

	public static void main(String[] args) {
		SpringApplication.run(SegurosrefApplication.class, args);
	}
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder){
		return builder.sources(SegurosrefApplication.class);
	}

}
