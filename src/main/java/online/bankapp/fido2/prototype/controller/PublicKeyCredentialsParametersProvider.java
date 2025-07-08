package online.bankapp.fido2.prototype.controller;

import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;

public final class PublicKeyCredentialsParametersProvider {
    public static PublicKeyCredentialParameters getPubKeyCredParam() {

        return new PublicKeyCredentialParameters(
                PublicKeyCredentialType.PUBLIC_KEY,
                COSEAlgorithmIdentifier.ES256
        );
    }
}
