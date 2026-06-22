package com.referidos.app.segurosref.controllers;

import static com.referidos.app.segurosref.configs.JwtConfig.CONTENT_TYPE;
import lombok.RequiredArgsConstructor;

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
import com.referidos.app.segurosref.helpers.DataHelper;
import com.referidos.app.segurosref.helpers.ResponseHelper;
import com.referidos.app.segurosref.requests.ChangePwdRequest;
import com.referidos.app.segurosref.requests.UserUpdateRequest;
import com.referidos.app.segurosref.responses.GeneralResponse;
import com.referidos.app.segurosref.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users")
@PreAuthorize(value = "denyAll()")
@Tag(name = "User", description = "Controller to handle the system users")
public class UserController {

        private final UserService userService;

        @PutMapping(value = "/update")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Update your user", description = "Update your specific user by its id", tags = {
                        "User" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Update your user data", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = UserUpdateRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "The user was updated successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<?> update(@ModelAttribute UserUpdateRequest user, Authentication authentication) {
                BindingHelper bindingHelper = new BindingHelper();
                userService.validateUpdate(user, bindingHelper);
                if (bindingHelper.findErrors()) {
                        return ResponseHelper.preconditionMap("información no aceptada", bindingHelper.getData());
                }
                return userService.update(user, authentication.getPrincipal().toString());
        }

        @PutMapping(value = "/change/password")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Update your user's current password", description = "Submit your old password for verification and your new one for be changed", tags = {
                        "User" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Change your user's password", required = true, content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = ChangePwdRequest.class))), responses = {
                                        @ApiResponse(responseCode = "200", description = "the user password was changed successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<?> changePassword(@RequestBody ChangePwdRequest changePwd, BindingResult bindingResult,
                        Authentication authentication) {
                userService.validatePasswordChanged(changePwd, bindingResult);
                if (bindingResult.hasErrors()) {
                        return ResponseHelper.preconditionMap("información no aceptada",
                                        DataHelper.buildErrorFields(bindingResult));
                }
                return userService.changePassword(changePwd, authentication.getPrincipal().toString());
        }

        @PostMapping(value = "/hydration/data")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Hydrate your user data", description = "Hydrate your user data by your own token", tags = {
                        "User" }, responses = {
                                        @ApiResponse(responseCode = "200", description = "the user data was hydrated successfully", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<GeneralResponse> hydrationData(Authentication auth) {
                return userService.hydrationData(auth.getPrincipal().toString());
        }

        @PostMapping(value = "/list/referreds")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Obtain the list of the referreds", description = "Obtain the list of the referreds of the user", tags = {
                        "User" }, responses = {
                                        @ApiResponse(responseCode = "200", description = "The referreds have been recovered", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<GeneralResponse> listReferreds(Authentication auth) {
                return userService.listReferreds(auth.getPrincipal().toString());
        }

        @PostMapping(value = "/obtain/commissions")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Obtain the commissions of the user", description = "Obtain the commissions of the user", tags = {
                        "User" }, responses = {
                                        @ApiResponse(responseCode = "200", description = "The commissions have been recovered", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<GeneralResponse> obtainCommissions(Authentication auth) {
                return userService.obtainCommissions(auth.getPrincipal().toString());
        }

        @PostMapping(value = "/obtain/payments")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Obtain the payments of the user", description = "Obtain the payments of the user", tags = {
                        "User" }, responses = {
                                        @ApiResponse(responseCode = "200", description = "The payments have been recovered", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<GeneralResponse> obtainPayments(Authentication auth) {
                return userService.obtainPayments(auth.getPrincipal().toString());
        }

        @PostMapping(value = "/monthly/earnings")
        @PreAuthorize(value = "hasRole('USER')")
        @Operation(summary = "Obtain the monthly earnings of the user", description = "Obtain the monthly earnings of the user", tags = {
                        "User" }, responses = {
                                        @ApiResponse(responseCode = "200", description = "The monthly earnings of the user have been recovered", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class))),
                                        @ApiResponse(responseCode = "4XX", description = "General responses", content = @Content(mediaType = CONTENT_TYPE, schema = @Schema(implementation = GeneralResponse.class)))
                        }, parameters = {
                                        @Parameter(name = "Refresh-Token", in = ParameterIn.HEADER, description = "Token that allow you to update the credentials", required = true)
                        })
        public ResponseEntity<GeneralResponse> monthlyEarnings(Authentication auth) {
                return userService.monthlyEarnings(auth.getPrincipal().toString());
        }

}
