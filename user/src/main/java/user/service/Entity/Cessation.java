package user.service.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cessation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String motifCessation;

    LocalDate dateCessation;

    @Column(name = "motif_of_activer_compte")
    String motifReactivation;

    @OneToMany(mappedBy = "cessation", fetch = FetchType.LAZY)
    @Builder.Default
    List<User> users = new ArrayList<>();
}