package ma.adria.bank.transaction.service;
import ma.adria.bank.transaction.domain.TransactionCreated;
public interface TransactionEvents { void publish(TransactionCreated event); }
