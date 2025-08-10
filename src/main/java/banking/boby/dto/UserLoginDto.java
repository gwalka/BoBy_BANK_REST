package banking.boby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Данные для авторизации пользователя")
public record UserLoginDto(
        @NotBlank
        @Size(max = 64)
        @Schema(description = "Имя пользователя", example = "user123", required = true)
        String username,

        @NotBlank
        @Size(min = 8, max = 64)
        @Schema(description = "Пароль пользователя", example = "P@ssw0rd123", required = true)
        String password
) {}
