package ru.practicum.explorewithme.compilation.service;


import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequestDto;

import java.util.List;

public interface CompilationService {
    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long compId, UpdateCompilationRequestDto request);

    void delete(Long compId);

    CompilationDto getById(Long compId);

    List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size);
}