
package ma.adria.bank.transaction.consumer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final ObjectMapper objectMapper;

    public TransactionEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${bank.kafka.transaction-topic:bank.transactions.created}",
            groupId = "${spring.kafka.consumer.group-id:bank-transaction-logger}"
    )
    public void consume(ConsumerRecord<String, String> record)
            throws Exception {

        JsonNode event = objectMapper.readTree(record.value());

        log.info(
                "TransactionCreated: eventId={}, type={}, transactionId={}, " +
                        "accountId={}, amount={}, currency={}, occurredAt={}, " +
                        "partition={}, offset={}",
                event.path("eventId").asText(),
                event.path("eventType").asText(),
                event.path("transactionId").asText(),
                event.path("accountId").asText(),
                event.path("amount").asText(),
                event.path("currency").asText(),
                event.path("occurredAt").asText(),
                record.partition(),
                record.offset()
        );
    }
}
