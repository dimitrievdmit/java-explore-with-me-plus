package ru.practicum.explorewithme.interaction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventInternalDto {
    Long id;
    Long initiatorId;
    EventState state;
    Integer participantLimit;
    Boolean requestModeration;
}