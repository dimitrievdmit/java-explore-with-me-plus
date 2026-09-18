package ru.practicum.explorewithme.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.explorewithme.interaction.dto.EventState;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class EventSearchParamsAdmin {
    private List<Long> users;
    private List<EventState> states;
    private List<Long> categories;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private int from;
    private int size;
}
