package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificationDto {
    String idCertification;
    String titre;
    String description;
    LocalDate dateCertif;
    String pdfBase64; // input : base64 brut ou data URI ; output : toujours data URI complet
}