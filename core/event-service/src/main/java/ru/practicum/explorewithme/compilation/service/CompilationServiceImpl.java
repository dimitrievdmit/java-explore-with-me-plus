package ru.practicum.explorewithme.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.compilation.dal.CompilationRepository;
import ru.practicum.explorewithme.compilation.dto.CompilationDto;
import ru.practicum.explorewithme.compilation.dto.NewCompilationDto;
import ru.practicum.explorewithme.compilation.dto.UpdateCompilationRequestDto;
import ru.practicum.explorewithme.compilation.mapper.CompilationMapper;
import ru.practicum.explorewithme.compilation.model.Compilation;
import ru.practicum.explorewithme.event.dal.EventRepository;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.RequestClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;

    private void validateEventsExist(Set<Event> events, List<Long> eventIds) {
        if (eventIds != null && !eventIds.isEmpty()) {
            Set<Long> foundIds = events.stream().map(Event::getId).collect(Collectors.toSet());
            for (Long eventId : eventIds) {
                if (!foundIds.contains(eventId)) {
                    throw new NotFoundException("Событие с id=" + eventId + " не найдено");
                }
            }
        }
    }

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto dto) {
        Compilation compilation = CompilationMapper.toEntity(dto);

        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            validateEventsExist(events, dto.getEvents());
            compilation.setEvents(events);
        }

        compilation = compilationRepository.save(compilation);
        log.info("Создана подборка: {}", compilation.getId());
        return toDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequestDto request) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        CompilationMapper.updateEntityFromRequest(request, compilation);

        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            validateEventsExist(events, request.getEvents());
            compilation.setEvents(events);
        }

        log.info("Обновлена подборка: {}", compilation.getId());
        return toDto(compilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        compilationRepository.delete(compilation);
        log.info("Удалена подборка: {}", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));
        return toDto(compilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getAll(Boolean pinned, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = pinned != null
                ? compilationRepository.findByPinned(pinned, pageable).getContent()
                : compilationRepository.findAll(pageable).getContent();

        List<Event> allEvents = compilations.stream().flatMap(c -> c.getEvents().stream()).toList();
        Map<Long, UserShortDto> initiators = getInitiatorsMap(allEvents);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(allEvents);

        return compilations.stream()
                .map(c -> CompilationMapper.toDto(c, initiators, confirmedRequests))
                .collect(Collectors.toList());
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Event> events = new ArrayList<>(compilation.getEvents());
        return CompilationMapper.toDto(compilation, getInitiatorsMap(events), getConfirmedRequestsMap(events));
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> ids = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
        return userClient.getUsersShort(ids).stream().collect(Collectors.toMap(UserShortDto::getId, u -> u));
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> ids = events.stream().map(Event::getId).collect(Collectors.toList());
        return requestClient.getConfirmedRequestsCounts(new EventIdListDto(ids)).stream()
                .collect(Collectors.toMap(ConfirmedRequestsCountDto::getEventId, ConfirmedRequestsCountDto::getCount));
    }
}