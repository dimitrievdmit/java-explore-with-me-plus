package ru.practicum.explorewithme.interaction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventShortDto {
    Long id;
    String annotation;
    CategoryDto category;
    Long confirmedRequests;
    String eventDate;
    UserShortDto initiator;
    Boolean paid;
    String title;
    Double rating;
}