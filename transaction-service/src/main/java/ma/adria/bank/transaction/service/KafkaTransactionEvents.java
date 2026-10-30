package ma.adria.bank.transaction.service;
import java.util.concurrent.TimeUnit;
import ma.adria.bank.transaction.domain.TransactionCreated;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class KafkaTransactionEvents implements TransactionEvents {
    public static final String TOPIC="bank.transactions.created";
    private final KafkaTemplate<String,String> kafka;
    private final ObjectMapper json;
    public KafkaTransactionEvents(KafkaTemplate<String,String> kafka,ObjectMapper json) { this.kafka=kafka;this.json=json; }
    public void publish(TransactionCreated event) {
        try {
            kafka.send(TOPIC,event.accountId().toString(),json.writeValueAsString(event)).get(12,TimeUnit.SECONDS);
        } catch(InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publication interrupted",interrupted);
        } catch(Exception failure) {
            throw new IllegalStateException("Kafka publication was not confirmed",failure);
        }
    }
}
