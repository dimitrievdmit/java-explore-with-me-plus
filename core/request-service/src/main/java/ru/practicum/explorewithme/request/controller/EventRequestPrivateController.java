package ru.practicum.explorewithme.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.service.EventRequestService;

import java.util.List;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class EventRequestPrivateController {

    private final EventRequestService eventRequestService;

    @GetMapping
    public List<ParticipationRequestDto> getEventRequests(@PathVariable Long userId,
                                                          @PathVariable Long eventId) {
        return eventRequestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult updateEventRequests(@PathVariable Long userId,
                                                              @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest request) {
        return eventRequestService.updateEventRequests(userId, eventId, request);
    }
}
