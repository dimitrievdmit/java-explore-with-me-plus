package ru.practicum.explorewithme.stats.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_event_interactions")
@IdClass(UserEventInteractionId.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserEventInteraction {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Instant timestamp;
}
