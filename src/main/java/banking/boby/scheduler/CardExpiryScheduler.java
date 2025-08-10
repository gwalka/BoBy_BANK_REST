package banking.boby.scheduler;

import banking.boby.service.AdminCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CardExpiryScheduler {

    private final AdminCardService adminCardService;

    @Scheduled(cron = "0 0 0 1 * *", zone = "Europe/Moscow")
    public void deactivateExpiredCards() {
        log.info("Запуск деактивации просроченных карт");
        adminCardService.deactivateExpiredCards();
        log.info("Деактивация просроченных карт завершена");
    }
}
