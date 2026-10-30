package ma.adria.bank.account.service;

public class BalanceConflictException extends IllegalStateException {
    public BalanceConflictException() { super("Account balance changed; consult the account again before updating it"); }
}
