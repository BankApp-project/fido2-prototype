package online.bankapp.fido2.prototype.model;

import com.webauthn4j.credential.CredentialRecord;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserCredentials {
    private String username;
    private CredentialRecord credentialRecord;
    private byte[] credentialId;
    private LocalDateTime createdAt;

    public UserCredentials(String username, CredentialRecord credentialRecord, byte[] credentialId) {
        this.username = username;
        this.credentialRecord = credentialRecord;
        this.credentialId = credentialId;
        this.createdAt = LocalDateTime.now();
    }
}
