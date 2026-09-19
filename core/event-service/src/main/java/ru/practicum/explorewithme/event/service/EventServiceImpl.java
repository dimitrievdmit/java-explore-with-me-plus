package ru.practicum.explorewithme.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.category.dal.CategoryRepository;
import ru.practicum.explorewithme.event.dal.EventRepository;
import ru.practicum.explorewithme.event.dto.*;
import ru.practicum.explorewithme.event.enums.AdminEventStateAction;
import ru.practicum.explorewithme.event.enums.UserEventStateAction;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.service.predicate.EventPredicate;
import ru.practicum.explorewithme.interaction.dto.*;
import ru.practicum.explorewithme.interaction.exception.BadRequestException;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.RequestClient;
import ru.practicum.explorewithme.interaction.feign.StatsClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        log.info("Создание события пользователем id={}", userId);
        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(), EventMapper.FORMATTER);
        if (ChronoUnit.HOURS.between(LocalDateTime.now(), eventDate) < 2) {
            throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }

        UserShortDto initiator = userClient.getUserShort(userId);

        Event event = EventMapper.toEntity(newEventDto);
        event.setInitiatorId(userId);
        event.setCategory(categoryRepository.findById(newEventDto.getCategory()).orElseThrow(() -> new NotFoundException("Категория с id=" + newEventDto.getCategory() + " не найдена")));
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);
        log.debug("Событие сохранено с id={}", event.getId());
        return EventMapper.toFullDto(event, initiator, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        log.info("Получение событий пользователя id={}, from={}, size={}", userId, from, size);
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);
        UserShortDto initiator = userClient.getUserShort(userId);

        return events.stream()
                .map(e -> EventMapper.toShortDto(e, initiator, confirmedRequests.getOrDefault(e.getId(), 0L), 0L))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEvent(Long userId, Long eventId) {
        log.info("Получение события id={} пользователя id={}", eventId, userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или недоступно"));
        Long confirmed = getConfirmedRequestsMap(List.of(event)).getOrDefault(eventId, 0L);
        UserShortDto initiator = userClient.getUserShort(userId);
        return EventMapper.toFullDto(event, initiator, confirmed, 0L);
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события id={} пользователем id={}", eventId, userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId).orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено или недоступно"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя редактировать опубликованное событие");
        }

        if (request.getStateAction() != null) {
            if (request.getStateAction() == UserEventStateAction.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            } else if (request.getStateAction() == UserEventStateAction.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            }
        }

        if (request.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(request.getEventDate(), EventMapper.FORMATTER);
            if (ChronoUnit.HOURS.between(LocalDateTime.now(), newDate) < 2) {
                throw new BadRequestException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
            }
        }

        if (request.getCategory() != null) {
            event.setCategory(categoryRepository.findById(request.getCategory()).orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategory() + " не найдена")));
        }

        EventMapper.updateEntityFromRequest(request, event);
        eventRepository.save(event);
        log.debug("Событие обновлено");
        Long confirmed = getConfirmedRequestsMap(List.of(event)).getOrDefault(eventId, 0L);
        UserShortDto initiator = userClient.getUserShort(userId);
        return EventMapper.toFullDto(event, initiator, confirmed, 0L);
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventSearchParamsAdmin params) {
        log.info("Получение событий администратором: users={}, states={}, categories={}", params.getUsers(), params.getStates(), params.getCategories());

        BooleanExpression predicate = EventPredicate.buildAdmin(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), Sort.by("id").ascending());
        Page<Event> events = eventRepository.findAll(predicate, pageable);

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events.getContent());
        Map<Long, UserShortDto> initiators = getInitiatorsMap(events.getContent());

        return events.stream()
                .map(e -> EventMapper.toFullDto(e, initiators.get(e.getInitiatorId()), confirmedRequests.getOrDefault(e.getId(), 0L), 0L))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события id={} администратором", eventId);
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (request.getStateAction() != null) {
            if (request.getStateAction() == AdminEventStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие можно публиковать, только если оно в состоянии ожидания публикации");
                }
                LocalDateTime now = LocalDateTime.now();
                if (event.getEventDate().isBefore(now.plusHours(1))) {
                    throw new ConflictException("Дата начала события должна быть не ранее чем за час от даты публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now);
            } else if (request.getStateAction() == AdminEventStateAction.REJECT_EVENT) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Событие можно отклонить, только если оно еще не опубликовано");
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (request.getEventDate() != null) {
            LocalDateTime newDate = LocalDateTime.parse(request.getEventDate(), EventMapper.FORMATTER);
            if (newDate.isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Дата события не может быть в прошлом");
            }
        }

        if (request.getCategory() != null) {
            event.setCategory(categoryRepository.findById(request.getCategory()).orElseThrow(() -> new NotFoundException("Категория с id=" + request.getCategory() + " не найдена")));
        }

        EventMapper.updateEntityFromAdminRequest(request, event);
        eventRepository.save(event);
        log.debug("Событие обновлено администратором");
        Long confirmed = getConfirmedRequestsMap(List.of(event)).getOrDefault(eventId, 0L);
        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());
        return EventMapper.toFullDto(event, initiator, confirmed, 0L);
    }

    @Override
    public List<EventFullDto> searchEventsByRadius(Float lat, Float lon, Float radius, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> page = eventRepository.findEventsByRadius(lat, lon, radius, pageable);

        if (page.isEmpty()) {
            return Collections.emptyList();
        }

        List<Event> events = page.getContent();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);
        Map<Long, UserShortDto> initiators = getInitiatorsMap(events);
        Map<Long, Long> views = getViewsMap(events);

        return events.stream()
                .map(e -> EventMapper.toFullDto(e, initiators.get(e.getInitiatorId()),
                        confirmedRequests.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .collect(Collectors.toList());
    }

    @Override
    public EventInternalDto getEventInternal(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        return EventMapper.toInternalDto(event);
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();

        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toList());
        LocalDateTime start = events.stream().map(Event::getCreatedOn).min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(10));

        List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStatsDto stat : stats) {
            String uri = stat.getUri();
            try {
                Long eventId = Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
                viewsMap.put(eventId, stat.getHits());
            } catch (NumberFormatException ignored) {
                // uri не относится к конкретному событию
            }
        }
        return viewsMap;
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        return requestClient.getConfirmedRequestsCounts(new EventIdListDto(eventIds)).stream()
                .collect(Collectors.toMap(ConfirmedRequestsCountDto::getEventId, ConfirmedRequestsCountDto::getCount));
    }

    private Map<Long, UserShortDto> getInitiatorsMap(List<Event> events) {
        if (events.isEmpty()) return Collections.emptyMap();
        List<Long> initiatorIds = events.stream().map(Event::getInitiatorId).distinct().collect(Collectors.toList());
        return userClient.getUsersShort(initiatorIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, dto -> dto));
    }

    @Override
    public List<EventShortDto> getEventsPublic(EventSearchParams params) {
        BooleanExpression predicate = EventPredicate.build(params);
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize(), getSort(params.getSort()));
        Page<Event> page = eventRepository.findAll(predicate, pageable);

        List<Event> events = page.getContent();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);
        Map<Long, UserShortDto> initiators = getInitiatorsMap(events);

        List<EventShortDto> list = events.stream()
                .map(e -> EventMapper.toShortDto(e, initiators.get(e.getInitiatorId()), confirmedRequests.getOrDefault(e.getId(), 0L), 0L))
                .toList();
        log.info("Список событий после фильтрации {}", list);
        return list;
    }

    private Sort getSort(String sort) {
        // Сортировка по views пока не поддержана - это поле не хранится в БД
        // event-service, а приходит из stats-service. Оставлено на будущее.
        return Sort.by(Sort.Direction.ASC, "eventDate");
    }

    @Override
    public EventFullDto getEventPublic(Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Событие " + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие должно быть опубликовано");
        }

        long views = getViews(eventId, event);
        Long confirmedRequests = getConfirmedRequestsMap(List.of(event)).getOrDefault(eventId, 0L);
        UserShortDto initiator = userClient.getUserShort(event.getInitiatorId());

        return EventMapper.toFullDto(event, initiator, confirmedRequests, views);
    }

    private Long getViews(Long eventId, Event event) {
        List<ViewStatsDto> stats = statsClient.getStats(event.getCreatedOn(), LocalDateTime.now(), List.of("/events/" + eventId), true);
        return stats.isEmpty() ? 0L : stats.get(0).getHits();
    }
}