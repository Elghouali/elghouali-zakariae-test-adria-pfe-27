package ma.adria.bank.account;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.repository.InMemoryAccountRepository;
import ma.adria.bank.account.service.AccountService;
import ma.adria.bank.account.service.AccountNotFoundException;
import ma.adria.bank.account.service.BalanceConflictException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountServiceTest {
    private final AccountService service = new AccountService(new InMemoryAccountRepository());

    @Test
    void createsAnActiveAccountWithTheDocumentFields() {
        var account = service.create(" customer-1 ", "eur", 120.50);
        assertThat(account.id()).isNotNull();
        assertThat(account.customerReference()).isEqualTo("customer-1");
        assertThat(account.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.currency()).isEqualTo("EUR");
        assertThat(account.balance()).isEqualTo(120.50);
        assertThat(service.get(account.id())).isEqualTo(account);
        assertThat(service.list()).containsExactly(account);
    }

    @Test
    void reportsUnknownAccounts() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> service.changeStatus(id, AccountStatus.BLOCKED))
                .isInstanceOf(AccountNotFoundException.class);
        assertThatThrownBy(() -> service.updateBalance(id, 0.0, 1.0))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void validatesAllEntryPointsInTheSharedService() {
        assertThatThrownBy(() -> service.create(" ", "EUR", 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create(null, "EUR", 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("c", null, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.create("c", "EURO", 0.0)).isInstanceOf(IllegalArgumentException.class);
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThatThrownBy(() -> service.create("c", "EUR", invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        var account = service.create("c", "EUR", 10.0);
        assertThatThrownBy(() -> service.get(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.changeStatus(account.id(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateBalance(account.id(), Double.NaN, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateBalance(account.id(), 10.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.get(account.id()).balance()).isEqualTo(10);
    }

    @Test
    void blockedAccountsCannotHaveTheirBalanceChanged() {
        var account = service.create("c", "MAD", 100.0);
        assertThat(service.changeStatus(account.id(), AccountStatus.BLOCKED).status()).isEqualTo(AccountStatus.BLOCKED);
        assertThatThrownBy(() -> service.updateBalance(account.id(), 100.0, 90.0)).isInstanceOf(IllegalStateException.class);
        assertThat(service.get(account.id()).balance()).isEqualTo(100);
        service.changeStatus(account.id(), AccountStatus.ACTIVE);
        assertThat(service.updateBalance(account.id(), 100.0, 90.0).balance()).isEqualTo(90);
    }

    @Test
    void staleBalanceUpdatesCannotOverwriteNewerBalances() {
        var account = service.create("c", "EUR", 100.0);
        service.updateBalance(account.id(), 100.0, 90.0);
        assertThatThrownBy(() -> service.updateBalance(account.id(), 100.0, 80.0)).isInstanceOf(BalanceConflictException.class);
        assertThat(service.get(account.id()).balance()).isEqualTo(90);
    }

    @Test
    void competingUpdatesHaveExactlyOneWinner() throws Exception {
        var account = service.create("c", "EUR", 100.0);
        var gate = new CountDownLatch(1);
        var successes = new AtomicInteger();
        var conflicts = new AtomicInteger();
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(i -> pool.submit(() -> {
                gate.await();
                try {
                    service.updateBalance(account.id(), 100.0, 90.0);
                    successes.incrementAndGet();
                } catch (BalanceConflictException expected) {
                    conflicts.incrementAndGet();
                }
                return null;
            })).toList();
            gate.countDown();
            for (var task : tasks) task.get();
        }
        assertThat(successes.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(1);
        assertThat(service.get(account.id()).balance()).isEqualTo(90);
    }
}
