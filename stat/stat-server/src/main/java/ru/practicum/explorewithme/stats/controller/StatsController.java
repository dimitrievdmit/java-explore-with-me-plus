package ru.practicum.explorewithme.stats.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.interaction.api.StatsApi;
import ru.practicum.explorewithme.interaction.dto.EndpointHitDto;
import ru.practicum.explorewithme.interaction.dto.ViewStatsDto;
import ru.practicum.explorewithme.stats.exception.BadRequestException;
import ru.practicum.explorewithme.stats.service.StatsService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatsController implements StatsApi {

    private final StatsService statsService;

    @Override
    public void hit(EndpointHitDto endpointHitDTO) {
        statsService.hit(endpointHitDTO);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, Boolean unique) {
        if (end.isBefore(start)) {
            throw new BadRequestException("end должен быть позже start");
        }
        return statsService.getStats(start, end, uris, unique);
    }
}