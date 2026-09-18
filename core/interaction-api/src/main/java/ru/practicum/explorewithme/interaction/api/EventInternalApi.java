package ru.practicum.explorewithme.interaction.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;

import java.util.List;

public interface EventInternalApi {

    // используется request-service для валидации при создании заявки
    @GetMapping("/internal/events/{eventId}")
    EventInternalDto getEventInternal(@PathVariable("eventId") Long eventId);

    // используется location-service вместо прямого SQL-джойна с admin_locations
    @GetMapping("/internal/events/search-by-radius")
    List<EventFullDto> searchByRadius(@RequestParam("lat") Float lat,
                                      @RequestParam("lon") Float lon,
                                      @RequestParam("radius") Float radius,
                                      @RequestParam(value = "from", defaultValue = "0") Integer from,
                                      @RequestParam(value = "size", defaultValue = "10") Integer size);
}