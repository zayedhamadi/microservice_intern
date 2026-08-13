package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "enregistrerPosteRecrutement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class enregistrerPosteRecrutement {
    @Id
    String idEnregistrerPosteRecrutement;
    String idPosteRecrutement;
    @Indexed
    String recruteurKeycloakId;
    LocalDateTime dateEnregistrerPsoteRecrutement;
    
}
