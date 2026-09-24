package ru.practicum.explorewithme.compilation.mapper;


import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequestDto;
import ru.practicum.explorewithme.compilation.model.Compilation;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.interaction.dto.EventShortDto;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto dto) {
        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        return compilation;
    }

    public static CompilationDto toDto(Compilation compilation, Map<Long, UserShortDto> initiators,
                                       Map<Long, Long> confirmedRequests) {
        List<EventShortDto> events = compilation.getEvents().stream()
                .map(e -> EventMapper.toShortDto(e, initiators.get(e.getInitiatorId()),
                        confirmedRequests.getOrDefault(e.getId(), 0L), 0.0))
                .collect(Collectors.toList());

        return CompilationDto.builder().id(compilation.getId()).title(compilation.getTitle())
                .pinned(compilation.getPinned()).events(events).build();
    }

    public static void updateEntityFromRequest(UpdateCompilationRequestDto request, Compilation compilation) {
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
    }
}