package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.data.*;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @PostMapping("/registration/start")
    public PublicKeyCredentialCreationOptions RegistrationStart(@RequestBody RegistrationDto dto) {
        try {
            log.info("Starting registration process for user: {}", dto.getUsername());
            var rp = new PublicKeyCredentialRpEntity("localhost", "localhost");

            UUID id = UUID.randomUUID();
            var response = getPublicKeyCredentialCreationOptions(dto, id, rp);
            log.info("Response about to send: {}", response);
            
            return response;
        } catch (Exception e) {
            log.info("Exception occurred: {}", e.getMessage());
            return null;
        }
    }

    private PublicKeyCredentialCreationOptions getPublicKeyCredentialCreationOptions(RegistrationDto dto, UUID id, PublicKeyCredentialRpEntity rp) {
        var bytes = id.toString().getBytes();
        var newUser = new PublicKeyCredentialUserEntity(bytes, dto.getUsername(), dto.getUsername());

        Challenge challenge = new DefaultChallenge();

        var pubKeyCredParams = new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY,
                COSEAlgorithmIdentifier.ES256
        );

        return new PublicKeyCredentialCreationOptions(
                rp,
                newUser,
                challenge,
                List.of(pubKeyCredParams),
                null,
                Collections.emptyList(),
                null,
                Collections.emptyList(),
                null,
                null
        );
    }
}
