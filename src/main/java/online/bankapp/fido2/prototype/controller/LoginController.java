package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.data.*;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.data.extension.client.AuthenticationExtensionClientInput;
import com.webauthn4j.data.extension.client.AuthenticationExtensionsClientInputs;
import com.webauthn4j.verifier.exception.VerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bankapp.fido2.prototype.Repository.AuthRepository;
import online.bankapp.fido2.prototype.controller.dto.AuthDto;
import online.bankapp.fido2.prototype.model.UserCredentials;
import online.bankapp.fido2.prototype.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/login")
public class LoginController {

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final AuthRepository authRepo;
    private final AuthService authService;

    @PostMapping("/")
    public ResponseEntity<String> Login(@RequestBody AuthDto dto, @RequestHeader("Session-ID") UUID sessionUuid) {
        log.info("Starting authentication process");
        log.info("DB SIZE: {}", authRepo.credentialsSize());
        AuthenticationData authenticationData;
        try {
            //use `webAuthnManager.verifyAuthenticationResponseJSON instead?
            authenticationData = webAuthnManager.parseAuthenticationResponseJSON(dto.getAuthenticationResponseJSON());
        } catch (Exception e) {
            log.info("Error parsing authentication response: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
        log.info("Auth CredentialId: {}", authenticationData.getCredentialId());
        Challenge challenge = authRepo.getChallenge(sessionUuid);
        if (challenge == null) {
            log.warn("Challenge doesn't exists!");
            return ResponseEntity.badRequest().build();
        }

        UserCredentials credentials = authRepo.loadCredentials(Arrays.hashCode(authenticationData.getCredentialId()));
        if (credentials == null) {
            log.warn("Credentials for given credentialId doesn't exists!");
            return ResponseEntity.badRequest().build();
        }
        CredentialRecord credentialRecord = credentials.getCredentialRecord();

        AuthenticationParameters authenticationParameters = authService.getAuthenticationParameters(challenge, credentialRecord);
        try {
            webAuthnManager.verify(authenticationData, authenticationParameters);
        } catch (VerificationException e) {
            log.warn("Error during authentication request validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        //TODO: there should also be updating counter of the authenicatior record
        log.info("Auth process finished for user: {}", credentials.getUsername());
        return ResponseEntity.ok()
                .header("Session-ID", sessionUuid.toString())
                .body(String.format("Welcome %s!", credentials.getUsername())); //there we will send jwt token instead
    }

    @GetMapping("/challenge/")
    public ResponseEntity<PublicKeyCredentialRequestOptions> AuthChallenge() {
        try {
            log.info("Starting auth process");
            Challenge challenge = new DefaultChallenge();
            PublicKeyCredentialRequestOptions response = getPublicKeyCredentialRequestOptions(challenge);
            UUID sessionUuid = UUID.randomUUID();
            authRepo.saveChallenge(sessionUuid, challenge);
            log.info("Auth process completed");
            return ResponseEntity.ok()
                    .header("Session-ID", sessionUuid.toString())
                    .body(response);
        } catch (Exception e) {
            log.info("Exception occurred during auth process: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    private PublicKeyCredentialRequestOptions getPublicKeyCredentialRequestOptions(Challenge challenge) {

        String rpId = "localhost";
        List<PublicKeyCredentialDescriptor> allowCredentials = Collections.emptyList();
        UserVerificationRequirement userVerification = UserVerificationRequirement.REQUIRED;
        List<PublicKeyCredentialHints> hints = Collections.emptyList();
        AuthenticationExtensionsClientInputs<AuthenticationExtensionClientInput> extensions = null;

        return new PublicKeyCredentialRequestOptions(
                challenge,
                30000L,
                rpId,
                allowCredentials,
                userVerification,
                hints,
                extensions
        );
    }
}
