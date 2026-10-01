package com.mazurek.eventOrganizer.auth;


import com.mazurek.eventOrganizer.auth.dto.*;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeAddressUnavailableException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenInvalidException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final DeviceTypeResolver deviceTypeResolver;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegistrationResponse> registerNewUser(@Valid @RequestBody RegisterRequest registerRequest){
        authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistrationResponse.verificationRequired());

    }
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest,
                                                                   @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                                                   @RequestHeader(value = "X-Device-Type", required = false) String deviceTypeHeader
    ){
        DeviceType deviceType = deviceTypeResolver.determineDeviceType(deviceTypeHeader, userAgent);
        return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest, deviceType));
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        authenticationService.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshAccessToken(refreshTokenRequest));
    }

    @PostMapping("/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> generateNewActivationToken(@Valid @RequestBody EmailBasedRequest request){
            authenticationService.regenerateActivationTokenByUserEmail(request.getEmail());
            return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody EmailBasedRequest request) {
        authenticationService.requestPasswordReset(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> resetPassword(
            @PathVariable UUID tokenId,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authenticationService.resetPassword(tokenId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activate/{tokenId}")
    @ApiResponse(
            responseCode = "200",
            description = "Account activated, or an expired activation token replaced and a new email queued.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ActivationResponse.class))
    )
    public ResponseEntity<ActivationResponse> activateAccount(@PathVariable UUID tokenId) {
        return switch (authenticationService.activateAccount(tokenId)) {
            case ACTIVATED -> ResponseEntity.ok(ActivationResponse.activated());
            case TOKEN_EXPIRED_NEW_SENT -> ResponseEntity.ok(ActivationResponse.expiredResent());
            case INVALID_TOKEN -> throw new ActivationTokenNotFoundException();
        };
    }

    @PostMapping("/change-email/{tokenId}")
    @ApiResponse(
            responseCode = "200",
            description = "Email address changed and existing sessions invalidated.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = EmailChangeResponse.class))
    )
    @ApiResponse(responseCode = "410", description = "Email-change token expired.",
            content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/ApiError")))
    @ApiResponse(responseCode = "409", description = "The requested email address is unavailable.",
            content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/ApiError")))
    public ResponseEntity<EmailChangeResponse> confirmEmailChange(@PathVariable UUID tokenId) {
        // Map outcomes after the service transaction commits, preserving expired-token cleanup.
        return switch (authenticationService.confirmEmailChange(tokenId)) {
            case CHANGED -> ResponseEntity.ok(EmailChangeResponse.changed());
            case EXPIRED -> throw new EmailChangeTokenExpiredException();
            case INVALID_TOKEN -> throw new EmailChangeTokenInvalidException();
            case EMAIL_UNAVAILABLE -> throw new EmailChangeAddressUnavailableException();
        };
    }
}
