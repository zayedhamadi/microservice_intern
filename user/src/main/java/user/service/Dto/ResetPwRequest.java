package user.service.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPwRequest {

    @NotBlank(message = "Le token est obligatoire")
    private String token;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Minimum 8 caractères")
    private String password;

    @NotBlank(message = "La confirmation est obligatoire")
    private String confirmPassword;
}