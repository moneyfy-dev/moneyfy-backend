package com.referidos.app.segurosref.configs;

import org.springframework.http.HttpHeaders;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
    info = @Info(
        title = "REFERIDOS APP",
        description = "Our app allowed you to search the car insurance that adjust to your needs the most",
        termsOfService = "#",
        version = "1.0.0",
        contact = @Contact(
            name = "Name of the representative",
            email = "Representative@gmail.com",
            url = "#"
        ),
        license = @License(
            name = "Standard Software Use License for 'Name of the representative'",
            url = "#"
        )
    ),
    servers = {
        @Server(
            description = "DEV SERVER",
            url = "http://localhost:9000"
        ),
        @Server(
            description = "QA SERVER",
            url = "http://localhost:8080/segurosref"
        ),
        @Server(
            description = "QA SERVER 2",
            url = "https://app-moneyfy-qa.connect360.cl/segurosref"
        )
    },
    security = @SecurityRequirement(
        name = "Security Token"
    )
)
@SecurityScheme (
    type = SecuritySchemeType.HTTP,
    name = "Security Token",
    description = "Need the access token to use the app",
    paramName = HttpHeaders.AUTHORIZATION,
    in = SecuritySchemeIn.HEADER,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfig {

}
