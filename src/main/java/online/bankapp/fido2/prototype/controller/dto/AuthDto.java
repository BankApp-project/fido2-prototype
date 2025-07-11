package online.bankapp.fido2.prototype.controller.dto;

import lombok.Data;

//TODO: switch to record
@Data
public class AuthDto {
    private String authenticationResponseJSON;
}
