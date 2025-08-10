package banking.boby.controller;

import banking.boby.dto.CardAdminDto;
import banking.boby.service.AdminCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("admin/cards/")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardController {

    private final AdminCardService adminCardService;

    @Value("${app.pagination.page-size}")
    private int pageSize;

    @Operation(summary = "Создать новую карту для пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "404", description = "Не найден пользователь с таким id"),
    })
    @PostMapping("/create")
    public ResponseEntity<Void> createCard(
            @Parameter(description = "ID пользователя, для которого создаётся карта", required = true)
            @RequestParam Long userId) {
        adminCardService.createCard(userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Заблокировать карту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта успешно заблокирована"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
            @ApiResponse(responseCode = "409", description = "Карта уже заблокирована")
    })
    @PostMapping("/{cardId}/block")
    public ResponseEntity<Void> blockCard(
            @Parameter(description = "ID карты для блокировки", required = true)
            @PathVariable Long cardId) {
        adminCardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Активировать карту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта успешно активирована"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    @PostMapping("/{cardId}/activate")
    public ResponseEntity<Void> activateCard(
            @Parameter(description = "ID карты для активации", required = true)
            @PathVariable Long cardId) {
        adminCardService.activateCard(cardId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Удалить карту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Карта успешно удалена"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена"),
            @ApiResponse(responseCode = "401", description = "Неавторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "ID карты для удаления", required = true)
            @PathVariable Long cardId) {
        adminCardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Получить все карты (с пагинацией)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Страница карт успешно получена"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "403", description = "Нет прав доступа")
    })
    @GetMapping
    public ResponseEntity<Page<CardAdminDto>> getAllCards(
            @Parameter(description = "Номер страницы (начинается с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<CardAdminDto> cardsPage = adminCardService.getAllCards(pageable);
        return ResponseEntity.ok(cardsPage);
    }
}

