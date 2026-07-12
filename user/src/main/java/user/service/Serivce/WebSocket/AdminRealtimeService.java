package user.service.Serivce.WebSocket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import user.service.Mail.UserEmailService;
import user.service.Repository.CessationRepository;

@Service
@Slf4j
@AllArgsConstructor
public class AdminRealtimeService {
    CessationRepository cessationRepository;
    UserEmailService userEmailService;
}
