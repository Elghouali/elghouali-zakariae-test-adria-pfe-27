package ma.adria.bank.transaction;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.*;
import ma.adria.bank.transaction.service.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.config.name=transaction-test","spring.cloud.config.enabled=false","eureka.client.enabled=false","bank.transaction.max-amount=1000","spring.ai.mcp.server.protocol=STATELESS"})
@AutoConfigureMockMvc
class TransactionApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountFeignClient accountClient;
    @MockitoBean TransactionEvents events;
    RequestPostProcessor manager() { return jwt().authorities(new SimpleGrantedAuthority("SCOPE_transactions:manage")); }
    @BeforeEach void account() { when(accountClient.get(any(),anyString())).thenAnswer(c->new Accounts.AccountView(c.getArgument(0),"ACTIVE","EUR",2000)); }
    Map<String,Object> input() { return new HashMap<>(Map.of("requestId",UUID.randomUUID().toString(),"accountId",UUID.randomUUID().toString(),"amount","25.50","currency","EUR","merchantCategory","retail","country","FR")); }

    @Test void authenticationAndScopeProtectEveryInterface() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/transactions").with(jwt())).andExpect(status().isForbidden());
        for(var path:List.of("/graphql","/mcp")) {
            mvc.perform(post(path).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
            mvc.perform(post(path).with(jwt()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        }
    }

    @Test void restValidatesPersistsAndSupportsIdempotentTransferRequests() throws Exception {
        var input=input();input.put("targetAccountId",UUID.randomUUID().toString());
        var body=json.writeValueAsString(input);var id=input.get("requestId");
        mvc.perform(post("/api/transactions").with(manager()).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.amount").value(25.5));
        mvc.perform(post("/api/transactions").with(manager()).contentType("application/json").content(body)).andExpect(status().isCreated());
        verify(events,times(1)).publish(any());
        mvc.perform(get("/api/transactions/{id}",id).with(manager())).andExpect(status().isOk());
        mvc.perform(get("/api/transactions").with(manager())).andExpect(status().isOk());
        mvc.perform(get("/api/transactions/{id}",UUID.randomUUID()).with(manager())).andExpect(status().isNotFound());
        input.put("amount","1000.01");
        mvc.perform(post("/api/transactions").with(manager()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
        input.put("amount","1");
        mvc.perform(post("/api/transactions").with(manager()).contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isConflict());
    }

    JsonNode graph(String query) throws Exception {
        var result=mvc.perform(post("/graphql").with(manager()).contentType("application/json").content(json.writeValueAsString(Map.of("query",query))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    @Test void graphqlKeepsDecimalPrecisionAndSharesRestState() throws Exception {
        var id=UUID.randomUUID();var account=UUID.randomUUID();
        var result=graph("mutation { createTransaction(input:{requestId:\""+id+"\",accountId:\""+account+"\",amount:\"25.123456789123456789\",currency:\"EUR\",merchantCategory:\"retail\",country:\"FR\"}) { id amount status } }");
        assertThat(result.has("errors")).isFalse();
        assertThat(result.at("/data/createTransaction/amount").asText()).isEqualTo("25.123456789123456789");
        assertThat(graph("{ transaction(id:\""+id+"\") { id } }").at("/data/transaction/id").asText()).isEqualTo(id.toString());
        assertThat(graph("{ transactions { id } }").has("errors")).isFalse();
        assertThat(graph("{ transaction(id:\""+UUID.randomUUID()+"\") { id } }").has("errors")).isTrue();
        mvc.perform(get("/api/transactions/{id}",id).with(manager())).andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    JsonNode mcp(String method,Object parameters) throws Exception {
        var result=mvc.perform(post("/mcp").with(manager()).contentType("application/json").accept("application/json","text/event-stream")
                .content(json.writeValueAsString(Map.of("jsonrpc","2.0","id",1,"method",method,"params",parameters)))).andExpect(status().isOk()).andReturn();
        if(result.getRequest().isAsyncStarted()) result=mvc.perform(asyncDispatch(result)).andReturn();
        var body=result.getResponse().getContentAsString();
        if(body.startsWith("event:") || body.startsWith("data:")) body=body.lines().filter(l->l.startsWith("data:")).map(l->l.substring(5).trim()).findFirst().orElseThrow();
        return json.readTree(body);
    }
    @Test void mcpInvokesTheSameTransactionOperations() throws Exception {
        assertThat(mcp("tools/list",Map.of()).toString()).contains("create_transaction","get_transaction","list_transactions");
        var input=input();
        var result=mcp("tools/call",Map.of("name","create_transaction","arguments",input));
        assertThat(result.at("/result/isError").asBoolean()).isFalse();
        assertThat(mcp("tools/call",Map.of("name","get_transaction","arguments",Map.of("transactionId",input.get("requestId")))).toString()).contains(input.get("requestId").toString());
        assertThat(mcp("tools/call",Map.of("name","list_transactions","arguments",Map.of())).toString()).contains(input.get("requestId").toString());
    }

    @Test void failedPublishingRemainsVisibleAndIsRetriable() throws Exception {
        doThrow(new IllegalStateException("Kafka offline")).when(events).publish(any());var input=input();
        mvc.perform(post("/api/transactions").with(manager()).contentType("application/json").content(json.writeValueAsString(input)))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.transactionId").value(input.get("requestId")));
        mvc.perform(get("/api/transactions/{id}",input.get("requestId")).with(manager())).andExpect(jsonPath("$.status").value("APPLIED"));
    }
}
