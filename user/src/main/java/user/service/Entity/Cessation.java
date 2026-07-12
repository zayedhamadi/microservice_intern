package user.service.Entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "cessations")
public class Cessation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String motifCessation;

    LocalDate dateCessation;

    @OneToMany(mappedBy = "cessation", fetch = FetchType.LAZY)
    List<User> users;
}