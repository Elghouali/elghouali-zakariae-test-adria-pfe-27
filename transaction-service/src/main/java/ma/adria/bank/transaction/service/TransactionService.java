package ma.adria.bank.transaction.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import ma.adria.bank.transaction.domain.*;
import ma.adria.bank.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository repository;
    private final Accounts accounts;
    private final TransactionEvents events;
    private final BigDecimal maximum;
    private final Clock clock;
    private final ConcurrentHashMap<UUID,Object> locks=new ConcurrentHashMap<>();

    public TransactionService(TransactionRepository repository,Accounts accounts,TransactionEvents events,
            @Value("${bank.transaction.max-amount}") BigDecimal maximum,Clock clock) {
        this.repository=repository;this.accounts=accounts;this.events=events;this.maximum=maximum;this.clock=clock;
        if(maximum.signum()<=0) throw new IllegalArgumentException("Transaction maximum must be positive");
    }

    public List<Transaction> list() { return repository.list(); }
    public Transaction get(UUID id) {
        if(id==null) throw new IllegalArgumentException("transactionId is required");
        return repository.find(id).orElseThrow(()->new TransactionNotFoundException(id)).transaction();
    }

    public Transaction create(TransactionRequest input) {
        validate(input);
        synchronized(locks.computeIfAbsent(input.requestId(),key->new Object())) {
            var entry=repository.find(input.requestId()).orElse(null);
            if(entry==null) {
                checkAccount(accounts.get(input.accountId()),input,true);
                if(input.targetAccountId()!=null) checkAccount(accounts.get(input.targetAccountId()),input,false);
                var now=clock.instant();
                var transaction=new Transaction(input.requestId(),input.accountId(),input.amount(),input.currency(),
                        input.merchantCategory(),input.country(),now,input.targetAccountId(),TransactionStatus.PENDING);
                var event=new TransactionCreated(UUID.randomUUID(),"TransactionCreated",transaction.id(),transaction.accountId(),transaction.amount(),transaction.currency(),now);
                entry=new TransactionRepository.Entry(transaction,event);
                repository.save(entry);
            } else if(!sameRequest(entry.transaction(),input)) {
                throw new IllegalStateException("requestId already belongs to a different transaction");
            }
            if(entry.transaction().status()==TransactionStatus.REJECTED) throw new IllegalStateException("This requestId was definitively rejected; use a new requestId after correcting the request");
            if(entry.transaction().status()==TransactionStatus.COMPLETED) return entry.transaction();
            try {
                if(entry.transaction().status()==TransactionStatus.PENDING) {
                    accounts.apply(input.requestId(),input.accountId(),input.targetAccountId(),input.amount(),input.currency());
                    entry=new TransactionRepository.Entry(entry.transaction().withStatus(TransactionStatus.APPLIED),entry.event());
                    repository.save(entry);
                }
                events.publish(entry.event());
                entry=new TransactionRepository.Entry(entry.transaction().withStatus(TransactionStatus.COMPLETED),entry.event());
                repository.save(entry);
                return entry.transaction();
            } catch(IllegalArgumentException | IllegalStateException rejection) {
                if(entry.transaction().status()==TransactionStatus.PENDING) {
                    repository.save(new TransactionRepository.Entry(entry.transaction().withStatus(TransactionStatus.REJECTED),entry.event()));
                    throw rejection;
                }
                throw new ProcessingException(input.requestId(),rejection);
            } catch(RuntimeException failure) {
                throw new ProcessingException(input.requestId(),failure);
            }
        }
    }

    private void validate(TransactionRequest input) {
        if(input==null || input.requestId()==null || input.accountId()==null) throw new IllegalArgumentException("requestId and accountId are required");
        if(input.amount()!=null && (input.amount().precision()>34 || input.amount().scale() < -18 || input.amount().scale()>18)) throw new IllegalArgumentException("amount exceeds supported decimal precision/scale");
        if(input.amount()==null || input.amount().signum()<=0 || input.amount().compareTo(maximum)>0) throw new IllegalArgumentException("amount must be positive and at most "+maximum);
        if(input.currency()==null || !input.currency().matches("[A-Z]{3}")) throw new IllegalArgumentException("currency must contain three uppercase letters");
        if(input.merchantCategory()==null || input.merchantCategory().isBlank() || input.country()==null || input.country().isBlank()) throw new IllegalArgumentException("merchantCategory and country are required");
        if(input.accountId().equals(input.targetAccountId())) throw new IllegalArgumentException("Transfer accounts must differ");
    }

    private void checkAccount(Accounts.AccountView account,TransactionRequest input,boolean source) {
        if(account==null) throw new IllegalArgumentException("Account does not exist");
        if(!"ACTIVE".equals(account.status())) throw new IllegalStateException("Account must be active");
        if(!input.currency().equals(account.currency())) throw new IllegalArgumentException("Account and transaction currencies must match");
        if(!Double.isFinite(account.balance())) throw new IllegalStateException("Account balance is invalid");
        if(source && BigDecimal.valueOf(account.balance()).compareTo(input.amount())<0) throw new IllegalStateException("Insufficient funds");
    }

    private boolean sameRequest(Transaction transaction,TransactionRequest input) {
        return transaction.accountId().equals(input.accountId()) && Objects.equals(transaction.targetAccountId(),input.targetAccountId())
                && transaction.amount().compareTo(input.amount())==0 && transaction.currency().equals(input.currency())
                && transaction.merchantCategory().equals(input.merchantCategory()) && transaction.country().equals(input.country());
    }
}
