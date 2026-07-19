package user.service.Service.Admin;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.service.Dto.CessationDTO;
import user.service.Entity.Cessation;
import user.service.Entity.Enum.Compte;
import user.service.Entity.User;
import user.service.Mail.UserEmailService;
import user.service.Repository.CessationRepository;
import user.service.Repository.UserRepository;
import user.service.Serivce.Admin.CessationService;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CessationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CessationRepository cessationRepository;
    @Mock
    private UserEmailService userEmailService;
    @Mock
    private AdminRealtimeService realtimeService;
    @Mock
    private UserFinderService userFinderService;

    @InjectMocks
    private CessationService cessationService;

    @Test
    void cesserUser_devraitDesactiverUnCompteActif() {
        User user = User.builder()
                .id(1L)
                .prenom("Jean")
                .nom("Dupont")
                .email("jean@test.com")
                .etatCompte(Compte.ACTIF)
                .build();
        when(userFinderService.byId(1L)).thenReturn(user);

        Cessation savedCessation = Cessation.builder()
                .id(10L)
                .motifCessation("Démission")
                .dateCessation(LocalDate.now())
                .build();
        when(cessationRepository.saveAndFlush(any(Cessation.class))).thenReturn(savedCessation);

        CessationDTO dto = CessationDTO.builder()
                .motifCessation("Démission")
                .build();

        cessationService.cesserUser(1L, dto);

        assertThat(user.getEtatCompte()).isEqualTo(Compte.INACTIF);
        assertThat(user.getCessation()).isEqualTo(savedCessation);
        verify(userRepository).save(user);
    }

    @Test
    void cesserUser_devraitRejeterUnCompteDejaInactif() {
        User user = User.builder()
                .id(1L)
                .etatCompte(Compte.INACTIF)
                .build();
        when(userFinderService.byId(1L)).thenReturn(user);

        CessationDTO dto = CessationDTO.builder()
                .motifCessation("Démission")
                .build();

        assertThatThrownBy(() -> cessationService.cesserUser(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà inactif");

        verify(cessationRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void reactiverUser_devraitReactiverUnCompteInactif() {
        User user = User.builder()
                .id(1L)
                .prenom("Jean")
                .nom("Dupont")
                .email("jean@test.com")
                .etatCompte(Compte.INACTIF)
                .build();
        when(userFinderService.byId(1L)).thenReturn(user);

        cessationService.reactiverUser(1L);

        assertThat(user.getEtatCompte()).isEqualTo(Compte.ACTIF);
        verify(userRepository).save(user);
        verify(userEmailService).sendReactivationEmail("jean@test.com", "Jean");
    }

    @Test
    void reactiverUser_devraitRejeterUnCompteDejaActif() {
        User user = User.builder()
                .id(1L)
                .etatCompte(Compte.ACTIF)
                .build();
        when(userFinderService.byId(1L)).thenReturn(user);

        assertThatThrownBy(() -> cessationService.reactiverUser(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("déjà actif");

        verify(userRepository, never()).save(any());
        verify(userEmailService, never()).sendReactivationEmail(any(), any());
    }
}