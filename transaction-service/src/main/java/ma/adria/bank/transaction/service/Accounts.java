package ma.adria.bank.transaction.service;
import java.math.BigDecimal;
import java.util.UUID;
public interface Accounts {
    record AccountView(UUID id,String status,String currency,double balance) {}
    AccountView get(UUID id);
    void apply(UUID operationId,UUID source,UUID target,BigDecimal amount,String currency);
}
