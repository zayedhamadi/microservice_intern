package user.service.Dto;



import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificationDTO {

    Long idCertification;
    String titre, description,pdfBase64;
    LocalDate dateCertif;
}