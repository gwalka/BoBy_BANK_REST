package banking.boby.dto;

import banking.boby.entity.enums.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Schema(description = "DTO с информацией о карте пользователя")
public record CardUserDto(
        @Schema(description = "Уникальный идентификатор карты", example = "123")
        Long id,

        @Schema(description = "Маскированный номер карты", example = "1234 **** **** 5678")
        String maskedNum,

        @Schema(description = "Дата истечения срока действия карты", example = "2027-12-31")
        LocalDate expDate,

        @Schema(description = "Баланс на карте", example = "1500.50")
        BigDecimal balance,

        @Schema(description = "Статус карты")
        CardStatus status
) {}
