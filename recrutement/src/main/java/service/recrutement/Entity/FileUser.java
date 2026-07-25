package service.recrutement.Entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fileUser")
public class FileUser {

    @Id
    String idFileUser;

    @Indexed(unique = true)
    String keycloakId;

    byte[] cvUser;

    String cvFileName;
}