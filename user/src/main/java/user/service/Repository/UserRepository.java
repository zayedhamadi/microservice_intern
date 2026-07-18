package user.service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import user.service.Entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.num_Tel = :numTel")
    Optional<User> findByNumTel(@Param("numTel") Integer numTel);

    Optional<User> findByEmail(String email);
    Optional<User> findByKeycloakId(String keycloakId);
    boolean existsByEmail(String email);
    boolean existsByKeycloakId(String keycloakId);

    @Query(nativeQuery = true, value =
            "SELECT id, nom, prenom, email, etat_compte, role, " +
                    "date_inscrit, image, num_tel " +
                    "FROM users " +
                    "WHERE ( " +
                    "  LOWER(nom)    LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  OR LOWER(prenom) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  OR LOWER(email)  LIKE LOWER(CONCAT('%', :q, '%')) " +
                    ") " +
                    "ORDER BY date_inscrit DESC")
    List<Object[]> searchUsers(@Param("q") String query);

    @Query(nativeQuery = true, value =
            "SELECT u.id, u.nom, u.prenom, u.email, u.etat_compte, u.role, " +
                    "u.date_inscrit, u.image, u.num_tel, " +
                    "c.motif_cessation, c.date_cessation " +
                    "FROM users u " +
                    "LEFT JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.etat_compte = 'INACTIF' " +
                    "ORDER BY u.date_inscrit DESC")
    List<Object[]> getAllInactifUsers();

    @Query(nativeQuery = true, value =
            "SELECT id, nom, prenom, email, etat_compte, role, " +
                    "date_inscrit, image, num_tel " +
                    "FROM users WHERE etat_compte != 'INACTIF' " +
                    "ORDER BY date_inscrit DESC LIMIT 5")
    List<Object[]> getStatisticsLast5InscriptionUser();

    @Query(nativeQuery = true, value =
            "SELECT id, nom, prenom, email, etat_compte, role, " +
                    "date_inscrit, image, num_tel " +
                    "FROM users WHERE etat_compte != 'INACTIF' " +
                    "ORDER BY date_inscrit DESC")
    List<Object[]> getAllActiveUsers();

    @Query(nativeQuery = true, value =
            "SELECT id, nom, prenom, email, etat_compte, role, date_inscrit, " +
                    "image, num_tel " +
                    "FROM users WHERE etat_compte != 'INACTIF' " +
                    "ORDER BY date_inscrit DESC " +
                    "LIMIT :size OFFSET :offset")
    List<Object[]> getAllActiveUsersPaged(@Param("offset") int offset, @Param("size") int size);

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte != 'INACTIF'")
    Long countAllActiveUsers();

    @Query(nativeQuery = true, value =
            "SELECT u.id, u.nom, u.prenom, u.email, u.etat_compte, u.role, " +
                    "u.date_inscrit, u.image, u.genre, u.adresse, u.description, " +
                    "u.num_tel, u.date_naissance, u.matricule, c.motif_cessation, " +
                    "c.date_cessation, c.motif_of_activer_compte " +
                    "FROM users u " +
                    "LEFT JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.id = :id")
    List<Object[]> findUserDetailById(@Param("id") Long id);






    @Query(nativeQuery = true, value =
            "SELECT u.id, u.nom, u.prenom, u.email, u.etat_compte, u.role, " +
                    "u.date_inscrit, u.image, u.genre, u.adresse, u.description, " +
                    "u.num_tel, u.date_naissance, u.matricule, u.linkedin, u.twitter, " +
                    "u.siteweb, u.specialite_etude, u.universite_etude, u.niveau_etude, " +
                    "u.annees_experience, c.motif_cessation, c.date_cessation, " +
                    "c.motif_of_activer_compte " +
                    "FROM users u " +
                    "LEFT JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.id = :id")
    List<Object[]> findUserDetailByIdAdmin(@Param("id") Long id);


    @Query(nativeQuery = true, value =
            "SELECT genre, COUNT(*) " +
                    "FROM users " +
                    "WHERE etat_compte != 'INACTIF' AND genre IS NOT NULL " +
                    "GROUP BY genre")
    List<Object[]> getStatisticsUserGener();

    @Query(nativeQuery = true, value =
            "SELECT role, genre, COUNT(*) AS total FROM users " +
                    "WHERE etat_compte != 'INACTIF' AND genre IS NOT NULL " +
                    "AND role IN ('RH', 'EMPLOYEE', 'CANDIDAT') " +
                    "GROUP BY role, genre")
    List<Object[]> getGenreByRole();

    @Query(nativeQuery = true, value =
            "SELECT role, etat_compte, COUNT(*) AS total FROM users " +
                    "WHERE role IN ('RH', 'EMPLOYEE', 'CANDIDAT') " +
                    "GROUP BY role, etat_compte")
    List<Object[]> getStatusByRole();

    // ── Totaux ──────────────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF'")
    Long getCountUsers();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE)")
    Long getCountUsersThisMonth();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)")
    Long getUsersLastMonth();

    // ── RH ───────────────────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF' AND role = 'RH'")
    Long getCountRHUsers();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'RH' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE)")
    Long getRHThisMonth();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'RH' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)")
    Long getRHLastMonth();

    // ── EMPLOYEE ─────────────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF' AND role = 'EMPLOYEE'")
    Long getCountEmployeeUsers();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE)")
    Long getEmployeeThisMonth();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'EMPLOYEE' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)")
    Long getEmployeeLastMonth();

    // ── CANDIDAT ─────────────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'ACTIF' AND role = 'CANDIDAT'")
    Long getCountCandidatUsers();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'CANDIDAT' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE)")
    Long getCandidatThisMonth();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE role = 'CANDIDAT' AND etat_compte = 'ACTIF' " +
                    "AND MONTH(date_inscrit) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) " +
                    "AND YEAR(date_inscrit)  = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)")
    Long getCandidatLastMonth();

    // ── INACTIFS ─────────────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users WHERE etat_compte = 'INACTIF'")
    Long getCountInactifUsers();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users u " +
                    "INNER JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.etat_compte = 'INACTIF' " +
                    "AND MONTH(c.date_cessation) = MONTH(CURRENT_DATE) " +
                    "AND YEAR(c.date_cessation)  = YEAR(CURRENT_DATE)")
    Long getInactifsThisMonth();

    @Query(nativeQuery = true, value =
            "SELECT COUNT(*) FROM users u " +
                    "INNER JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.etat_compte = 'INACTIF' " +
                    "AND MONTH(c.date_cessation) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) " +
                    "AND YEAR(c.date_cessation)  = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)")
    Long getInactifsLastMonth();

    // ── SÉRIES MENSUELLES ────────────────────────────────────

    @Query(nativeQuery = true, value =
            "SELECT role, MONTH(date_inscrit) AS mois, COUNT(*) AS total " +
                    "FROM users " +
                    "WHERE etat_compte = 'ACTIF' " +
                    "AND YEAR(date_inscrit) = YEAR(CURRENT_DATE) " +
                    "AND role IN ('RH', 'EMPLOYEE', 'CANDIDAT') " +
                    "GROUP BY role, MONTH(date_inscrit) " +
                    "ORDER BY mois")
    List<Object[]> getMonthlyRegistrations();

    @Query(nativeQuery = true, value =
            "SELECT MONTH(date_inscrit) AS mois, COUNT(*) AS total " +
                    "FROM users " +
                    "WHERE YEAR(date_inscrit) = YEAR(CURRENT_DATE) " +
                    "GROUP BY MONTH(date_inscrit) " +
                    "ORDER BY mois")
    List<Object[]> getMonthlyAllRegistrations();

    @Query(nativeQuery = true, value =
            "SELECT MONTH(c.date_cessation) AS mois, COUNT(*) AS total " +
                    "FROM users u " +
                    "INNER JOIN cessations c ON u.cessation_id = c.id " +
                    "WHERE u.etat_compte = 'INACTIF' " +
                    "AND YEAR(c.date_cessation) = YEAR(CURRENT_DATE) " +
                    "GROUP BY MONTH(c.date_cessation) " +
                    "ORDER BY mois")
    List<Object[]> getMonthlyCessations();
}