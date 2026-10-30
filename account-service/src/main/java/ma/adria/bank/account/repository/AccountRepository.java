package ma.adria.bank.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import ma.adria.bank.account.domain.Account;

public interface AccountRepository {
    <T> T atomically(java.util.function.Supplier<T> action);
    List<Account> findAll();
    Optional<Account> findById(UUID id);
    Account save(Account account);
    Account update(UUID id, UnaryOperator<Account> operation);
}
