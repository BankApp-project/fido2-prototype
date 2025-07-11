package online.bankapp.fido2.prototype.service;

import com.webauthn4j.credential.CredentialRecord;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.server.ServerProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public final class AuthService {

    public RegistrationParameters getRegistrationParameters(Challenge challenge) {
        ServerProperty serverProperty = getServerProperty(challenge);

        // expectations
        List<PublicKeyCredentialParameters> pubKeyCredParams = getPubKeyCredParamList();
        boolean userVerificationRequired = true;
        boolean userPresenceRequired = true;

        return new RegistrationParameters(
                serverProperty,
                pubKeyCredParams,
                userVerificationRequired,
                userPresenceRequired
        );
    }

    public AuthenticationParameters getAuthenticationParameters(Challenge challenge, CredentialRecord credentialRecord) {
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

    public List<PublicKeyCredentialParameters> getPubKeyCredParamList() {

        var keyParam1 = new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY,
                COSEAlgorithmIdentifier.ES256
        );

        var keyParam2 = new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY,
                COSEAlgorithmIdentifier.RS256
        );

        return List.of(keyParam1, keyParam2);
    }

    private ServerProperty getServerProperty(Challenge challenge) {
        return new ServerProperty(new Origin("http://localhost:8080"), "localhost", challenge);
    }

}
