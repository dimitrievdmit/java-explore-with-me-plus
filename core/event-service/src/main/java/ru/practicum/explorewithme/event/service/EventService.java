package ru.practicum.explorewithme.event.service;

import ru.practicum.explorewithme.event.dto.*;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.dto.EventShortDto;

import java.util.List;

public interface EventService {
    EventFullDto addEvent(Long userId, NewEventDto newEventDto);

    List<EventShortDto> getEvents(Long userId, int from, int size);

    EventFullDto getEvent(Long userId, Long eventId);

    EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventShortDto> getEventsPublic(EventSearchParams params);

    EventFullDto getEventPublic(Long eventId);

    List<EventShortDto> getRecommendationsForUser(Long userId, int maxResults);

    void registerView(Long userId, Long eventId);

    void likeEvent(Long userId, Long eventId);

    List<EventFullDto> getEventsByAdmin(EventSearchParamsAdmin params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    EventInternalDto getEventInternal(Long eventId);

    List<EventFullDto> searchEventsByRadius(Float lat, Float lon, Float radius, int from, int size);
}
