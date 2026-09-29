package ru.practicum.explorewithme;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.practicum.explorewithme.interaction.grpc.AnalyzerGrpcClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;

@SuppressWarnings("EmptyMethod")
@SpringBootTest
class ContextTest {
    @MockBean
    private AnalyzerGrpcClient analyzerGrpcClient;
    @MockBean
    private CollectorGrpcClient collectorGrpcClient;

    @Test
    void contextLoads() {
    }
}
