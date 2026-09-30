package ru.practicum.explorewithme.interaction.grpc;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GrpcClientFallbackTest {

    @Test
    void collectorShouldIgnoreCircuitBreakerFailure() {
        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(factory.create("collector")).thenReturn(circuitBreaker);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            try {
                return supplier.get();
            } catch (Throwable cause) {
                Function<Throwable, ?> fallback = invocation.getArgument(1);
                return fallback.apply(cause);
            }
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));

        CollectorGrpcClient client = new CollectorGrpcClient(factory);
        client.collectUserAction(1L, 2L, ActionTypeProto.ACTION_VIEW, Instant.now());

        verify(factory).create("collector");
        verify(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }

    @Test
    void analyzerShouldReturnEmptyRecommendationsOnCircuitBreakerFailure() {
        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(factory.create("analyzer")).thenReturn(circuitBreaker);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            try {
                return supplier.get();
            } catch (Throwable cause) {
                Function<Throwable, ?> fallback = invocation.getArgument(1);
                return fallback.apply(cause);
            }
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));

        AnalyzerGrpcClient client = new AnalyzerGrpcClient(factory);

        List<RecommendedEventProto> result = client.getRecommendationsForUser(1L, 10).toList();

        assertThat(result).isEmpty();
        verify(factory).create("analyzer");
        verify(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }

    @Test
    void analyzerShouldReturnEmptySimilarEventsOnCircuitBreakerFailure() {
        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(factory.create("analyzer")).thenReturn(circuitBreaker);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            try {
                return supplier.get();
            } catch (Throwable cause) {
                Function<Throwable, ?> fallback = invocation.getArgument(1);
                return fallback.apply(cause);
            }
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));

        AnalyzerGrpcClient client = new AnalyzerGrpcClient(factory);

        List<RecommendedEventProto> result = client.getSimilarEvents(2L, 1L, 10).toList();

        assertThat(result).isEmpty();
        verify(factory).create("analyzer");
        verify(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }

    @Test
    void analyzerShouldReturnEmptyInteractionsOnCircuitBreakerFailure() {
        CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
        CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
        when(factory.create("analyzer")).thenReturn(circuitBreaker);
        doAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            try {
                return supplier.get();
            } catch (Throwable cause) {
                Function<Throwable, ?> fallback = invocation.getArgument(1);
                return fallback.apply(cause);
            }
        }).when(circuitBreaker).run(any(Supplier.class), any(Function.class));

        AnalyzerGrpcClient client = new AnalyzerGrpcClient(factory);

        assertThat(client.getInteractionsCount(List.of(1L, 2L))).isEmpty();
        verify(factory).create("analyzer");
        verify(circuitBreaker).run(any(Supplier.class), any(Function.class));
    }
}
