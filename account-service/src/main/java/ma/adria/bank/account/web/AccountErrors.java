package ma.adria.bank.account.web;

import graphql.GraphQLError;
import ma.adria.bank.account.service.AccountNotFoundException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountErrors {
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail missing(AccountNotFoundException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflict(IllegalStateException error) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage());
    }

    @GraphQlExceptionHandler(AccountNotFoundException.class)
    public GraphQLError graphMissing(AccountNotFoundException error) {
        return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(error.getMessage()).build();
    }

    @GraphQlExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public GraphQLError graphInvalid(RuntimeException error) {
        return GraphQLError.newError().errorType(ErrorType.BAD_REQUEST).message(error.getMessage()).build();
    }
}
