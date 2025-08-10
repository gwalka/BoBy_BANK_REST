package banking.boby.dto;

import banking.boby.entity.enums.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

@Builder
@Schema(description = "Информация о карте для администратора")
public record CardAdminDto(
        @Schema(description = "Уникальный идентификатор карты", example = "101")
        Long id,

        @Schema(description = "Замаскированный номер карты", example = "**** **** **** 5678")
        String maskedNum,

        @Schema(description = "Идентификатор пользователя, которому принадлежит карта", example = "202")
        Long userId,

        @Schema(description = "Дата окончания действия карты", example = "2028-08-31")
        LocalDate expDate,

        @Schema(description = "Статус карты")
        CardStatus status
) {}
