package banking.boby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class BobyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BobyApplication.class, args);
    }

}
