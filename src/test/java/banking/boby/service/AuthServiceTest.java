package banking.boby.service;

import banking.boby.dto.UserLoginDto;
import banking.boby.dto.UserRegisterDto;
import banking.boby.entity.User;
import banking.boby.entity.enums.Role;
import banking.boby.exception.DataValidationException;
import banking.boby.repository.UserRepository;
import banking.boby.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void positiveRegisterUser() {
        UserRegisterDto dto = getRegisterDto();

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userRepository.existsByUsername(dto.username())).thenReturn(false);

        authService.registerUser(dto);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void negativeRegisterEmailExists() {
        UserRegisterDto dto = getRegisterDto();

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThrows(DataValidationException.class, () -> authService.registerUser(dto));

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void negativeRegisterUsernameExists() {
        UserRegisterDto dto = getRegisterDto();

        when(userRepository.existsByUsername(dto.username())).thenReturn(true);

        assertThrows(DataValidationException.class, () -> authService.registerUser(dto));

        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    void positiveLogin() {
        UserLoginDto loginDto = new UserLoginDto("benask", "password");
        User user = User.builder()
                .password("password")
                .username("benask")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("benask")).thenReturn(Optional.ofNullable(user));
        assert user != null;
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), anyString())).thenReturn("mocked-jwt-token");

        assertDoesNotThrow(() -> authService.login(loginDto));

        verify(userRepository).findByUsername("benask");
        verify(passwordEncoder).matches(loginDto.password(), user.getPassword());

    }

    @Test
    void negativeLoginWrongUsername() {
        UserLoginDto loginDto = new UserLoginDto("wrongUser", "password");

        when(userRepository.findByUsername("wrongUser")).thenReturn(Optional.empty());

        DataValidationException ex = assertThrows(DataValidationException.class,
                () -> authService.login(loginDto));

        assertEquals("Неверный логин или пароль", ex.getMessage());
        verify(userRepository).findByUsername("wrongUser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    @Test
    void negativeLoginWrongPassword() {
        UserLoginDto loginDto = new UserLoginDto("benask", "wrongPassword");
        User user = User.builder()
                .password("password")
                .username("benask")
                .role(Role.USER)
                .build();

        when(userRepository.findByUsername("benask")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDto.password(), user.getPassword())).thenReturn(false);

        DataValidationException ex = assertThrows(DataValidationException.class,
                () -> authService.login(loginDto));

        assertEquals("Неверный логин или пароль", ex.getMessage());
        verify(userRepository).findByUsername("benask");
        verify(passwordEncoder).matches("wrongPassword", "password");
        verify(jwtTokenProvider, never()).generateToken(anyLong(), anyString());
    }

    private UserRegisterDto getRegisterDto() {
        UserRegisterDto dto = UserRegisterDto.builder()
                .email("test@test.com")
                .firstName("ben")
                .lastName("askren")
                .password("password")
                .username("banask")
                .build();
        return dto;
    }

}
