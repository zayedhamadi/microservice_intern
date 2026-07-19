package user.service.Dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    String password;
}