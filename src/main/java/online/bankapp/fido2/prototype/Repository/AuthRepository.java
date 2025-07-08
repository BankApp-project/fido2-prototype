package online.bankapp.fido2.prototype.Repository;

import com.webauthn4j.data.client.challenge.Challenge;
import online.bankapp.fido2.prototype.model.UserCredentials;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

@Repository
public class AuthRepository {
    private final HashMap<String, Challenge> challenges;
    private final HashMap<byte[], UserCredentials> credentials;

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
    public boolean saveCredential(byte[] credentialId, UserCredentials credentials) {
        return this.credentials.putIfAbsent(credentialId, credentials) == null;
    }

    public UserCredentials loadCredentials(byte[] credentialId) {
        return this.credentials.get(credentialId);
    }

    /**
     * Adds a challenge associated with a specific username to the repository if it does not already exist.
     *
     * @param username the username to associate the challenge with
     * @param challenge the challenge to be stored
     * @return true if the challenge was successfully added, false if a challenge already exists for the given username
     */
    public boolean addChallenge(String username, Challenge challenge) {
        return this.challenges.putIfAbsent(username, challenge) == null;
    }

    /**
     * Retrieves the challenge associated with the given username.
     *
     * @param username the username whose associated challenge is to be retrieved
     * @return the challenge associated with the specified username, or null if no challenge exists for the username
     */
    public Challenge getChallenge(String username) {
        return challenges.get(username);
    }
}
