package user.service.Dto;


import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import user.service.Entity.Enum.Genre;
import user.service.Entity.Enum.Role;


import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {

    @NotBlank(message = "Le nom est obligatoire")
    String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Minimum 8 caractères")
    String password;

    @NotNull(message = "Le rôle est obligatoire")
    Role role;

    Genre genre;
    String adresse;
    String description;
    LocalDate dateNaissance;
    Integer num_Tel;
}