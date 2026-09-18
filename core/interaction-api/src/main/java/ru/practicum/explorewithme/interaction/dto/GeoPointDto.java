package ru.practicum.explorewithme.interaction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeoPointDto {
    Float lat;
    Float lon;
}
