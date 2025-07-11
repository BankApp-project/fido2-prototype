package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.verifier.exception.VerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bankapp.fido2.prototype.Repository.AuthRepository;
import online.bankapp.fido2.prototype.controller.dto.RegistrationFinishDto;
import online.bankapp.fido2.prototype.controller.dto.RegistrationStartDto;
import online.bankapp.fido2.prototype.model.UserCredentials;
import online.bankapp.fido2.prototype.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final AuthRepository authRepo;
    private final AuthService authService;

    @PostMapping("/registration/challenge")
    public ResponseEntity<PublicKeyCredentialCreationOptions> RegistrationStart(@RequestBody RegistrationStartDto dto) {
        try {
            log.info("Starting registration process for user: {}", dto.getUsername());

            Challenge challenge = new DefaultChallenge();
            var response = getPublicKeyCredentialCreationOptions(dto.getUsername(), challenge);

            //store challenge in-memory
            UUID sessionUuid = UUID.randomUUID();
            if (!authRepo.saveChallenge(sessionUuid, challenge)) {
                log.warn("Challenge already exists in DB!");
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            log.info("Registration/start process completed for user: {}", dto.getUsername());
            return ResponseEntity.ok()
                    .header("Session-ID", sessionUuid.toString())
                    .body(response);
        } catch (Exception e) {
            log.info("Exception occurred: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions(String username, Challenge challenge) {
        var rp = new PublicKeyCredentialRpEntity("localhost", "localhost");

        UUID id = UUID.randomUUID();
        var idBytes = id.toString().getBytes();

        var newUser = new PublicKeyCredentialUserEntity(idBytes, username, username);

        //in prod this should be list of credentials that given user has
        List<PublicKeyCredentialDescriptor> excludeList = Collections.emptyList();

        AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(
                null,
                ResidentKeyRequirement.REQUIRED, // REQUIRED for true passwordless experience, PREFERRED for compatibility
                UserVerificationRequirement.REQUIRED // User verification REQUIRED for biometric security
        );
        return new PublicKeyCredentialCreationOptions(
                rp,
                newUser,
                challenge,
                authService.getPubKeyCredParamList(),
                60000L, //60s
                excludeList,
                authenticatorSelection,
                Collections.emptyList(),
                AttestationConveyancePreference.NONE, //Controls how much information server wants to receive about the authenticator
                null
        );
    }


    @PostMapping("/registration/finish")
    public ResponseEntity<String> RegistrationFinish(@RequestBody RegistrationFinishDto dto, @RequestHeader("Session-ID") UUID sessionUuid) {
        RegistrationData registrationData;
        try {
            registrationData = webAuthnManager.parseRegistrationResponseJSON(dto.getRegistrationResponseJSON());
        } catch (Exception e) {
            log.info("Error parsing registration response: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
        Challenge challenge = authRepo.getChallenge(sessionUuid);
        if (challenge == null) {
            log.warn("Challenge doesn't exists!");
            return ResponseEntity.badRequest().build();
        }
        var registrationParameters = authService.getRegistrationParameters(challenge);

        try {
            //verify if request is from valid source
            webAuthnManager.verify(registrationData, registrationParameters);
        } catch (VerificationException e) {
            log.warn("Error during registration request validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        CredentialRecord credentialRecord =
                new CredentialRecordImpl(
                        registrationData.getAttestationObject(),
                        registrationData.getCollectedClientData(),
                        registrationData.getClientExtensions(),
                        registrationData.getTransports()
                );
        byte[] credentialId =
                credentialRecord
                        .getAttestedCredentialData()
                        .getCredentialId();

        UserCredentials newCredentials = new UserCredentials(
                dto.getUsername(),
                credentialRecord,
                credentialId);

        if (!authRepo.saveCredential(Arrays.hashCode(credentialId), newCredentials)) {
            log.warn("This credentialId already exists in DB!");
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        ResponseCookie knownDevice = ResponseCookie.from("knownDevice", "true")
                .httpOnly(false)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .sameSite("Lax")
                .build();

        log.info("Registration/finish process completed for user: {}", dto.getUsername());
        log.info("RegCredentialId: {}", credentialId);
        log.info("Congrats! You are our {} new user", authRepo.credentialsSize());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, knownDevice.toString())
                .body(String.format("Welcome %s!", dto.getUsername())); //there we will send jwt token instead
    }

}