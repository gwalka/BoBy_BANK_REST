package banking.boby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Данные для регистрации нового пользователя")
public record UserRegisterDto(
        @Size(max = 64)
        @Email
        @Schema(description = "Электронная почта пользователя", example = "user@example.com")
        String email,

        @Size(max = 32)
        @NotBlank
        @Schema(description = "Имя пользователя", example = "Иван")
        String firstName,

        @Size(max = 32)
        @NotBlank
        @Schema(description = "Фамилия пользователя", example = "Иванов")
        String lastName,

        @Size(max = 64)
        @NotBlank
        @Schema(description = "Уникальное имя пользователя", example = "ivan123")
        String username,

        @Size(min = 8, max = 64)
        @NotBlank
        @Schema(description = "Пароль пользователя", example = "P@ssw0rd123")
        String password
) {
}
