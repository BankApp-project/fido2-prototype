package online.bankapp.fido2.prototype.model;

import java.util.UUID;

public class User {

    UUID userHandle;
    String username;

    public User(String username) {
        this.username = username;
        this.userHandle = UUID.randomUUID();
    }

    public String getUsername() {
        return username;
    }

    public UUID getUserHandle() {
        return userHandle;
    }
}
