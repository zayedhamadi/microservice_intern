package service.recrutement.Entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosteCandidateStatsDto {

    private long totalOuverts;

    private List<PosteCandidateItemDto> derniersPostes;
}