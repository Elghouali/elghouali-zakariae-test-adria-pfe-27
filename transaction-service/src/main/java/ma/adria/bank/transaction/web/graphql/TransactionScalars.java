package ma.adria.bank.transaction.web.graphql;
import java.math.BigDecimal;
import graphql.language.StringValue;
import graphql.schema.*;
import org.springframework.context.annotation.*;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
@Configuration
public class TransactionScalars {
    @Bean RuntimeWiringConfigurer transactionWiring() {
        var decimal=GraphQLScalarType.newScalar().name("Decimal").description("Exact decimal represented as a string")
                .coercing(new Coercing<BigDecimal,String>() {
                    @Override public String serialize(Object value) { return ((BigDecimal)value).toPlainString(); }
                    @Override public BigDecimal parseValue(Object value) {
                        if(!(value instanceof String text)) throw new CoercingParseValueException("Decimal values must be strings");
                        try { return new BigDecimal(text); } catch(NumberFormatException error) { throw new CoercingParseValueException("Invalid decimal"); }
                    }
                    @Override public BigDecimal parseLiteral(Object value) {
                        if(!(value instanceof StringValue text)) throw new CoercingParseLiteralException("Decimal literals must be strings");
                        try { return new BigDecimal(text.getValue()); } catch(NumberFormatException error) { throw new CoercingParseLiteralException("Invalid decimal"); }
                    }
                }).build();
        var instant=GraphQLScalarType.newScalar().name("Instant").coercing(new Coercing<java.time.Instant,String>() {
            @Override public String serialize(Object value) { return value.toString(); }
            @Override public java.time.Instant parseValue(Object value) { throw new CoercingParseValueException("Instant is output-only"); }
            @Override public java.time.Instant parseLiteral(Object value) { throw new CoercingParseLiteralException("Instant is output-only"); }
        }).build();
        return wiring->wiring.scalar(decimal).scalar(instant);
    }
}
