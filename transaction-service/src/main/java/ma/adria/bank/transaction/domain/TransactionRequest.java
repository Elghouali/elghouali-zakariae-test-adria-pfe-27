package ma.adria.bank.transaction.domain;
import java.math.BigDecimal;
import java.util.UUID;
public record TransactionRequest(UUID requestId, UUID accountId, BigDecimal amount, String currency,
        String merchantCategory, String country, UUID targetAccountId) {}
