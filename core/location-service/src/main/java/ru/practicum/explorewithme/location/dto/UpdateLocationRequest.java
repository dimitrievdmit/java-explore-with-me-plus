package ru.practicum.explorewithme.location.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateLocationRequest {
    @Size(max = 120)
    String name;

    @DecimalMin("-90")
    @DecimalMax("90")
    Float lat;

    @DecimalMin("-180")
    @DecimalMax("180")
    Float lon;

    @Positive
    Float radius;
}
