package com.logistics.auth.service;

import com.logistics.auth.constant.MessageCode;
import com.logistics.auth.dto.AuthDTOs.AuthResponse;
import com.logistics.auth.exception.AccountInactiveException;
import com.logistics.auth.model.User;
import com.logistics.auth.repository.AuthAuditLogRepository;
import com.logistics.auth.repository.CourierProfileRepository;
import com.logistics.auth.repository.MerchantProfileRepository;
import com.logistics.auth.repository.RoleRepository;
import com.logistics.auth.repository.UserRepository;
import com.logistics.auth.security.KeycloakClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CourierProfileRepository courierProfileRepository;

    @Mock
    private MerchantProfileRepository merchantProfileRepository;

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthAuditLogRepository auditLogRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .username("john_doe")
                .email("john.doe@example.com")
                .googleId("google-sub-12345")
                .fullName("John Doe")
                .role(User.UserRole.ROLE_CUSTOMER)
                .active(true)
                .mfaEnabled(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully login existing user by Google ID")
    void testSuccessfulGoogleLogin_ExistingUser_ByGoogleId() {
        when(userRepository.findByGoogleId("google-sub-12345")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(roleRepository.findByCodeWithPermissions("ROLE_CUSTOMER")).thenReturn(Optional.empty());
        when(messageService.getMessage(MessageCode.SUCCESS)).thenReturn("Success");

        AuthResponse response = googleOAuthService.processGoogleUser(
                "google-sub-12345", "john.doe@example.com", "John Doe Updated", "http://avatar.url/img.png", request);

        assertNotNull(response);
        assertEquals("ROLE_CUSTOMER", response.getRole());
        assertEquals("john.doe@example.com", response.getEmail());

        verify(userRepository).save(existingUser);
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Should link Google ID to existing account matching email without creating duplicate account")
    void testSuccessfulGoogleLogin_ExistingUser_ByEmail_LinksGoogleId() {
        User userWithoutGoogleId = User.builder()
                .id(UUID.randomUUID())
                .username("jane_smith")
                .email("jane.smith@example.com")
                .googleId(null)
                .fullName("Jane Smith")
                .role(User.UserRole.ROLE_CUSTOMER)
                .active(true)
                .build();

        when(userRepository.findByGoogleId("new-google-sub-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("jane.smith@example.com")).thenReturn(Optional.of(userWithoutGoogleId));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByCodeWithPermissions("ROLE_CUSTOMER")).thenReturn(Optional.empty());
        when(messageService.getMessage(MessageCode.SUCCESS)).thenReturn("Success");

        AuthResponse response = googleOAuthService.processGoogleUser(
                "new-google-sub-789", "jane.smith@example.com", "Jane Smith", "http://photo.com/pic.jpg", request);

        assertNotNull(response);
        assertEquals("new-google-sub-789", userWithoutGoogleId.getGoogleId());
        assertEquals("jane.smith@example.com", response.getEmail());
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    @DisplayName("Should create new user with ROLE_CUSTOMER when user does not exist")
    void testSuccessfulGoogleLogin_NewUserCreation() {
        when(userRepository.findByGoogleId("google-sub-brand-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newbie@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-random-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(roleRepository.findByCodeWithPermissions("ROLE_CUSTOMER")).thenReturn(Optional.empty());
        when(messageService.getMessage(MessageCode.SUCCESS)).thenReturn("Success");

        AuthResponse response = googleOAuthService.processGoogleUser(
                "google-sub-brand-new", "newbie@example.com", "Newbie Developer", "http://avatar.com/new.png", request);

        assertNotNull(response);
        assertEquals("newbie@example.com", response.getEmail());
        assertEquals("ROLE_CUSTOMER", response.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertEquals("google-sub-brand-new", createdUser.getGoogleId());
        assertEquals("newbie", createdUser.getUsername());
        assertEquals(User.UserRole.ROLE_CUSTOMER, createdUser.getRole());
        assertTrue(createdUser.getActive());
    }

    @Test
    @DisplayName("Should generate unique username on username collision for new user")
    void testSuccessfulGoogleLogin_UsernameCollision_GeneratesUnique() {
        when(userRepository.findByGoogleId("google-sub-collision")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("alex@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("alex")).thenReturn(true);
        when(userRepository.existsByUsername("alex_1")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(roleRepository.findByCodeWithPermissions("ROLE_CUSTOMER")).thenReturn(Optional.empty());
        when(messageService.getMessage(MessageCode.SUCCESS)).thenReturn("Success");

        AuthResponse response = googleOAuthService.processGoogleUser(
                "google-sub-collision", "alex@example.com", "Alex", null, request);

        assertNotNull(response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, atLeastOnce()).save(userCaptor.capture());
        assertEquals("alex_1", userCaptor.getValue().getUsername());
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when account is deactivated")
    void testGoogleLogin_InactiveAccount_ThrowsAccountInactiveException() {
        existingUser.setActive(false);
        when(userRepository.findByGoogleId("google-sub-12345")).thenReturn(Optional.of(existingUser));
        when(messageService.getMessage(MessageCode.ACCOUNT_INACTIVE)).thenReturn("Account is deactivated");

        assertThrows(AccountInactiveException.class, () ->
                googleOAuthService.processGoogleUser("google-sub-12345", "john.doe@example.com", "John", null, request));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when Google email is null or empty")
    void testGoogleLogin_MissingOrInvalidEmail_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                googleOAuthService.processGoogleUser("sub-123", null, "Name", null, request));

        assertThrows(IllegalArgumentException.class, () ->
                googleOAuthService.processGoogleUser("sub-123", "   ", "Name", null, request));

        verifyNoInteractions(userRepository);
    }
}
