package user.service.Dto;



import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginEventPayload {
    Long userId;
    String prenom;
    String nom;
    String action;
    String email;
    LocalDateTime timestamp;
}