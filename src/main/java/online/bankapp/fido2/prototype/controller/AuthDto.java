package online.bankapp.fido2.prototype.controller;

import lombok.Data;

//TODO: switch to record
@Data
public class AuthDto {
    private String username;
    private String authenticationResponseJSON;
}
