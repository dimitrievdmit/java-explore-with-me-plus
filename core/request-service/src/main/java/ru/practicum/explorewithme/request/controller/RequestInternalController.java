package ru.practicum.explorewithme.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.interaction.api.RequestInternalApi;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.request.service.EventRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RequestInternalController implements RequestInternalApi {

    private final EventRequestService eventRequestService;

    @Override
    public List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(EventIdListDto eventIdListDto) {
        return eventRequestService.getConfirmedRequestsCounts(eventIdListDto);
    }
}