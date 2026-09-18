package ru.practicum.explorewithme.interaction.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;

import java.util.List;

public interface RequestInternalApi {

    // батч, чтобы event-service не делал N+1 при листинге событий
    @PostMapping("/internal/requests/confirmed-counts")
    List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(@RequestBody @Valid EventIdListDto eventIdListDto);

}