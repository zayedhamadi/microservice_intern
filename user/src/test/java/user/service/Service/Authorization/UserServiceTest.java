package user.service.Service.Authorization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import user.service.Dto.AuthResponse;
import user.service.Dto.LoginRequest;
import user.service.Dto.RegisterRequest;
import user.service.Entity.Enum.Compte;
import user.service.Entity.Enum.Role;
import user.service.Entity.User;
import user.service.Repository.UserRepository;
import user.service.Serivce.Authorization.KeycloakService;
import user.service.Serivce.Authorization.UserService;
import user.service.Serivce.FileService;
import user.service.Serivce.UserCommun.UserFinderService;
import user.service.Serivce.WebSocket.AdminRealtimeService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private UserFinderService userFinderService;
    @Mock
    private FileService fileService;
    @Mock
    private AdminRealtimeService realtimeService;

    @InjectMocks
    private UserService userService;

    // ---------- SIGNUP ----------

    @Test
    void register_devraitCreerUnUtilisateur() {
        RegisterRequest dto = RegisterRequest.builder()
                .email("test@test.com")
                .nom("jenkins")
                .prenom("test")
                .password("Test123@.")
                .role(Role.CANDIDAT)
                .build();

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(keycloakService.createUserInKeycloak(dto)).thenReturn("kc-uuid-123");

        User savedUser = User.builder()
                .id(1L)
                .keycloakId("kc-uuid-123")
                .email(dto.getEmail())
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .role(Role.CANDIDAT)
                .etatCompte(Compte.ACTIF)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.register(dto);

        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getKeycloakId()).isEqualTo("kc-uuid-123");
        assertThat(result.getRole()).isEqualTo(Role.CANDIDAT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_devraitRejeterLeRoleEmployee() {
        RegisterRequest dto = RegisterRequest.builder()
                .email("test@test.com")
                .role(Role.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("EMPLOYEE");

        verifyNoInteractions(keycloakService);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_devraitRejeterEmailDejaUtilise() {
        RegisterRequest dto = RegisterRequest.builder()
                .email("existe@test.com")
                .role(Role.CANDIDAT)
                .build();

        when(userRepository.existsByEmail("existe@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email déjà utilisé");

        verify(keycloakService, never()).createUserInKeycloak(any(RegisterRequest.class));
        verify(userRepository, never()).save(any());
    }

    // ---------- SIGNIN ----------

    @Test
    void login_devraitReussirPourUnCompteActif() {
        LoginRequest dto = LoginRequest.builder()
                .email("test@test.com")
                .password("Test123@.")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@test.com")
                .nom("jenkins")
                .prenom("test")
                .role(Role.CANDIDAT)
                .keycloakId("kc-uuid-123")
                .etatCompte(Compte.ACTIF)
                .build();
        when(userFinderService.byEmail("test@test.com")).thenReturn(user);

        AuthResponse keycloakResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-123")
                .tokenType("Bearer")
                .expiresIn(300L)
                .build();
        when(keycloakService.login(dto)).thenReturn(keycloakResponse);

        AuthResponse result = userService.login(dto);

        assertThat(result.getAccessToken()).isEqualTo("access-token-123");
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getRole()).isEqualTo("CANDIDAT");
    }

    @Test
    void login_devraitRejeterUnCompteInactif() {
        LoginRequest dto = LoginRequest.builder()
                .email("test@test.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .email("test@test.com")
                .etatCompte(Compte.INACTIF)
                .build();
        when(userFinderService.byEmail("test@test.com")).thenReturn(user);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("désactivé");

        verify(keycloakService, never()).login(any());
    }
}