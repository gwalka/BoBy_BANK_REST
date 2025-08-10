package banking.boby.controller;

import banking.boby.dto.BalanceResponseDto;
import banking.boby.dto.CardDigitsDto;
import banking.boby.dto.CardUserDto;
import banking.boby.dto.TransferRequestDto;
import banking.boby.service.UserCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserCardController {

    private final UserCardService userCardService;

    @Operation(summary = "Запрос на блокировку карты",
            description = "Отправляет запрос на блокировку карты. Может возникнуть ошибка при неправильной операции с картой или при отсутствии доступа.")
    @ApiResponse(responseCode = "200", description = "Запрос на блокировку карты выполнен")
    @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    @PostMapping("/{cardId}/block")
    public ResponseEntity<String> blockCardRequest(@PathVariable @NonNull Long cardId) {
        userCardService.blockCardRequest(cardId);
        return ResponseEntity.ok("Запрос на блокировку карты выполнен");
    }

    @Operation(summary = "Получить баланс карты",
            description = "Возвращает текущий баланс карты." +
                    "Может возникнуть ошибка при отсутствии доступа или если карта не найдена.")
    @ApiResponse(responseCode = "200", description = "Баланс карты успешно получен")
    @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable @NotNull Long cardId) {
        BalanceResponseDto balance = userCardService.getBalance(cardId);
        return ResponseEntity.ok(balance);
    }

    @Operation(summary = "Получить список своих карт",
            description = "Возвращает постраничный список карт пользователя.")
    @ApiResponse(responseCode = "200", description = "Список карт успешно получен")
    @GetMapping("/cards")
    public ResponseEntity<Page<CardUserDto>> getMyCards(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "2") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<CardUserDto> cardsPage = userCardService.getMyCards(pageable);
        return ResponseEntity.ok(cardsPage);
    }

    @Operation(summary = "Перевод средств",
            description = "Выполняет перевод средств с одной карты на другую в рамках одного аккаунта." +
                    "Может возникнуть ошибка при отсутствии доступа или небезопасной операции.")
    @ApiResponse(responseCode = "200", description = "Перевод успешно выполнен")
    @ApiResponse(responseCode = "403", description = "Доступ запрещён или небезопасная операция")
    @PostMapping("/transfer")
    public ResponseEntity<String> transferFunds(@Valid @RequestBody TransferRequestDto request) {
        userCardService.transferFunds(request);
        return ResponseEntity.ok("Перевод успешно выполнен");
    }

    @Operation(summary = "Получить полный номер карты",
            description = "Возвращает полный номер карты." +
                    "Может возникнуть ошибка, если карта не найдена или доступ запрещён.")
    @ApiResponse(responseCode = "200", description = "Полный номер карты успешно получен")
    @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    @GetMapping("/{cardId}/digits")
    public ResponseEntity<CardDigitsDto> getFullCardNumber(@PathVariable Long cardId) {
        return ResponseEntity.ok(userCardService.getFullCardNumber(cardId));
    }
}
