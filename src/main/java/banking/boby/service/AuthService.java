package banking.boby.service;

import banking.boby.dto.UserLoginDto;
import banking.boby.dto.UserRegisterDto;
import banking.boby.entity.User;
import banking.boby.entity.enums.Role;
import banking.boby.exception.DataValidationException;
import banking.boby.repository.UserRepository;
import banking.boby.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void registerUser(UserRegisterDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new DataValidationException("Email %s уже зарегистрирован", dto.email());
        }

        if (userRepository.existsByUsername(dto.username())) {
            throw new DataValidationException("Username %s уже занят", dto.username());
        }

        userRepository.save(mapToEntity(dto));
    }

    public String login(UserLoginDto dto) {
        User user = userRepository.findByUsername(dto.username())
                .orElseThrow(() -> new DataValidationException("Неверный логин или пароль"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new DataValidationException("Неверный логин или пароль");
        }
        return jwtTokenProvider.generateToken(user.getId(), user.getRole().toString());
    }

    private User mapToEntity(UserRegisterDto dto) {
        return User.builder()
                .username(dto.username())
                .email(dto.email())
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .password(passwordEncoder.encode(dto.password()))
                .role(Role.USER)
                .build();

    }



}
