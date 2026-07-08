package user.service.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgetPwRequest {
    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;
}
