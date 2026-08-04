package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatDto {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
}