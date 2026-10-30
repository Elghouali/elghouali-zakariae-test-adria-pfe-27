package ma.adria.bank.transaction;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
@SpringBootApplication
@EnableFeignClients
public class TransactionApplication {
    public static void main(String[] args) { SpringApplication.run(TransactionApplication.class,args); }
    @Bean Clock transactionClock() { return Clock.systemUTC(); }
}
