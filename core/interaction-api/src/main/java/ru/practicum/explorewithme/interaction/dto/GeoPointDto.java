package ru.practicum.explorewithme.interaction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeoPointDto {
    @NotNull(message = "Широта должна быть указана")
    @DecimalMin(value = "-90", message = "Широта должна быть от -90 до 90")
    @DecimalMax(value = "90", message = "Широта должна быть от -90 до 90")
    Float lat;

    @NotNull(message = "Долгота должна быть указана")
    @DecimalMin(value = "-180", message = "Долгота должна быть от -180 до 180")
    @DecimalMax(value = "180", message = "Долгота должна быть от -180 до 180")
    Float lon;
}
