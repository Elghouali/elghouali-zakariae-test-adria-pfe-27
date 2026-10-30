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

    public List<Account> findAll() {
        return accounts.values().stream().sorted(Comparator.comparing(Account::id)).toList();
    }

    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public Account save(Account account) {
        accounts.put(account.id(), account);
        return account;
    }

    public Account update(UUID id, UnaryOperator<Account> operation) {
        return accounts.compute(id, (key, current) -> {
            if (current == null) throw new AccountNotFoundException(id);
            return operation.apply(current);
        });
    }
}
