package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.ConfirmUserRequest;
import com.referidos.app.segurosref.requests.EmailRequest;
import com.referidos.app.segurosref.requests.PasswordResetRequest;
import com.referidos.app.segurosref.requests.UserLoginRequest;
import com.referidos.app.segurosref.requests.UserRegisterRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
import com.referidos.app.segurosref.services.UserDetailsServiceImpl;
import com.referidos.app.segurosref.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/auth")
@Tag(
    name = "Authentication",
    description = "Controller to authenticate the system users"
)
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    // ENDPOINTS PARA EL FLUJO DE REGISTRAR UN NUEVO USUARIO DE LA APLICACIÓN
    @PostMapping(value = "/register")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Receive a confirmation code for common users",
        description = "Receive a confirmation code to register a new user",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter your user information",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = UserRegisterRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Confirmation code was sent successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> register(@RequestBody UserRegisterRequest user, BindingResult bindingResult) {
        userService.validateRegister(user, bindingResult);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return userDetailsServiceImpl.userRegister(user);
    }

    @PostMapping(value="/confirm/registration")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Complete your user registration",
        description = "Complete your user registration sending the confirmation code",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the email account and the code confirmation",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = ConfirmUserRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "User registered successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> confirmRegistration(@RequestBody ConfirmUserRequest confirm,
                HttpServletRequest request) throws JsonProcessingException {
        return userDetailsServiceImpl.confirmRegistration(confirm, request);
    }

    // ENDPOINT PARA INICIO DE SESSIÓN DE UN USUARIO DE LA APLICACIÓN
    @PostMapping(value = "/log-in")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "User login",
        description = "Authenticate the system user and return the token authentication",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Validate your user with the email and the password",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = UserLoginRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Successful authentication",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> login(@RequestBody UserLoginRequest requestUser,
            HttpServletRequest request) throws JsonProcessingException {
        return userDetailsServiceImpl.userLogin(requestUser, request);
    }

    // ENDPOINT PARA CAMBIAR EL DISPOSITIVO RELACIONADO A LA CUENTA DEL USUARIO DE LA APLICACIÓN
    @PutMapping(value = "/confirm/device/change")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Confirm the device change",
        description = "Confirm the device change of your user account",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the email account and the confirmation code for the device change",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = ConfirmUserRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The device of your account was changed successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> confirmDeviceChange(@RequestBody ConfirmUserRequest confirm, HttpServletRequest request) {
        return userDetailsServiceImpl.confirmDeviceChange(confirm, request);
    }

    // ENDPOINTS PARA EL FLUJO DE RESTABLECIMIENTO DE LA CONTRASEÑA DEL USUARIO DE LA APLICACIÓN
    @PostMapping(value = "/restore/password")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Get the authorization code to change your user's password",
        description = "Get the authorization code to change your user's password",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the email account to receive the authorization code",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = EmailRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The authorization code was sent successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> restorePassword(@RequestBody EmailRequest emailRequest) {
        return userDetailsServiceImpl.restorePassword(emailRequest.email());
    }

    @PutMapping(value = "/confirm/password/reset")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Restore your user's password",
        description = "Restore your user's password by the authorization code",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the authorization code and the password for your account",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = PasswordResetRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Your user's password was changed successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> confirmPasswordReset(@RequestBody PasswordResetRequest passwordReset, HttpServletRequest request) {
        return userDetailsServiceImpl.confirmPasswordReset(passwordReset, request);
    }

    // ENDPOINT PARA REENVIAR CÓDIGO DE CONFIRMACIÓN EN FLUJO ACTIVO, YA SEA DE: REGISTRAR USUARIO, CAMBIO DE DISPOSITIVO O REESTABLECIMIENTO DE LA CONTRASEÑA
    @PutMapping(value = "/resend/code")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Resending authorization code",
        description = "Resending authorization code by user email",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the email account",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = EmailRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The authorization code was resent successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        }
    )
    public ResponseEntity<GeneralResponses> resendUserCode(@RequestBody EmailRequest emailRequest, HttpServletRequest request) {
        return userDetailsServiceImpl.resendUserCode(emailRequest.email(), emailRequest.type());
    }

    // ENDPOINT PARA DESHABILITAR/ELIMINAR USUARIO DE LA APLICACIÓN
    @DeleteMapping(value = "/disable/account")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Disable user",
        description = "Disable user account",
        tags = {"Authentication"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The user account was disabled successfully",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            ),
            @ApiResponse(
                responseCode = "4XX",
                description = "General responses",
                content = @Content(
                    mediaType = CONTENT_TYPE,
                    schema = @Schema(implementation = GeneralResponses.class)
                )
            )
        },
        parameters = {
            @Parameter(
                name = "Refresh-Token",
                in = ParameterIn.HEADER,
                description = "Token that allow you to update the credentials",
                required = true
            )
        }
    )
    public ResponseEntity<GeneralResponses> disableAccount(Authentication auth, HttpServletRequest request) {
        return userDetailsServiceImpl.disableAccount(auth.getPrincipal().toString(), request.getHeader("User-Agent"));
    }

    // ENDPOINT PRUEBA PARA AUTENTICARSE CON GOOGLE, RECIBE EL TOKEN Y LO VALIDA
    // @GetMapping(value="/sso/{token}")
    // @PreAuthorize(value = "permitAll()")
    // @Operation(
    //     summary = "Validate user by Google SSO",
    //     description = "Validate your google account by the token provided",
    //     tags = {"Authentication"},
    //     parameters = {
    //         @Parameter(
    //             name = "token",
    //             description = "Access token provided by Google SSO",
    //             example = "tokenID",
    //             required = true,
    //             in = ParameterIn.PATH
    //         )
    //     },
    //     responses = {
    //         @ApiResponse(
    //             responseCode = "200",
    //             description = "Successful authentication",
    //             content = @Content(
    //                 mediaType = CONTENT_TYPE
    //                 // schema = @Schema(implementation = GeneralResponses.class)
    //             )
    //         ),
    //         @ApiResponse(
    //             responseCode = "4XX",
    //             description = "General responses",
    //             content = @Content(
    //                 mediaType = CONTENT_TYPE,
    //                 schema = @Schema(implementation = GeneralResponses.class)
    //             )
    //         )
    //     }
    // )
    // public ResponseEntity<GeneralResponses> googleSSO(@PathVariable String token) {
    //     RestTemplate restTemplate = new RestTemplate();
    //     String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + token;
    //     try {
    //         ResponseEntity<Map> response = restTemplate.getForEntity(tokenInfoUrl, Map.class);
    //         if(response.getStatusCode() == HttpStatus.OK) {
    //             Map<String, Object> userInfo = response.getBody();
    //             return ResponseHelper.ok("el recurso fue encontrado exitosamente", userInfo);
    //         }
    //     } catch(RestClientException e) {
    //         return ResponseHelper.notFound("no se ha podido identificar el recurso", null);
    //     }
    //     return ResponseHelper.notFound("no se ha podido identificar el recurso", null);
    // }

    // SERVICIO SUPUESTO PARA CREAR USUARIO ADMINISTRADOR, NO IMPLEMENTADO
    // @PostMapping(value = "/save")
    // @PreAuthorize(value = "hasRole('ADMIN')")
    // @Operation(
    //     summary = "Receive a confirmation code to register a new user",
    //     description = "Receive a confirmation code for register any kind of users",
    //     tags = {"Authentication"},
    //     requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
    //         description = "Enter your user information",
    //         required = true,
    //         content = @Content(
    //             mediaType = CONTENT_TYPE,
    //             schema = @Schema(implementation = UserSaveRequest.class)
    //         )
    //     ),
    //     responses = {
    //         @ApiResponse(
    //             responseCode = "200",
    //             description = "Confirmation code was sent successfully",
    //             content = @Content(
    //                 mediaType = CONTENT_TYPE,
    //                 schema = @Schema(implementation = GeneralResponses.class)
    //             )
    //         ),
    //         @ApiResponse(
    //             responseCode = "4XX",
    //             description = "General responses",
    //             content = @Content(
    //                 mediaType = CONTENT_TYPE,
    //                 schema = @Schema(implementation = GeneralResponses.class)
    //             )
    //         )
    //     }
    // )
    // public ResponseEntity<GeneralResponses> save(@RequestBody UserRegisterRequest user, BindingResult bindingResult, Authentication auth) {
    //     userService.validateSave(user, bindingResult);
    //     if(bindingResult.hasErrors()) {
    //         return ResponseHelper.badRequestMap("información no aceptada", ResponseHelper.validate(bindingResult));
    //     }
    //     return userDetailsServiceImpl.userSave(user);
    // }

}
