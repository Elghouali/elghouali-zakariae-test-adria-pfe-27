package ma.adria.bank.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import feign.*;
import ma.adria.bank.transaction.domain.TransactionCreated;
import ma.adria.bank.transaction.service.*;
import org.junit.jupiter.api.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionConnectorsTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); Thread.interrupted(); }

    @Test void feignCapturesTheBearerBeforeTheCircuitBreakerThreadRuns() {
        var client=mock(AccountFeignClient.class);var accounts=new FeignAccounts(client);var id=UUID.randomUUID();
        var token=Jwt.withTokenValue("test-bearer").header("alg","test").subject("manager").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token));
        accounts.get(id);var operation=UUID.randomUUID();accounts.apply(operation,id,null,BigDecimal.ONE,"EUR");
        verify(client).get(id,"Bearer test-bearer");
        verify(client).apply(id,new AccountFeignClient.MovementInput(operation,null,BigDecimal.ONE,"EUR"),"Bearer test-bearer");
        SecurityContextHolder.clearContext();
        assertThatThrownBy(()->accounts.get(id)).isInstanceOf(IllegalStateException.class);
    }

    private FeignException httpFailure(int status) {
        var request=Request.create(Request.HttpMethod.GET,"http://account.test",Map.of(),null,java.nio.charset.StandardCharsets.UTF_8,null);
        return FeignException.errorStatus("get",Response.builder().status(status).reason("test").request(request).headers(Map.of()).build());
    }

    @Test void fallbacksRejectRequestsInsteadOfInventingBankData() {
        var factory=new AccountClientFallback();
        for(int status:new int[]{400,404}) {
            assertThatThrownBy(()->factory.create(httpFailure(status)).get(UUID.randomUUID(),"token")).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->factory.create(httpFailure(409)).get(UUID.randomUUID(),"token")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->factory.create(new RuntimeException("offline")).get(UUID.randomUUID(),"token")).isInstanceOf(AccountUnavailableException.class);
        assertThatThrownBy(()->factory.create(new RuntimeException("offline")).apply(UUID.randomUUID(),null,"token")).isInstanceOf(AccountUnavailableException.class);
    }

    @Test void kafkaUsesTheRequiredTopicAccountKeyAndEventFields() throws Exception {
        @SuppressWarnings("unchecked") KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        var json=JsonMapper.builder().build();
        var event=new TransactionCreated(UUID.randomUUID(),"TransactionCreated",UUID.randomUUID(),UUID.randomUUID(),new BigDecimal("250.00"),"EUR",Instant.parse("2026-01-15T10:30:00Z"));
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));
        new KafkaTransactionEvents(kafka,json).publish(event);
        var payload=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("bank.transactions.created"),eq(event.accountId().toString()),payload.capture());
        var value=json.readTree(payload.getValue());
        assertThat(value.get("eventId").asText()).isEqualTo(event.eventId().toString());
        assertThat(value.get("eventType").asText()).isEqualTo("TransactionCreated");
        assertThat(value.get("transactionId").asText()).isEqualTo(event.transactionId().toString());
        assertThat(value.get("amount").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(value.get("currency").asText()).isEqualTo("EUR");
        assertThat(value.get("occurredAt").asText()).isEqualTo("2026-01-15T10:30:00Z");
    }

    @Test void kafkaFailuresArePropagatedAndInterruptsPreserved() throws Exception {
        @SuppressWarnings("unchecked") KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
        var event=new TransactionCreated(UUID.randomUUID(),"TransactionCreated",UUID.randomUUID(),UUID.randomUUID(),BigDecimal.ONE,"EUR",Instant.now());
        var publisher=new KafkaTransactionEvents(kafka,JsonMapper.builder().build());
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("offline")));
        assertThatThrownBy(()->publisher.publish(event)).isInstanceOf(IllegalStateException.class);
        @SuppressWarnings("unchecked") CompletableFuture<org.springframework.kafka.support.SendResult<String,String>> future=mock(CompletableFuture.class);
        when(future.get(anyLong(),any())).thenThrow(new InterruptedException());
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(future);
        assertThatThrownBy(()->publisher.publish(event)).isInstanceOf(IllegalStateException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
