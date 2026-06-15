package pk.kj.pasir_kosecki_jakub.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.List;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionResolver {

    @Override
    public Mono<List<GraphQLError>> resolveException(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ConstraintViolationException validationException) {
            List<GraphQLError> errors = validationException.getConstraintViolations()
                    .stream()
                    .map(violation -> GraphqlErrorBuilder.newError(env)
                            .message(violation.getPropertyPath() + ": " + violation.getMessage())
                            .build())
                    .toList();

            return Mono.just(errors);
        }

        GraphQLError error = GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .build();

        return Mono.just(List.of(error));
    }
}