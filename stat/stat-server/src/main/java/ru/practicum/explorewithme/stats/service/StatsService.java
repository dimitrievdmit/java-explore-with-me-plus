package ru.practicum.explorewithme.stats.service;


import ru.practicum.explorewithme.interaction.dto.EndpointHitDto;
import ru.practicum.explorewithme.interaction.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    /**
     * Сохранить информацию о запросе к эндпоинту.
     *
     * @param endpointHitDTO данные запроса (app, uri, ip, timestamp)
     */
    void hit(EndpointHitDto endpointHitDTO);

    /**
     * Получить статистику по посещениям за указанный период.
     *
     * @param start  дата и время начала диапазона (включительно)
     * @param end    дата и время конца диапазона (включительно)
     * @param uris   список URI для фильтрации (может быть null или пустым — тогда все URI)
     * @param unique учитывать только уникальные IP (true — да, false/null — нет)
     * @return список объектов статистики, отсортированный по убыванию количества просмотров
     */
    List<ViewStatsDto> getStats(LocalDateTime start,
                                LocalDateTime end,
                                List<String> uris,
                                Boolean unique);
}