package ma.adria.bank.transaction.web;
import graphql.GraphQLError;
import ma.adria.bank.transaction.service.*;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice
public class TransactionErrors {
    @ExceptionHandler(TransactionNotFoundException.class) public ProblemDetail missing(TransactionNotFoundException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class) public ProblemDetail invalid(IllegalArgumentException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) public ProblemDetail conflict(IllegalStateException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e.getMessage()); }
    @ExceptionHandler({ProcessingException.class,AccountUnavailableException.class}) public ProblemDetail unavailable(RuntimeException e) {
        var problem=ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,e.getMessage());
        if(e instanceof ProcessingException operation) problem.setProperty("transactionId",operation.transactionId());
        return problem;
    }
    @GraphQlExceptionHandler(TransactionNotFoundException.class) public GraphQLError graphMissing(TransactionNotFoundException e) { return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(e.getMessage()).build(); }
    @GraphQlExceptionHandler({IllegalArgumentException.class,IllegalStateException.class}) public GraphQLError graphInvalid(RuntimeException e) { return GraphQLError.newError().errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build(); }
    @GraphQlExceptionHandler({ProcessingException.class,AccountUnavailableException.class}) public GraphQLError graphUnavailable(RuntimeException e) { return GraphQLError.newError().errorType(ErrorType.INTERNAL_ERROR).message(e.getMessage()).build(); }
}
