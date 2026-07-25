package service.recrutement.Entity.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class fileUSerDto {
    boolean exists;
    String cvFileName;
    String cvBase64;
}