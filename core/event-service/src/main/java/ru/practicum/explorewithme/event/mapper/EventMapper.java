package ru.practicum.explorewithme.event.mapper;

import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.dto.UpdateEventAdminRequest;
import ru.practicum.explorewithme.event.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.GeoPoint;
import ru.practicum.explorewithme.interaction.dto.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class EventMapper {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event toEntity(NewEventDto dto) {
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setEventDate(LocalDateTime.parse(dto.getEventDate(), FORMATTER));
        event.setLocation(new GeoPoint(
                dto.getLocation().getLat(), dto.getLocation().getLon()));
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setTitle(dto.getTitle());
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }

    public static EventFullDto toFullDto(Event event, UserShortDto initiator, Long confirmedRequests, Double rating) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(initiator)
                .location(new GeoPointDto(event.getLocation().getLat(), event.getLocation().getLon()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(FORMATTER) : null)
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                .rating(rating != null ? rating : 0.0)
                .build();
    }

    public static EventShortDto toShortDto(Event event, UserShortDto initiator, Long confirmedRequests, Double rating) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .rating(rating != null ? rating : 0.0)
                .build();
    }

    public static void updateEntityFromRequest(UpdateEventUserRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(LocalDateTime.parse(request.getEventDate(), FORMATTER));
        if (request.getLocation() != null)
            event.setLocation(new GeoPoint(
                    request.getLocation().getLat(), request.getLocation().getLon()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    public static void updateEntityFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventDate() != null) event.setEventDate(LocalDateTime.parse(request.getEventDate(), FORMATTER));
        if (request.getLocation() != null)
            event.setLocation(new GeoPoint(
                    request.getLocation().getLat(), request.getLocation().getLon()));
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
    }

    public static EventInternalDto toInternalDto(Event event) {
        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .build();
    }
}