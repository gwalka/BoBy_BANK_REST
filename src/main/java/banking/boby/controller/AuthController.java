package banking.boby.controller;

import banking.boby.dto.UserLoginDto;
import banking.boby.dto.UserRegisterDto;
import banking.boby.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @Operation(summary = "Регистрация пользователя",
            description = "Регистрирует нового пользователя. Может выбросить ошибку валидации данных.")
    @ApiResponse(responseCode = "200", description = "Успешная регистрация")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegisterDto dto) {
        service.registerUser(dto);
        return ResponseEntity.ok("Вы успешно зарегистрировались");
    }

    @Operation(summary = "Авторизация пользователя",
            description = "Выполняет вход пользователя и возвращает токен." +
                    "Может выбросить ошибку валидации данных или при неверных учетных данных.")
    @ApiResponse(responseCode = "200", description = "Успешный вход и получение токена")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации данных")
    @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginDto dto) {
        String token = service.login(dto);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
