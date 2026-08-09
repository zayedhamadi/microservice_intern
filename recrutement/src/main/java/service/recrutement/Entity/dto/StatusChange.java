package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import service.recrutement.Entity.Enum.ApplicationStatus;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusChange {
    ApplicationStatus statut;
    LocalDateTime date;
    String commentaire;
    String auteurKeycloakId; 
}