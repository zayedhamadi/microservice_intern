package user.service.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartementDTO {
    Long id;
    String nom;
    String description;
    LocalDate dateCreation;

    public DepartementDTO(String nom, String description) {
        this.nom = nom;
        this.description = description;
    }
}