package service.recrutement.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import service.recrutement.Client.UserServiceClient;
import service.recrutement.Entity.Enum.StatusPosteRecrutement;
import service.recrutement.Entity.PosteRecrutement;
import service.recrutement.Entity.dto.CandidatDto;
import service.recrutement.Entity.dto.DepartementDto;
import service.recrutement.Entity.dto.PosteRecrutementDto;
import service.recrutement.Mail.RecrutementMail;
import service.recrutement.Repository.ApplicationRepository;
import service.recrutement.Repository.PosteRecrutementRepository;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplyService {
    private final ApplicationRepository applicationRepository;


}
