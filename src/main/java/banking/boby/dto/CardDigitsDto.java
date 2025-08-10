package banking.boby.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "DTO, содержащий полный номер карты")
public record CardDigitsDto(
        @Schema(description = "Уникальный идентификатор карты", example = "123")
        Long id,

        @Schema(description = "Полный номер карты", example = "1234 5678 9012 3456")
        String fullNumber
) {}
