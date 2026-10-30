package ma.adria.bank.account.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.service.AccountNotFoundException;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryAccountRepository implements AccountRepository {
    private final ConcurrentHashMap<UUID, Account> accounts = new ConcurrentHashMap<>();

    public synchronized <T> T atomically(java.util.function.Supplier<T> action) { return action.get(); }

    public synchronized List<Account> findAll() {
        return accounts.values().stream().sorted(Comparator.comparing(Account::id)).toList();
    }

    public synchronized Optional<Account> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public synchronized Account save(Account account) {
        accounts.put(account.id(), account);
        return account;
    }

    public synchronized Account update(UUID id, UnaryOperator<Account> operation) {
        return accounts.compute(id, (key, current) -> {
            if (current == null) throw new AccountNotFoundException(id);
            return operation.apply(current);
        });
    }
}
