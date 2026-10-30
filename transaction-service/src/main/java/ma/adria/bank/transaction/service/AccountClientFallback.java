package ma.adria.bank.transaction.service;
import java.util.UUID;
import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountClientFallback implements FallbackFactory<AccountFeignClient> {
    public AccountFeignClient create(Throwable failure) {
        return new AccountFeignClient() {
            public Accounts.AccountView get(UUID id,String authorization) { throw mapped(failure); }
            public void apply(UUID id,MovementInput input,String authorization) { throw mapped(failure); }
        };
    }
    private RuntimeException mapped(Throwable failure) {
        for(Throwable cause=failure;cause!=null;cause=cause.getCause()) {
            if(cause instanceof FeignException http) {
                if(http.status()==400 || http.status()==404) return new IllegalArgumentException("Account is missing or the movement is invalid");
                if(http.status()==409) return new IllegalStateException("Account movement was rejected");
            }
        }
        return new AccountUnavailableException(failure);
    }
}
