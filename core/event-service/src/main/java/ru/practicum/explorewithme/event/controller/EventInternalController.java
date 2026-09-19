package ru.practicum.explorewithme.event.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.event.service.EventService;
import ru.practicum.explorewithme.interaction.api.EventInternalApi;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventInternalController implements EventInternalApi {

    private final EventService eventService;

    @Override
    public EventInternalDto getEventInternal(Long eventId) {
        return eventService.getEventInternal(eventId);
    }

    @Override
    public List<EventFullDto> searchByRadius(Float lat, Float lon, Float radius, Integer from, Integer size) {
        return eventService.searchEventsByRadius(lat, lon, radius, from, size);
    }
}