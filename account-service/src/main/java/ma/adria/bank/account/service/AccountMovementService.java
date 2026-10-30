package ma.adria.bank.account.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountMovementService {
    public record MovementResult(Account source, Account target) {}
    private record Applied(UUID source, UUID target, BigDecimal amount, String currency, MovementResult result) {}
    private final AccountRepository repository;
    private final Map<UUID, Applied> applied = new HashMap<>();

    public AccountMovementService(AccountRepository repository) { this.repository=repository; }

    public MovementResult apply(UUID operationId, UUID sourceId, UUID targetId, BigDecimal amount, String currency) {
        if(operationId==null || sourceId==null || sourceId.equals(targetId)) throw new IllegalArgumentException("Distinct source and target and an operationId are required");
        if(amount!=null && (amount.precision()>34 || amount.scale() < -18 || amount.scale()>18)) throw new IllegalArgumentException("amount exceeds supported decimal precision/scale");
        if(amount==null || amount.signum()<=0 || currency==null) throw new IllegalArgumentException("Positive amount and currency are required");
        return repository.atomically(()->{
            var previous=applied.get(operationId);
            if(previous!=null) {
                if(!previous.source().equals(sourceId) || !java.util.Objects.equals(previous.target(),targetId)
                        || previous.amount().compareTo(amount)!=0 || !previous.currency().equals(currency)) {
                    throw new IllegalStateException("operationId already belongs to a different movement");
                }
                return previous.result();
            }
            var source=account(sourceId,currency);
            var target=targetId==null ? null : account(targetId,currency);
            if(BigDecimal.valueOf(source.balance()).compareTo(amount)<0) throw new IllegalStateException("Insufficient funds");
            var debited=withBalance(source,BigDecimal.valueOf(source.balance()).subtract(amount));
            var credited=target==null ? null : withBalance(target,BigDecimal.valueOf(target.balance()).add(amount));
            repository.save(debited);
            if(credited!=null) repository.save(credited);
            var result=new MovementResult(debited,credited);
            applied.put(operationId,new Applied(sourceId,targetId,amount,currency,result));
            return result;
        });
    }

    private Account account(UUID id,String currency) {
        var account=repository.findById(id).orElseThrow(()->new AccountNotFoundException(id));
        if(account.status()!=AccountStatus.ACTIVE) throw new IllegalStateException("Account must be active");
        if(!account.currency().equals(currency)) throw new IllegalArgumentException("Account and transaction currencies must match");
        return account;
    }

    private Account withBalance(Account account,BigDecimal balance) {
        double value=balance.doubleValue();
        if(!Double.isFinite(value) || BigDecimal.valueOf(value).compareTo(balance)!=0) {
            throw new IllegalArgumentException("Balance cannot be represented safely by the specified double account model");
        }
        return new Account(account.id(),account.customerReference(),account.status(),account.currency(),value);
    }
}
