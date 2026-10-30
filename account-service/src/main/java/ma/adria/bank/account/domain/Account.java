package ma.adria.bank.account.domain;

import java.util.UUID;

public record Account(UUID id, String customerReference, AccountStatus status, String currency, double balance) {}
