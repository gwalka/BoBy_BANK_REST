package banking.boby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Информация о балансе по карте")
public record BalanceResponseDto(
        @Schema(description = "Идентификатор карты", example = "12345")
        Long cardId,

        @Schema(description = "Замаскированный номер карты", example = "**** **** **** 1234")
        String maskedNum,

        @Schema(description = "Текущий баланс карты", example = "15000.75")
        BigDecimal balance
) {}
