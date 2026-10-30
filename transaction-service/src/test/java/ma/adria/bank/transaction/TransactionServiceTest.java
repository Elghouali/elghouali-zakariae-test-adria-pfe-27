package ma.adria.bank.transaction;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import ma.adria.bank.transaction.domain.*;
import ma.adria.bank.transaction.repository.TransactionRepository;
import ma.adria.bank.transaction.service.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;

class TransactionServiceTest {
    final TransactionRepository repo=new TransactionRepository();
    final Accounts accounts=mock(Accounts.class);
    final TransactionEvents events=mock(TransactionEvents.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-01-15T10:30:00Z"),ZoneOffset.UTC);
    final TransactionService service=new TransactionService(repo,accounts,events,new BigDecimal("1000"),clock);
    final UUID accountId=UUID.randomUUID();

    TransactionRequest request(String amount) { return new TransactionRequest(UUID.randomUUID(),accountId,new BigDecimal(amount),"EUR","retail","FR",null); }
    void active() { when(accounts.get(accountId)).thenReturn(new Accounts.AccountView(accountId,"ACTIVE","EUR",2000.0)); }

    @Test void validatesBeforePersistingOrCallingAccounts() {
        for(String invalid:new String[]{"0","-1","1000.01","1e-1000000000"}) assertThatThrownBy(()->service.create(request(invalid))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.create(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(accounts,events);assertThat(service.list()).isEmpty();
    }

    @Test void requiresActiveExistingSameCurrencyAccountWithEnoughFunds() {
        when(accounts.get(accountId)).thenThrow(new IllegalArgumentException("missing"));
        assertThatThrownBy(()->service.create(request("1"))).isInstanceOf(IllegalArgumentException.class);
        reset(accounts);
        when(accounts.get(accountId)).thenReturn(new Accounts.AccountView(accountId,"BLOCKED","EUR",2000));
        assertThatThrownBy(()->service.create(request("1"))).isInstanceOf(IllegalStateException.class);
        when(accounts.get(accountId)).thenReturn(new Accounts.AccountView(accountId,"ACTIVE","MAD",2000));
        assertThatThrownBy(()->service.create(request("1"))).isInstanceOf(IllegalArgumentException.class);
        when(accounts.get(accountId)).thenReturn(new Accounts.AccountView(accountId,"ACTIVE","EUR",0));
        assertThatThrownBy(()->service.create(request("1"))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(events);assertThat(service.list()).isEmpty();
    }

    @Test void persistsThenMovesBalanceThenPublishesWithStableId() {
        active();var input=request("1000");
        doAnswer(call->{assertThat(service.get(input.requestId()).status()).isEqualTo(TransactionStatus.PENDING);return null;})
                .when(accounts).apply(input.requestId(),accountId,null,input.amount(),"EUR");
        doAnswer(call->{assertThat(service.get(input.requestId()).status()).isEqualTo(TransactionStatus.APPLIED);return null;})
                .when(events).publish(any());
        var result=service.create(input);
        assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.amount()).isEqualByComparingTo("1000");assertThat(result.createdAt()).isEqualTo(clock.instant());
        assertThat(service.create(input)).isEqualTo(result);
        verify(accounts,times(1)).apply(input.requestId(),accountId,null,input.amount(),"EUR");verify(events,times(1)).publish(any());
        assertThat(service.list()).containsExactly(result);
    }

    @Test void publicationFailureCanRetryWithoutDebitingAgain() {
        active();var input=request("25");doThrow(new IllegalStateException("Kafka unavailable")).doNothing().when(events).publish(any());
        assertThatThrownBy(()->service.create(input)).isInstanceOf(ProcessingException.class);
        assertThat(service.get(input.requestId()).status()).isEqualTo(TransactionStatus.APPLIED);
        assertThat(service.create(input).status()).isEqualTo(TransactionStatus.COMPLETED);
        verify(accounts,times(1)).apply(input.requestId(),accountId,null,input.amount(),"EUR");
        var captured=org.mockito.ArgumentCaptor.forClass(TransactionCreated.class);verify(events,times(2)).publish(captured.capture());
        assertThat(captured.getAllValues().get(0).eventId()).isEqualTo(captured.getAllValues().get(1).eventId());
    }

    @Test void uncertainAccountCallRemainsPendingAndDoesNotPublish() {
        active();var input=request("25");doThrow(new AccountUnavailableException(new RuntimeException("timeout"))).doNothing().when(accounts).apply(input.requestId(),accountId,null,input.amount(),"EUR");
        assertThatThrownBy(()->service.create(input)).isInstanceOf(ProcessingException.class);
        assertThat(service.get(input.requestId()).status()).isEqualTo(TransactionStatus.PENDING);verifyNoInteractions(events);
        assertThat(service.create(input).status()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test void rejectsReusingRequestIdWithDifferentData() {
        active();var input=request("10");service.create(input);
        var other=new TransactionRequest(input.requestId(),accountId,BigDecimal.ONE,"EUR","retail","FR",null);
        assertThatThrownBy(()->service.create(other)).isInstanceOf(IllegalStateException.class);
    }

    @Test void transferConsultsBothAccountsAndUsesOneAtomicMovement() {
        active();var target=UUID.randomUUID();when(accounts.get(target)).thenReturn(new Accounts.AccountView(target,"ACTIVE","EUR",0));
        var input=new TransactionRequest(UUID.randomUUID(),accountId,new BigDecimal("25"),"EUR","transfer","FR",target);
        assertThat(service.create(input).targetAccountId()).isEqualTo(target);
        verify(accounts).get(target);verify(accounts).apply(input.requestId(),accountId,target,input.amount(),"EUR");
        assertThatThrownBy(()->service.get(UUID.randomUUID())).isInstanceOf(TransactionNotFoundException.class);
    }
}
