package ru.practicum.explorewithme.location.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewLocationRequest {
    @NotBlank
    @Size(max = 120)
    String name;

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    Float lat;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    Float lon;

    @NotNull
    @Positive
    Float radius;
}
