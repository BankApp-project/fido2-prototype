package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.*;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.verifier.exception.VerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bankapp.fido2.prototype.Repository.AuthRepository;
import online.bankapp.fido2.prototype.model.UserCredentials;
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
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final AuthRepository authRepo;

    @PostMapping("/challenge")
    public ResponseEntity<PublicKeyCredentialCreationOptions> RegistrationStart(@RequestBody RegistrationStartDto dto) {
        try {
            log.info("Starting registration process for user: {}", dto.getUsername());

            var response = getPublicKeyCredentialCreationOptions(dto.getUsername());
            log.info("Response about to send: {}", response);

            //store challenge in in-memory db
            //HashMap FTW
            UUID sessionUuid = UUID.randomUUID();
            if (!authRepo.addChallenge(sessionUuid, response.getChallenge())) {
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

    private PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions(String username) {
        var rp = new PublicKeyCredentialRpEntity("localhost", "localhost");

        UUID id = UUID.randomUUID();
        var idBytes = id.toString().getBytes();

        var newUser = new PublicKeyCredentialUserEntity(idBytes, username, username);

        Challenge challenge = new DefaultChallenge();

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
                PublicKeyCredentialsParametersProvider.getPubKeyCredParamList(),
                60000L, //60s
                excludeList,
                authenticatorSelection,
                Collections.emptyList(),
                AttestationConveyancePreference.NONE, //Controls how much information server wants to receive about the authenticator
                null
        );
    }
    @PostMapping("/registration/finish")
    public ResponseEntity<Void> RegistrationFinish(@RequestBody RegistrationFinishDto dto, @RequestHeader("Session-ID") UUID sessionUuid) {
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
        var registrationParameters = getRegistrationParameters(challenge);

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
        log.info("Registration/finish process completed for user: {}", dto.getUsername());
        log.info("RegCredentialId: {}", credentialId);
        log.info("Congrats! You are our {} new user", authRepo.credentialsSize());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private ServerProperty getServerProperty(Challenge challenge) {
        return new ServerProperty(new Origin("http://localhost:8080"), "localhost", challenge);
    }

    private RegistrationParameters getRegistrationParameters(Challenge challenge) {
        ServerProperty serverProperty = getServerProperty(challenge);

        // expectations
        List<PublicKeyCredentialParameters> pubKeyCredParams = PublicKeyCredentialsParametersProvider.getPubKeyCredParamList();
        boolean userVerificationRequired = true;
        boolean userPresenceRequired = true;

        return new RegistrationParameters(
                serverProperty,
                pubKeyCredParams,
                userVerificationRequired,
                userPresenceRequired
        );
    }

    private AuthenticationParameters getAuthenticationParameters(Challenge challenge, CredentialRecord credentialRecord) {
        ServerProperty serverProperty = getServerProperty(challenge);

        //expectations
        List<byte[]> allowedCredentialIds = null;
        boolean userVerificationRequired = false; //TODO: CHECK THIS FLAG!!! maybe it wont work without biometrics?
        boolean userPresenceRequired = true;

        return new AuthenticationParameters(
                serverProperty,
                credentialRecord,
                allowedCredentialIds,
                userVerificationRequired,
                userPresenceRequired
        );
    }

    @PostMapping("/auth")
    public ResponseEntity<Void> Login(@RequestBody AuthDto dto, @RequestHeader("Session-ID") UUID sessionUuid) {
        log.info("Starting authentication process for user: {}", dto.getUsername());
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

        AuthenticationParameters authenticationParameters = getAuthenticationParameters(challenge, credentialRecord);
        try {
            webAuthnManager.verify(authenticationData, authenticationParameters);
        } catch (VerificationException e) {
            log.warn("Error during authentication request validation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        //TODO: there should also be something like updating counter of the authenicatior record, why and how?
        log.info("Auth process finished for user: {}", dto.getUsername());
        return ResponseEntity.ok()
                .header("Session-ID", sessionUuid.toString())
                .build();
    }
}