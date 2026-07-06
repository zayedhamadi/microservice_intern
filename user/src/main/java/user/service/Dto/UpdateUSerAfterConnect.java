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

    Role role;
    Genre genre;
    String adresse, nom, prenom, description;
    LocalDate dateNaissance;
    Integer num_Tel;
    String email, imageBase64, linkedin, twitter, siteweb, specialiteEtude, universiteEtude, niveauEtude;

    Integer anneesExperience;

}