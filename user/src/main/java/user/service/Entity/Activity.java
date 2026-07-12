// Entity/Activity.java
package user.service.Entity;

import jakarta.persistence.*;
import lombok.*;
import user.service.Entity.Enum.ActivityType;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    ActivityType type;

    String actorPrenom;
    String actorNom;
    String role;
    String motif;

    @Column(length = 500)
    String message;

    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();
}