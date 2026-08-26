package user.service.Dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    String keycloakId;
    String nom;
    String prenom;
    String email;
    String adresse;
    Integer numTel;
    String specialiteEtude;
    String niveauEtude;
    String universiteEtude;
    Integer anneesExperience;
    String linkedin;
    String matricule;
    String role;          // AJOUTÉ
    String compte;         // AJOUTÉ
    String imageBase64;    // AJOUTÉ
}