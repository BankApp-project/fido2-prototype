package online.bankapp.fido2.prototype.controller.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationFinishDto {
    private String username;
    private String registrationResponseJSON;
}
