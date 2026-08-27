package service.recrutement.Service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import service.recrutement.Entity.Application;
import service.recrutement.Entity.Interview;
import service.recrutement.Entity.Enum.InterviewStatus;
import service.recrutement.Entity.Enum.InterviewType;
import service.recrutement.Entity.dto.InterviewDto;
import service.recrutement.Entity.dto.InterviewStatsDto;
import service.recrutement.Entity.dto.PageResponseDto;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Repository.InterviewRepository;
import service.recrutement.Repository.PosteRecrutementRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CandidatService {

    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    static final int MAX_PAGE_SIZE = 100;
    static final int DEFAULT_PAGE_SIZE = 10;

    final ApplicationRepository applicationRepository;
    final InterviewRepository interviewRepository;
    final PosteRecrutementRepository posteRecrutementRepository;
    final MongoTemplate mongoTemplate;

    // ==================== 1. Liste paginée avec filtres + recherche ====================

    public PageResponseDto<InterviewDto> getMyListInterview(
            String candidatKeycloakId,
            InterviewType type,
            InterviewStatus status,
            String search,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        Sort sort = buildSort(sortBy, sortDir);
        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        Criteria criteria = Criteria.where("candidatKeycloakId").is(candidatKeycloakId);

        if (type != null) {
            criteria.and("type").is(type);
        }
        if (status != null) {
            criteria.and("statut").is(status);
        }

        Query baseQuery = new Query(criteria);

        if (StringUtils.hasText(search)) {
            String regex = Pattern.quote(search.trim());
            baseQuery.addCriteria(new Criteria().orOperator(
                    Criteria.where("posteRecrutement").regex(regex, "i"),
                    Criteria.where("interviewerName").regex(regex, "i"),
                    Criteria.where("lieu").regex(regex, "i"),
                    Criteria.where("notes").regex(regex, "i")
            ));
        }

        long total = mongoTemplate.count(baseQuery, Interview.class);

        Query pagedQuery = Query.of(baseQuery).with(pageable);
        List<Interview> results = mongoTemplate.find(pagedQuery, Interview.class);

        List<InterviewDto> dtos = results.stream().map(this::mapToDto).collect(Collectors.toList());
        return PageResponseDto.from(new PageImpl<>(dtos, pageable, total));
    }

    // ==================== 2. Détail d'un entretien précis (avec vérif d'appartenance) ====================

    public InterviewDto getMyInterviewById(String interviewId, String candidatKeycloakId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entretien introuvable"));

        if (!candidatKeycloakId.equals(interview.getCandidatKeycloakId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cet entretien ne vous appartient pas");
        }
        return mapToDto(interview);
    }

    // ==================== 3. Entretiens par type (sans pagination, pratique pour un badge/onglet) ====================

    public List<InterviewDto> getMyInterviewsByType(String candidatKeycloakId, InterviewType type) {
        return interviewRepository.findByCandidatKeycloakIdAndType(candidatKeycloakId, type)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ==================== 4. Statistiques par statut d'entretien (pour un dashboard candidat) ====================

    public List<InterviewStatsDto> getMyInterviewStats(String candidatKeycloakId) {
        List<Interview> mine = interviewRepository.findByCandidatKeycloakId(candidatKeycloakId);
        return mine.stream()
                .collect(Collectors.groupingBy(Interview::getStatut, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> InterviewStatsDto.builder()
                        .status(e.getKey() != null ? e.getKey().name() : "INCONNU")
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 5. Répartition des candidatures par statut (ApplicationStatus) ====================

    public List<InterviewStatsDto> getMyApplicationStatusBreakdown(String candidatKeycloakId) {
        List<Application> mine = applicationRepository.findByCandidatKeycloakId(candidatKeycloakId);
        return mine.stream()
                .collect(Collectors.groupingBy(Application::getStatut, Collectors.counting()))
                .entrySet()
                .stream()
                .map(e -> InterviewStatsDto.builder()
                        .status(e.getKey() != null ? e.getKey().name() : "INCONNU")
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 6. Prochain entretien à venir ====================

    public InterviewDto getMyNextUpcomingInterview(String candidatKeycloakId) {
        return interviewRepository
                .findFirstByCandidatKeycloakIdAndDateEntretienAfterOrderByDateEntretienAsc(
                        candidatKeycloakId, LocalDateTime.now())
                .map(this::mapToDto)
                .orElse(null);
    }

    // ==================== 7. Tous mes entretiens (sans pagination, ex. export ou vue calendrier complète) ====================

    public List<InterviewDto> getMyListInterview(String candidatKeycloakId) {
        return interviewRepository.findByCandidatKeycloakId(candidatKeycloakId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // ==================== Mapping Interview -> InterviewDto ====================

    private InterviewDto mapToDto(Interview i) {
        return InterviewDto.builder()
                .id(i.getIdInterview())
                .source(i.getSource())
                .applicationId(i.getApplicationId())
                .candidatKeycloakId(i.getCandidatKeycloakId())
                .candidateName(i.getCandidateName())
                .candidateEmail(i.getCandidateEmail())
                .posteRecrutement(i.getPosteRecrutement())
                .posteId(i.getPosteId())
                .recruteurKeycloakId(i.getRecruteurKeycloakId())
                .interviewerName(i.getInterviewerName())
                .type(i.getType())
                .interviewDate(i.getDateEntretien() != null ? i.getDateEntretien().format(DATE_FMT) : null)
                .startTime(i.getDateEntretien() != null ? i.getDateEntretien().format(TIME_FMT) : null)
                .endTime(i.getDateFinEntretien() != null ? i.getDateFinEntretien().format(TIME_FMT) : null)
                .mode(i.getMode())
                .location(i.getLieu())
                .meetingLink(i.getLienVisio())
                .status(i.getStatut())
                .resultat(i.getResultat())
                .notes(i.getNotes())
                .createdAt(i.getDateCreation() != null ? i.getDateCreation().toString() : null)
                .updatedAt(i.getDateModification() != null ? i.getDateModification().toString() : null)
                .build();
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = StringUtils.hasText(sortBy) ? sortBy : "dateEntretien";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}