package user.service.Dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {

    String accessToken;
    String refreshToken;
    String tokenType;
    Long expiresIn;
    String email;
    String nom;
    String prenom;
    String role;
    Long id;
    String keycloakId;
}