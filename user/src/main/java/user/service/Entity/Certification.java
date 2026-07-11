package user.service.Entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Certification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long idCertification;

    String titre;

    @Column(columnDefinition = "TEXT")
    String description;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDate dateCertif;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    byte[] pdfCertif;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}