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
public class createUserPerAdminDto {
    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50)
    String nom;

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50)
    String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    String email;

    String password;
    String matricule;

    @NotNull(message = "Le rôle est obligatoire")
    Role role;
}