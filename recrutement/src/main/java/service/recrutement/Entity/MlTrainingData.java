package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "mlTrainingData")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MlTrainingData {

   @Id
   String idMlTrainingData;

   String applicationId; // maintenant réellement cohérent avec Application.idApplication (String)

   // Features ML
   double skillsMatch;
   double experienceMatch;
   int educationLevel;
   boolean contractMatch;          // binaire → boolean plutôt qu'int, plus clair à la lecture
   boolean languageMatch;
   boolean certificationMatch;     // NOTE : source de vérité = Certification (MySQL, user-service) — nécessitera un appel Feign ou une synchro dédiée au moment de la génération du dataset
   boolean locationMatch;
   boolean salaryExpectationMatch;

   // Label — résultat réel observé
   boolean accepted; // 1 = ACCEPTED, 0 = REJECTED → boolean, la conversion 0/1 se fait à l'entraînement, pas dans le modèle de données

   LocalDateTime createdAt;
}