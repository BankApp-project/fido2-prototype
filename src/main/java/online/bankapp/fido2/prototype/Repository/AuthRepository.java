package online.bankapp.fido2.prototype.Repository;

import com.webauthn4j.data.client.challenge.Challenge;
import org.springframework.stereotype.Repository;

import java.util.HashMap;

@Repository
public class AuthRepository {
    private HashMap<String, Challenge> challenges;

    public AuthRepository() {
        challenges = new HashMap<>();
    }

    /**
     *
     * @param username
     * @param challenge
     * @return false if username already taken, true if operations was successful
     */
    public boolean addChallenge(String username, Challenge challenge) {
        return this.challenges.putIfAbsent(username, challenge) == null;
    }


    /**
     * Checks whether the provided challenge matches the stored challenge for the given username.
     *
     * @param username the username associated with the challenge to be compared
     * @param challenge the challenge to be checked against the stored challenge
     * @return true if the provided challenge matches the stored challenge for the username, false otherwise
     */
    public boolean challengeMatches(String username, Challenge challenge) {
        return this.challenges.get(username).equals(challenge);
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
