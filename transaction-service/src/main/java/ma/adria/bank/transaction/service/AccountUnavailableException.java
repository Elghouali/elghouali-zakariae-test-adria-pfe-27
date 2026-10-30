package ma.adria.bank.transaction.service;
public class AccountUnavailableException extends RuntimeException {
    public AccountUnavailableException(Throwable cause) { super("Account Service is unavailable",cause); }
}
