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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    //check how to use `WebAuthnManager`
    //im not sure this static constructor is fine. Didn't check it yet.
    //to continue: https://webauthn4j.github.io/webauthn4j/en/#registering-the-webauthn-public-key-credential-on-the-server
    private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
    private final AuthRepository authRepo;

    @PostMapping("/registration/start")
    public PublicKeyCredentialCreationOptions RegistrationStart(@RequestBody RegistrationStartDto dto) {
        try {
            log.info("Starting registration process for user: {}", dto.getUsername());
            var rp = new PublicKeyCredentialRpEntity("localhost", "localhost");

            UUID id = UUID.randomUUID();
            var response = getPublicKeyCredentialCreationOptions(dto, id, rp);
            log.info("Response about to send: {}", response);

            //store challenge in in-memory db/session
            authRepo.addChallenge(dto.getUsername(), response.getChallenge());

            return response;
        } catch (Exception e) {
            log.info("Exception occurred: {}", e.getMessage());
            return null;
        }
    }

    //Return viable HTTP CODES
    @PostMapping("/registration/finish")
    public String RegistrationFinish(@RequestBody RegistrationFinishDto dto) {

        RegistrationData registrationData = null;
        try {
            registrationData = webAuthnManager.parseRegistrationResponseJSON(dto.getRegistrationResponseJSON());
        } catch (Exception e) {
            log.info("Error parsing registration response: {}", e.getMessage());
            return null;
        }

        // Server properties
        Origin origin = new Origin("http://localhost");
        String rpId = "localhost";
        Challenge challenge = authRepo.getChallenge(dto.getUsername());
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge);
        if (challenge == null) {
            log.warn("Challenge doesn't exists!");
            return null;
        }

        // expectations
        var pubKeyCredParams = List.of(PublicKeyCredentialsParametersProvider.getPubKeyCredParam());
         /* U SURE? */
        boolean userVerificationRequired = true;
        boolean userPresenceRequired = true;

        var registrationParameters = new RegistrationParameters(serverProperty, pubKeyCredParams, userVerificationRequired, userPresenceRequired);

        try {
            //verify if request is from valid source
            webAuthnManager.verify(registrationData, registrationParameters);
        } catch (VerificationException e) {
            log.warn("Error during registration request validation!");
            return null;
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

        if (!authRepo.saveCredential(credentialId, newCredentials)) {
            log.warn("This credentialId already exists in DB!");
            return null;
        }

        return null;
    }

    private PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions(RegistrationStartDto dto, UUID id, PublicKeyCredentialRpEntity rp) {
        var bytes = id.toString().getBytes();
        var newUser = new PublicKeyCredentialUserEntity(bytes, dto.getUsername(), dto.getUsername());

        Challenge challenge = new DefaultChallenge();


        return new PublicKeyCredentialCreationOptions(
                rp,
                newUser,
                challenge,
                List.of(PublicKeyCredentialsParametersProvider.getPubKeyCredParam()),
                null,
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                null
        );
    }
}
