package user.service.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import user.service.Entity.Enum.Compte;
import user.service.Entity.Enum.Genre;
import user.service.Entity.Enum.NiveauEtude;
import user.service.Entity.Enum.Role;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String keycloakId;

    @Lob
    byte[] image;

    @Lob
    byte[] cvUser;

    @Column(unique = true, nullable = false)
    String email;

    @Column(nullable = false)
    String nom;

    @Column(nullable = false)
    String prenom;

    String adresse;
    String description;

    @Column(length = 100)
    String linkedin, twitter, siteweb, specialiteEtude, universiteEtude;

    @Enumerated(EnumType.STRING)
    NiveauEtude niveauEtude;

    @Builder.Default
    Integer anneesExperience = 0;

    LocalDate dateNaissance;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDate dateInscrit;

    @Column(name = "num_tel")
    Integer num_Tel;

    @Enumerated(EnumType.STRING)
    Role role;

    @Enumerated(EnumType.STRING)
    Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    Compte etatCompte = Compte.ACTIF;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Certification> certifications = new ArrayList<>();

    @Transient
    public boolean isRoleAssigned() {
        return role != null;
    }

    /** true si ce rôle nécessite un CV et un cursus académique. */
    @Transient
    public boolean requiresEtudes() {
        return role == Role.CANDIDAT || role == Role.EMPLOYEE;
    }

    @Transient
    public List<String> getMissingFields() {
        List<String> missing = new ArrayList<>();

        if (role == null) {
            missing.add("role");
            return missing;
        }

        if (isBlank(nom)) missing.add("nom");
        if (isBlank(prenom)) missing.add("prenom");
        if (isBlank(adresse)) missing.add("adresse");
        if (num_Tel == null) missing.add("num_Tel");
        if (dateNaissance == null) missing.add("dateNaissance");
        if (genre == null) missing.add("genre");

        if (requiresEtudes()) {
            if (isBlank(specialiteEtude)) missing.add("specialiteEtude");
            if (niveauEtude == null) missing.add("niveauEtude");
            // CV volontairement optionnel : on ne bloque pas le profil dessus
        }

        return missing;
    }

    @Transient
    public boolean isProfileComplete() {
        return getMissingFields().isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}