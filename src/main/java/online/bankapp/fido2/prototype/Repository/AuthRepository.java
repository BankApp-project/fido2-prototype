package online.bankapp.fido2.prototype.Repository;

import com.webauthn4j.data.client.challenge.Challenge;
import online.bankapp.fido2.prototype.model.UserCredentials;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.UUID;

@Repository
public class AuthRepository {
    private final HashMap<UUID, Challenge> challenges;
    private final HashMap<Integer, UserCredentials> credentials;

    public AuthRepository() {
        challenges = new HashMap<>();
        credentials = new HashMap<>();
    }

    public int credentialsSize() {
        return credentials.size();
    }

    /**
     * Adds a credential to the repository if it does not already exist.
     *
     * @param credentialId the unique identifier for the credentials, represented as a byte array
     * @param credentials the credentials to be added, encapsulating user and authentication details
     * @return true if the credential was successfully added, false if it already exists
     */
    public boolean saveCredential(int credentialId, UserCredentials credentials) {
        return this.credentials.putIfAbsent(credentialId, credentials) == null;
    }

    public UserCredentials loadCredentials(int credentialId) {
        return this.credentials.get(credentialId);
    }

    /**
     * Adds a challenge to the repository if it does not already exist for the provided UUID.
     *
     * @param uuid the unique identifier associated with the session for which the challenge is being stored
     * @param challenge the challenge to be stored, typically used for authentication or registration processes
     * @return true if the challenge was successfully added, false if a challenge already exists for the given UUID
     */
    public boolean saveChallenge(UUID uuid, Challenge challenge) {
        return this.challenges.putIfAbsent(uuid, challenge) == null;
    }

    /**
     * Retrieves a challenge associated with the provided session UUID.
     *
     * @param uuid the unique identifier for the session used to locate the associated challenge
     * @return the {@link Challenge} associated with the given session UUID, or null if no challenge exists for the provided UUID
     */
    public Challenge getChallenge(UUID uuid) {
        return challenges.get(uuid);
    }
}
