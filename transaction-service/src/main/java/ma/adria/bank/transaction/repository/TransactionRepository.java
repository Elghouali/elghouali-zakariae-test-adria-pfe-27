package ma.adria.bank.transaction.repository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import ma.adria.bank.transaction.domain.*;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepository {
    public record Entry(Transaction transaction, TransactionCreated event) {}
    private final Map<UUID,Entry> entries=new ConcurrentHashMap<>();
    public Optional<Entry> find(UUID id) { return Optional.ofNullable(entries.get(id)); }
    public void save(Entry entry) { entries.put(entry.transaction().id(),entry); }
    public List<Transaction> list() { return entries.values().stream().map(Entry::transaction).sorted(Comparator.comparing(Transaction::createdAt).thenComparing(Transaction::id)).toList(); }
}
