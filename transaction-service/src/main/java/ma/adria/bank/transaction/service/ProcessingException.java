package ma.adria.bank.transaction.service;
import java.util.UUID;
public class ProcessingException extends RuntimeException {
    private final UUID transactionId;
    public ProcessingException(UUID id,Throwable cause) {
        super("Operation is pending confirmation; inspect transaction "+id+" and retry only with the same requestId",cause);
        this.transactionId=id;
    }
    public UUID transactionId() { return transactionId; }
}
