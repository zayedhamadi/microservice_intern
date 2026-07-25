package user.service.Dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.*;
import lombok.experimental.FieldDefaults;
import user.service.Entity.Enum.Genre;
import user.service.Entity.Enum.Role;

import java.time.LocalDate;

@Getter
@Setter(AccessLevel.PUBLIC)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUSerAfterConnect {
    Long id;

    @Setter(AccessLevel.NONE) // empêche Lombok de générer un setRole(Role) qui entrerait en conflit
    Role role;

    Genre genre;
    LocalDate dateNaissance;

    String adresse, nom, prenom, description,
            email, imageBase64, linkedin, twitter, siteweb,
            specialiteEtude, universiteEtude, niveauEtude, cvBase64;

    Integer num_Tel, anneesExperience;

    @JsonSetter("role")
    public void setRole(String role) {
        if (role == null || role.isBlank()) {
            this.role = null;
            return;
        }
        try {
            this.role = Role.valueOf(role.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide : " + role);
        }
    }
}