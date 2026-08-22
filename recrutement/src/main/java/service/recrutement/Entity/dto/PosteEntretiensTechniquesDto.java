package service.recrutement.Entity.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteEntretiensTechniquesDto {
    private String posteId;
    private String posteTitre;
    private String departementNom;
    private long nombreCandidats;

    @Builder.Default
    private List<CandidatEntretienTechniqueDto> candidats = new ArrayList<>();
}