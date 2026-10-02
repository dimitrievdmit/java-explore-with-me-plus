package ru.practicum.explorewithme.compilation.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequestDto;
import ru.practicum.explorewithme.interaction.grpc.AnalyzerGrpcClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CompilationServiceIntegrationTest {
    @Autowired
    private CompilationService compilationService;

    @MockBean
    private AnalyzerGrpcClient analyzerGrpcClient;
    @MockBean
    private CollectorGrpcClient collectorGrpcClient;

    @Test
    void create_ShouldCreateCompilation() {
        NewCompilationDto dto = new NewCompilationDto("Test", false, null);

        CompilationDto result = compilationService.create(dto);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    void update_ShouldUpdateCompilation() {
        NewCompilationDto createDto = new NewCompilationDto("Original", false, null);
        CompilationDto created = compilationService.create(createDto);

        UpdateCompilationRequestDto updateDto = new UpdateCompilationRequestDto("Updated", true, null);
        CompilationDto result = compilationService.update(created.getId(), updateDto);

        assertThat(result.getTitle()).isEqualTo("Updated");
    }

    @Test
    void delete_ShouldDeleteCompilation() {
        NewCompilationDto dto = new NewCompilationDto("ToDelete", false, null);
        CompilationDto created = compilationService.create(dto);

        compilationService.delete(created.getId());

        assertThatThrownBy(() -> compilationService.getById(created.getId())).isInstanceOf(Exception.class);
    }

    @Test
    void getById_ShouldReturnCompilation() {
        NewCompilationDto dto = new NewCompilationDto("Test", false, null);
        CompilationDto created = compilationService.create(dto);

        CompilationDto result = compilationService.getById(created.getId());

        assertThat(result.getId()).isEqualTo(created.getId());
    }
}