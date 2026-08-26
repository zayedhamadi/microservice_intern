package user.service.Serivce.Admin;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.service.Repository.UserRepository;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserStatistics {

    UserRepository userRepository;

    public Map<String, List<Integer>> getMonthlyInscrVsCessation() {
        int[] inscriptions = new int[12];
        int[] cessations   = new int[12];

        for (Object[] row : userRepository.getMonthlyAllRegistrations()) {
            int mois  = ((Number) row[0]).intValue() - 1;
            int total = ((Number) row[1]).intValue();
            inscriptions[mois] = total;
        }
        for (Object[] row : userRepository.getMonthlyCessations()) {
            int mois  = ((Number) row[0]).intValue() - 1;
            int total = ((Number) row[1]).intValue();
            cessations[mois] = total;
        }

        Map<String, List<Integer>> result = new LinkedHashMap<>();
        result.put("inscriptions", toIntList(inscriptions));
        result.put("cessations",   toIntList(cessations));
        return result;
    }

    public Map<String, List<Integer>> getMonthlyRegistrations() {
        Map<String, int[]> raw = new LinkedHashMap<>();
        raw.put("RH",        new int[12]);
        raw.put("EMPLOYEE",  new int[12]);
        raw.put("CANDIDAT",  new int[12]);

        for (Object[] row : userRepository.getMonthlyRegistrations()) {
            String role  = (String) row[0];
            int    mois  = ((Number) row[1]).intValue() - 1;
            long   total = ((Number) row[2]).longValue();
            if (raw.containsKey(role)) raw.get(role)[mois] = (int) total;
        }

        Map<String, List<Integer>> result = new LinkedHashMap<>();
        raw.forEach((role, arr) -> result.put(role, toIntList(arr)));
        return result;
    }

    private Map<String, Object> buildStats(Long total, Long thisMonth, Long lastMonth) {
        long delta = (thisMonth != null ? thisMonth : 0L) - (lastMonth != null ? lastMonth : 0L);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total",     total     != null ? total     : 0L);
        s.put("delta",     delta);
        s.put("thisMonth", thisMonth != null ? thisMonth : 0L);
        s.put("lastMonth", lastMonth != null ? lastMonth : 0L);
        return s;
    }

    public Map<String, Object> getUsersStats() {
        return buildStats(
                userRepository.getCountUsers(),
                userRepository.getCountUsersThisMonth(),
                userRepository.getUsersLastMonth()
        );
    }

    public Map<String, Object> getRHStats() {
        return buildStats(
                userRepository.getCountRHUsers(),
                userRepository.getRHThisMonth(),
                userRepository.getRHLastMonth()
        );
    }

    public Map<String, Object> getEmployeeStats() {
        return buildStats(
                userRepository.getCountEmployeeUsers(),
                userRepository.getEmployeeThisMonth(),
                userRepository.getEmployeeLastMonth()
        );
    }

    public Map<String, Object> getCandidatsStats() {
        return buildStats(
                userRepository.getCountCandidatUsers(),
                userRepository.getCandidatThisMonth(),
                userRepository.getCandidatLastMonth()
        );
    }

    public Map<String, Object> getInactifsStats() {
        return buildStats(
                userRepository.getCountInactifUsers(),
                userRepository.getInactifsThisMonth(),
                userRepository.getInactifsLastMonth()
        );
    }

    public Map<String, Long> getStatisticsUserGener() {
        return userRepository.getStatisticsUserGener().stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue()
                ));
    }

    public Long getCountUsers()          { return userRepository.getCountUsers(); }
    public Long getCountRHUsers()        { return userRepository.getCountRHUsers(); }
    public Long getCountEmployeeUsers()  { return userRepository.getCountEmployeeUsers(); }
    public Long getCountCandidatUsers()  { return userRepository.getCountCandidatUsers(); }
    public Long getCountInactifUsers()   { return userRepository.getCountInactifUsers(); }

    public List<Map<String, Object>> getLast5InscriptionUsers() {
        return mapUsers(userRepository.getStatisticsLast5InscriptionUser());
    }

    public List<Map<String, Object>> getAllActiveUsers() {
        return mapUsers(userRepository.getAllActiveUsers());
    }

    public List<Map<String, Object>> getAllActiveUsersPaged(int offset, int size) {
        return mapUsers(userRepository.getAllActiveUsersPaged(offset, size));
    }

    public Long countAllActiveUsers() {
        return userRepository.countAllActiveUsers();
    }

    public List<Map<String, Object>> searchUsers(String query) {
        return mapUsers(userRepository.searchUsers(query));
    }



public Map<String, Object> findUserDetailByIdAdmin(Long id) {
    List<Object[]> rows = userRepository.findUserDetailById(id);
    if (rows.isEmpty()) throw new RuntimeException("User not found: " + id);

Object[] row = rows.get(0);
    log.info("Row content: {}", Arrays.toString(row));
    Map<String, Object> u = new LinkedHashMap<>();
    u.put("id", safeGet(row, 0));
    u.put("nom", safeGet(row, 1));
    u.put("prenom", safeGet(row, 2));
    u.put("email", safeGet(row, 3));
    u.put("etatCompte", safeGet(row, 4));
    u.put("role", safeGet(row, 5));
    u.put("dateInscrit", safeGet(row, 6) != null ? safeGet(row, 6).toString() : null);
    u.put("image", toBase64Image(safeGet(row, 7)));
    u.put("genre", safeGet(row, 8));
    u.put("adresse", safeGet(row, 9));
    u.put("description", safeGet(row, 10));
    u.put("numTel", safeGet(row, 11));
    u.put("dateNaissance", safeGet(row, 12) != null ? safeGet(row, 12).toString() : null);
    u.put("matricule", safeGet(row, 13));

    u.put("motifCessation", safeGet(row, 14));
    u.put("dateCessation", safeGet(row, 15) != null ? safeGet(row, 15).toString() : null);
    u.put("motifReactivation", safeGet(row, 16));
    return u;
}

    public Map<String, Object> getUserById(Long id) {
        List<Object[]> rows = userRepository.findUserDetailById(id);
        if (rows.isEmpty()) throw new RuntimeException("User not found: " + id);

        Object[] row = rows.get(0);
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("id",                safeGet(row, 0));
        u.put("nom",               safeGet(row, 1));
        u.put("prenom",            safeGet(row, 2));
        u.put("email",             safeGet(row, 3));
        u.put("etatCompte",        safeGet(row, 4));
        u.put("role",              safeGet(row, 5));
        u.put("dateInscrit",       safeGet(row, 6) != null ? safeGet(row, 6).toString() : null);
        u.put("image",             toBase64Image(safeGet(row, 7)));
        u.put("genre",             safeGet(row, 8));
        u.put("adresse",           safeGet(row, 9));
        u.put("description",       safeGet(row, 10));
        u.put("numTel",            safeGet(row, 11));
        u.put("dateNaissance",     safeGet(row, 12) != null ? safeGet(row, 12).toString() : null);
        u.put("matricule",         safeGet(row, 13));
        u.put("motifCessation",    safeGet(row, 14));
        u.put("dateCessation",     safeGet(row, 15) != null ? safeGet(row, 15).toString() : null);
        u.put("motifReactivation", safeGet(row, 16));
        return u;
    }

    public List<Map<String, Object>> getAllInactifUsers() {
        return userRepository.getAllInactifUsers().stream().map(row -> {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id",             safeGet(row, 0));
            u.put("nom",            safeGet(row, 1));
            u.put("prenom",         safeGet(row, 2));
            u.put("email",          safeGet(row, 3));
            u.put("etatCompte",     safeGet(row, 4));
            u.put("role",           safeGet(row, 5));
            u.put("dateInscrit",    safeGet(row, 6) != null ? safeGet(row, 6).toString() : null);
            u.put("image",          toBase64Image(safeGet(row, 7)));
            u.put("numTel",         safeGet(row, 8));
            u.put("motifCessation", safeGet(row, 9));
            u.put("dateCessation",  safeGet(row, 10) != null ? safeGet(row, 10).toString() : null);
            return u;
        }).collect(Collectors.toList());
    }

    public Map<String, Map<String, Long>> getStatusByRole() {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (Object[] row : userRepository.getStatusByRole()) {
            result.computeIfAbsent((String) row[0], k -> new LinkedHashMap<>())
                    .put((String) row[1], ((Number) row[2]).longValue());
        }
        return result;
    }

    public Map<String, Map<String, Long>> getGenreByRole() {
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (Object[] row : userRepository.getGenreByRole()) {
            result.computeIfAbsent((String) row[0], k -> new LinkedHashMap<>())
                    .put((String) row[1], ((Number) row[2]).longValue());
        }
        return result;
    }

    private Object safeGet(Object[] row, int index) {
        if (row == null || index >= row.length) return null;
        return row[index];
    }

    private List<Map<String, Object>> mapUsers(List<Object[]> list) {
        return list.stream().map(row -> {
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("id",          safeGet(row, 0));
            user.put("nom",         safeGet(row, 1));
            user.put("prenom",      safeGet(row, 2));
            user.put("email",       safeGet(row, 3));
            user.put("etatCompte",  safeGet(row, 4));
            user.put("role",        safeGet(row, 5));
            Object dateVal = safeGet(row, 6);
            user.put("dateInscrit", dateVal != null ? dateVal.toString() : null);
            user.put("image",       toBase64Image(safeGet(row, 7)));
            user.put("numTel",      safeGet(row, 8));
            return user;
        }).collect(Collectors.toList());
    }

    private List<Integer> toIntList(int[] arr) {
        List<Integer> list = new ArrayList<>(arr.length);
        for (int v : arr) list.add(v);
        return list;
    }

    private String toBase64Image(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String) {
            String s = (String) raw;
            if (s.isEmpty()) return null;
            if (s.startsWith("data:")) return s;
            return "data:image/jpeg;base64," + s;
        }
        if (raw instanceof byte[]) {
            byte[] bytes = (byte[]) raw;
            if (bytes.length == 0) return null;
            String mime = isPng(bytes) ? "image/png" : "image/jpeg";
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        if (raw instanceof Blob) {
            try {
                Blob blob  = (Blob) raw;
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                if (bytes.length == 0) return null;
                String mime = isPng(bytes) ? "image/png" : "image/jpeg";
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            } catch (SQLException e) {
                log.error("Erreur lecture Blob image : {}", e.getMessage());
                return null;
            }
        }
        log.warn("toBase64Image : type inattendu = {}", raw.getClass().getName());
        return null;
    }

    private boolean isPng(byte[] b) {
        return b.length >= 4
                && b[0] == (byte) 0x89 && b[1] == (byte) 0x50
                && b[2] == (byte) 0x4E && b[3] == (byte) 0x47;
    }
}