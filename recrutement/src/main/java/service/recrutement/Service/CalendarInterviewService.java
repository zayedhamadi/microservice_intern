package service.recrutement.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import service.recrutement.Entity.CalendarInterview;
import service.recrutement.Entity.Enum.CalendarInterviewStatus;
import service.recrutement.Entity.dto.CalendarInterviewDto;
import service.recrutement.Entity.dto.CalendarInterviewStatsDto;
import service.recrutement.Exception.CandidatureNotFoundException;
import service.recrutement.Repository.CalendarInterviewRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarInterviewService {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private final CalendarInterviewRepository repository;

    public List<CalendarInterviewDto> getAll() {
        return repository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public CalendarInterviewDto getById(String id) {
        return toDto(repository.findById(id).orElseThrow(() -> new CandidatureNotFoundException(id)));
    }

    @Transactional
    public CalendarInterviewDto create(CalendarInterviewDto dto) {
        var now = LocalDateTime.now();
        var e = toEntity(dto);
        e.setId(null);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return toDto(repository.save(e));
    }

    @Transactional
    public CalendarInterviewDto update(String id, CalendarInterviewDto dto) {
        var ex = repository.findById(id).orElseThrow(() -> new CandidatureNotFoundException(id));
        var up = toEntity(dto);
        up.setId(ex.getId());
        up.setCreatedAt(ex.getCreatedAt());
        up.setUpdatedAt(LocalDateTime.now());
        return toDto(repository.save(up));
    }

    @Transactional
    public void delete(String id) {
        if (!repository.existsById(id)) throw new CandidatureNotFoundException(id);
        repository.deleteById(id);
    }

    public List<CalendarInterviewStatsDto> getStats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (var s : CalendarInterviewStatus.values()) counts.put(s.name(), 0L);
        counts.put("INCONNU", 0L);
        repository.findAll().forEach(i -> {
            String k = i.getStatus() != null ? i.getStatus().name() : "INCONNU";
            counts.merge(k, 1L, Long::sum);
        });
        return counts.entrySet().stream().map(e -> CalendarInterviewStatsDto.builder().status(e.getKey()).count(e.getValue()).build()).collect(Collectors.toList());
    }

    private CalendarInterviewDto toDto(CalendarInterview e) {
        return CalendarInterviewDto.builder().id(e.getId()).candidateName(e.getCandidateName()).candidateEmail(e.getCandidateEmail()).posteRecrutement(e.getPosteRecrutement()).posteId(e.getPosteId()).interviewerName(e.getInterviewerName()).interviewDate(e.getInterviewDate() != null ? e.getInterviewDate().toString() : null).startTime(e.getStartTime() != null ? e.getStartTime().format(TIME_FMT) : null).endTime(e.getEndTime() != null ? e.getEndTime().format(TIME_FMT) : null).mode(e.getMode()).location(e.getLocation()).meetingLink(e.getMeetingLink()).status(e.getStatus()).notes(e.getNotes()).createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null).updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null).build();
    }

    private CalendarInterview toEntity(CalendarInterviewDto dto) {
        return CalendarInterview.builder().id(dto.getId()).candidateName(dto.getCandidateName()).candidateEmail(dto.getCandidateEmail()).posteRecrutement(dto.getPosteRecrutement()).posteId(dto.getPosteId()).interviewerName(dto.getInterviewerName()).interviewDate(dto.getInterviewDate() != null ? LocalDate.parse(dto.getInterviewDate()) : null).startTime(dto.getStartTime() != null ? LocalTime.parse(dto.getStartTime(), TIME_FMT) : null).endTime(dto.getEndTime() != null ? LocalTime.parse(dto.getEndTime(), TIME_FMT) : null).mode(dto.getMode()).location(dto.getLocation()).meetingLink(dto.getMeetingLink()).status(dto.getStatus()).notes(dto.getNotes()).build();
    }
}