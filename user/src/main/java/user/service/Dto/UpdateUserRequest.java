package user.service.Dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import user.service.Entity.Enum.Genre;
import user.service.Entity.Enum.NiveauEtude;

import java.time.LocalDate;

@Getter
@Setter
@ToString(exclude = {"currentPassword", "newPassword", "confirmPassword"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {

    // --- Communs à tous les rôles ---
    String nom;
    String prenom;
    Genre genre;
    String adresse;
    String description;
    LocalDate dateNaissance;
    Integer num_Tel;
    Integer anneesExperience;

    String linkedin;
    String twitter;
    String siteweb;

    String imageBase64;

    // --- Spécifique CANDIDAT / EMPLOYEE (cf. User.requiresEtudes()) ---
    String specialiteEtude;
    String universiteEtude;
    NiveauEtude niveauEtude;


    // --- Mot de passe (optionnel) ---
    String currentPassword;
    String newPassword;
    String confirmPassword;
}