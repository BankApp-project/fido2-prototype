package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;

import java.util.List;

public final class PublicKeyCredentialsParametersProvider {
    public static List<PublicKeyCredentialParameters> getPubKeyCredParamList() {

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
}
