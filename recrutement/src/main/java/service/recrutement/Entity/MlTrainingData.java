package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;


import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "MlTrainingData")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MlTrainingData {

   @Id
   String idMlTrainingData;

   String applicationId;

   double skillsMatch;
   double experienceMatch;

   /**
    * Encodage ordinal du niveau d'étude, cohérent avec NiveauEtude (user-service) :
    * 0 = LICENCE, 1 = MASTER, 2 = INGENIEUR, 3 = DOCTORAT, 4 = AUTRE
    */
   int educationLevel;

   boolean contractMatch;
   boolean languageMatch;
   boolean certificationMatch;
   boolean locationMatch;
   boolean salaryExpectationMatch;

   boolean accepted;

   LocalDateTime createdAt;
}