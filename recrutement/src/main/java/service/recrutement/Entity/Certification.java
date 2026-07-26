package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "certifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certification {

    @Id
    String idCertification;

    @Indexed
    String keycloakId;

    String titre;
    String description;

    @Builder.Default
    LocalDate dateCertif = LocalDate.now();

    byte[] pdfCertif;

    LocalDateTime createdAt;
}