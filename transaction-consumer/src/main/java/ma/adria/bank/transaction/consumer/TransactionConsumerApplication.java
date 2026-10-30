
package ma.adria.bank.transaction.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransactionConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TransactionConsumerApplication.class, args
        );
    }
}
