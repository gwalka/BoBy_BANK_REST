package banking.boby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Запрос на перевод средств между картами одного аккаунта")
public record TransferRequestDto(
        @NotNull(message = "Укажите карту списания")
        @Schema(description = "ID карты, с которой будут списаны деньги", example = "1001", required = true)
        Long fromCardId,

        @NotNull(message = "Укажите карту для получения")
        @Schema(description = "ID карты, на которую будут зачислены деньги", example = "1002", required = true)
        Long toCardId,

        @NotNull(message = "Введите сумму")
        @Positive(message = "Сумма должны быть положительной")
        @Schema(description = "Сумма перевода", example = "500.00", required = true)
        BigDecimal amount
) {}
