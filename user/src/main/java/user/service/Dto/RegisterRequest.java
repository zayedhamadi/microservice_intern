package user.service.Dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import user.service.Entity.Enum.Role;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50)
    String nom;

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50)
    String prenom;

    @NotBlank
    @Email(message = "Format d'email invalide")
    String email;

    @NotBlank
    @Size(min = 8, max = 64)
    String password;

    @NotNull(message = "Le rôle est requis")
    Role role;
}