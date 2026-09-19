package ru.practicum.explorewithme.event.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeoPoint {
    @Column(nullable = false)
    Float lat;

    @Column(nullable = false)
    Float lon;
}