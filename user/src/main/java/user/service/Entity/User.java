    package user.service.Entity;

    import jakarta.persistence.*;
    import lombok.*;
    import lombok.experimental.FieldDefaults;
    import org.hibernate.annotations.CreationTimestamp;
    import user.service.Entity.Enum.Compte;
    import user.service.Entity.Enum.Genre;
    import user.service.Entity.Enum.NiveauEtude;
    import user.service.Entity.Enum.Role;

    import java.time.LocalDate;


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

        @Column(unique = true, nullable = false)
        String email;

        @Column(nullable = false)
        String nom;

        @Column(nullable = false)
        String prenom;

        String adresse;
        String description;

        @Column(length = 100)
        String linkedin, twitter, siteweb;
        String specialiteEtude, universiteEtude, codePostal;


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

    }