package ma.adria.bank.transaction.domain;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
public record Transaction(UUID id, UUID accountId, BigDecimal amount, String currency,
        String merchantCategory, String country, Instant createdAt, UUID targetAccountId, TransactionStatus status) {
    public Transaction withStatus(TransactionStatus next) {
        return new Transaction(id,accountId,amount,currency,merchantCategory,country,createdAt,targetAccountId,next);
    }
}
