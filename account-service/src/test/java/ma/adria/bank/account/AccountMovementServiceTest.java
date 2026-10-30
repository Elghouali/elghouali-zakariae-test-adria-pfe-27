package ma.adria.bank.account;

import java.math.BigDecimal;
import java.util.UUID;
import ma.adria.bank.account.domain.AccountStatus;
import ma.adria.bank.account.repository.InMemoryAccountRepository;
import ma.adria.bank.account.service.AccountService;
import ma.adria.bank.account.service.AccountMovementService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AccountMovementServiceTest {
    final InMemoryAccountRepository repo = new InMemoryAccountRepository();
    final AccountService accounts = new AccountService(repo);
    final AccountMovementService movements = new AccountMovementService(repo);

    @Test void debitIsAtomicAndIdempotent() {
        var a=accounts.create("c","EUR",100.0);
        var operation=UUID.randomUUID();
        var result=movements.apply(operation,a.id(),null,new BigDecimal("25"),"EUR");
        assertThat(result.source().balance()).isEqualTo(75);
        assertThat(movements.apply(operation,a.id(),null,new BigDecimal("25.00"),"EUR")).isEqualTo(result);
        assertThat(accounts.get(a.id()).balance()).isEqualTo(75);
        assertThatThrownBy(()->movements.apply(operation,a.id(),null,new BigDecimal("20"),"EUR")).isInstanceOf(IllegalStateException.class);
    }

    @Test void transferChangesBothAccountsOrNeither() {
        var a=accounts.create("a","EUR",100.0);var b=accounts.create("b","EUR",10.0);
        movements.apply(UUID.randomUUID(),a.id(),b.id(),new BigDecimal("30"),"EUR");
        assertThat(accounts.get(a.id()).balance()).isEqualTo(70);
        assertThat(accounts.get(b.id()).balance()).isEqualTo(40);
        accounts.changeStatus(b.id(),AccountStatus.BLOCKED);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),b.id(),BigDecimal.ONE,"EUR")).isInstanceOf(IllegalStateException.class);
        assertThat(accounts.get(a.id()).balance()).isEqualTo(70);
    }

    @Test void rejectsInvalidOrUnsafeMovements() {
        var a=accounts.create("a","EUR",100.0);var b=accounts.create("b","MAD",10.0);
        assertThatThrownBy(()->movements.apply(null,a.id(),null,BigDecimal.ONE,"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),a.id(),BigDecimal.ONE,"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,BigDecimal.ZERO,"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,null,"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,BigDecimal.ONE,"MAD")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),b.id(),BigDecimal.ONE,"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,new BigDecimal("101"),"EUR")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),UUID.randomUUID(),null,BigDecimal.ONE,"EUR")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,new BigDecimal("0.0000000000000000001"),"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->movements.apply(UUID.randomUUID(),a.id(),null,new BigDecimal("1e-1000000000"),"EUR")).isInstanceOf(IllegalArgumentException.class);
        assertThat(accounts.get(a.id()).balance()).isEqualTo(100);
    }
}
