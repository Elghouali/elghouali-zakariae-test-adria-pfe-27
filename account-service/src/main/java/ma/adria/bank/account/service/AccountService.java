package ma.adria.bank.account.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import ma.adria.bank.account.domain.Account;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) { this.repository = repository; }

    public Account create(String customerReference, String currency, Double balance) {
        if (customerReference == null || customerReference.isBlank()) {
            throw new IllegalArgumentException("customerReference is required");
        }
        if (currency == null || !currency.trim().matches("[a-zA-Z]{3}")) {
            throw new IllegalArgumentException("currency must contain three letters");
        }
        requireFinite(balance, "balance");
        return repository.save(new Account(UUID.randomUUID(), customerReference.trim(), AccountStatus.ACTIVE,
                currency.trim().toUpperCase(Locale.ROOT), balance));
    }

    public List<Account> list() { return repository.findAll(); }

    public Account get(UUID id) {
        requireId(id);
        return repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    public Account changeStatus(UUID id, AccountStatus status) {
        requireId(id);
        if (status == null) throw new IllegalArgumentException("status is required");
        return repository.update(id, account -> new Account(account.id(), account.customerReference(), status,
                account.currency(), account.balance()));
    }

    public Account updateBalance(UUID id, Double expectedBalance, Double balance) {
        requireId(id);
        requireFinite(expectedBalance, "expectedBalance");
        requireFinite(balance, "balance");
        return repository.update(id, account -> {
            if (account.status() != AccountStatus.ACTIVE) throw new IllegalStateException("Account must be active");
            if (Double.compare(account.balance(), expectedBalance) != 0) throw new BalanceConflictException();
            return new Account(account.id(), account.customerReference(), account.status(), account.currency(), balance);
        });
    }

    private static void requireId(UUID id) {
        if (id == null) throw new IllegalArgumentException("accountId is required");
    }

    private static void requireFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) throw new IllegalArgumentException(field + " must be a finite number");
    }
}
