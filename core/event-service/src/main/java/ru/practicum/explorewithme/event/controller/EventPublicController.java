package ru.practicum.explorewithme.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.event.dto.EventSearchParams;
import ru.practicum.explorewithme.event.service.EventService;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventShortDto;
import ru.practicum.explorewithme.interaction.exception.BadRequestException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
public class EventPublicController {
    private static final int DEFAULT_RECOMMENDATIONS_LIMIT = 10;

    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(@RequestParam(required = false) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                         @RequestParam(required = false) Boolean onlyAvailable,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size) {
        if (rangeEnd != null && rangeStart != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeEnd должен быть позже rangeStart");
        }

        EventSearchParams params = EventSearchParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();
        return eventService.getEventsPublic(params);
    }

    @GetMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        return eventService.getRecommendationsForUser(userId, DEFAULT_RECOMMENDATIONS_LIMIT);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventByIdAndPublished(
            @PathVariable Long eventId,
            @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Запрос на получение события {} пользователем {}", eventId, userId);
        eventService.registerView(userId, eventId);
        return eventService.getEventPublic(eventId);
    }

    @PutMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Пользователь {} ставит лайк событию {}", userId, eventId);
        eventService.likeEvent(userId, eventId);
    }
}
