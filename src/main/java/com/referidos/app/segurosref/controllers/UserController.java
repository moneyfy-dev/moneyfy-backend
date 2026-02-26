package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.referidos.app.segurosref.helpers.BindingHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.SeedDefaultRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponses;
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
@RequestMapping(value = "/users")
@PreAuthorize(value = "denyAll()")
@Tag(
    name = "User",
    description = "Controller to handle the system users"
)
public class UserController {

    @Autowired
    private UserService userService;

    // ENDPOINTS PARA FLUJOS RELACIONADOS AL USUARIO
    @PutMapping(value = "/update")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Update your user",
        description = "Update your specific user by its id",
        tags = {"User"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Update your user data",
            required = true,
            content = @Content(
                mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                schema = @Schema(implementation = UserUpdateRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The user was updated successfully",
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
    public ResponseEntity<?> update(@ModelAttribute UserUpdateRequest user, Authentication authentication) {
        BindingHelper bindingHelper = new BindingHelper();
        userService.validateUpdate(user, bindingHelper);
        if(bindingHelper.isError()) {
            return ResponseHelper.preconditionMap("información no aceptada", bindingHelper.getData());
        }
        return userService.update(user, authentication.getPrincipal().toString());
    }

    @PutMapping(value = "/change/password")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Update your user's current password",
        description = "Submit your old password for verification and your new one for be changed",
        tags = {"User"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Change your user's password",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = ChangePwdRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "the user password was changed successfully",
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
    public ResponseEntity<?> changePassword(@RequestBody ChangePwdRequest changePwd, BindingResult bindingResult,
            Authentication authentication) {
        userService.validatePasswordChanged(changePwd, bindingResult);
        if(bindingResult.hasErrors()) {
            return ResponseHelper.preconditionMap("información no aceptada", ResponseHelper.buildErrorFields(bindingResult));
        }
        return userService.changePassword(changePwd, authentication.getPrincipal().toString());
    }

    @PostMapping(value = "/hydration/data")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Hydrate your user data",
        description = "Hydrate your user data by your own token",
        tags = {"User"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "the user data was hydrated successfully",
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
    public ResponseEntity<GeneralResponses> hydrationData(Authentication auth, HttpServletRequest request) {
        return userService.hydrationData(auth.getPrincipal().toString(), auth.getCredentials().toString(), request.getHeader("User-Agent"));
    }

    @PostMapping(value = "/list/referreds")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Obtain the list of the referreds",
        description = "Obtain the list of the referreds of the user",
        tags = {"User"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The referreds have been recovered",
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
    public ResponseEntity<GeneralResponses> listReferreds(Authentication auth, HttpServletRequest request) {
        return userService.listReferreds(auth.getPrincipal().toString(), auth.getCredentials().toString(), request.getHeader("User-Agent"));
    }

    @PostMapping(value = "/obtain/commissions")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Obtain the commissions of the user",
        description = "Obtain the commissions of the user",
        tags = {"User"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The commissions have been recovered",
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
    public ResponseEntity<GeneralResponses> obtainCommissions(Authentication auth) {
        return userService.obtainCommissions(auth.getPrincipal().toString());
    }

    @PostMapping(value = "/obtain/payments")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Obtain the payments of the user",
        description = "Obtain the payments of the user",
        tags = {"User"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The payments have been recovered",
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
    public ResponseEntity<GeneralResponses> obtainPayments(Authentication auth) {
        return userService.obtainPayments(auth.getPrincipal().toString());
    }

    // ENDPOINT PARA OBTENER LAS GANANCIAS DEL USUARIO EN LOS ÚLTIMOS 5 MESES
    @PostMapping(value = "/monthly/earnings")
    @PreAuthorize(value = "hasAnyRole('ADMIN', 'USER')")
    @Operation(
        summary = "Obtain the monthly earnings of the user",
        description = "Obtain the monthly earnings of the user",
        tags = {"User"},
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The monthly earnings of the user have been recovered",
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
    public ResponseEntity<GeneralResponses> monthlyEarnings(Authentication auth) {
        return userService.monthlyEarnings(auth.getPrincipal().toString());
    }

    // ENDPOINT PARA ALMACENAR O ACTUALIZAR LA DATA POR DEFECTO
    @PostMapping(value = "/seed/default")
    @PreAuthorize(value = "permitAll()")
    @Operation(
        summary = "Seed the default data",
        description = "Seed the default data",
        tags = {"User"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Enter the key to update the default data",
            required = true,
            content = @Content(
                mediaType = CONTENT_TYPE,
                schema = @Schema(implementation = SeedDefaultRequest.class)
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "The default data has been updated",
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
    public ResponseEntity<GeneralResponses> seedDefault(@RequestBody SeedDefaultRequest seedDefault) {
        return userService.seedDefault(seedDefault);
    }

    // ENDPOINTS SUPUESTOS PARA ADMINISTRADORES QUE NO SE ESTÁN UTILIZANDO AÚN
    // @GetMapping
    // @PreAuthorize(value = "hasRole('ADMIN')")
    // @Operation (
    //     summary = "Get all users",
    //     description = "You can get all the system users registered, having admin role",
    //     tags = {"User"},
    //     responses = @ApiResponse(
    //         responseCode = "200",
    //         description = "OK",
    //         content = @Content(
    //             mediaType = CONTENT_TYPE,
    //             schema = @Schema(implementation = GeneralResponses.class)
    //         )
    //     )
    // )
    // public ResponseEntity<?> listAll() {
    //     return ResponseHelper.ok(
    //             "la solicitud fue recibida correctamente",
    //             Map.of("users", userService.findAll()));
    // }

    // @GetMapping(value = "/{id}")
    // @PreAuthorize(value = "hasRole('ADMIN')")
    // @Operation (
    //     summary = "Get a specific user",
    //     description = "Get a specific user by its id, having admin role",
    //     tags = {"User"},
    //     parameters = {
    //         @Parameter (
    //             name = "id",
    //             description = "Enter the user id to start the searching",
    //             example = "123456789123456789abcdef",
    //             required = true,
    //             in = ParameterIn.PATH
    //         )
    //     },
    //     responses = {
    //         @ApiResponse(
    //             responseCode = "200",
    //             description = "The user was encountered successfully",
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
    // public ResponseEntity<?> findById(@PathVariable String id) {
    //     if(!ObjectId.isValid(id)) {
    //         return ResponseHelper.notFound("no se ha podido encontrar el usuario", null);
    //     }
    //     return userService.findById(new ObjectId(id));
    // }

}
