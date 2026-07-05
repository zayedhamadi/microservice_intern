package user.service.Dto;


import lombok.*;
import lombok.experimental.FieldDefaults;
import user.service.Entity.Enum.Genre;
import user.service.Entity.Enum.Role;


import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUSerAfterConnect {
    Long id;
    String email;
    Role role;
    Genre genre;
    String adresse;
    String nom;
    String prenom;
    String description;
    LocalDate dateNaissance;
    Integer num_Tel;
    String imageBase64;
    //  sociaux
     String linkedin, twitter,  siteweb,specialiteEtude,universiteEtude, niveauEtude;

     Integer anneesExperience;

}