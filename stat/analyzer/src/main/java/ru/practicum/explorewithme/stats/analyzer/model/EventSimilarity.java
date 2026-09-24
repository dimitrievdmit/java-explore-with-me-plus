package ru.practicum.explorewithme.stats.analyzer.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "event_similarity")
@IdClass(EventSimilarityId.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventSimilarity {
    @Id
    @Column(name = "event_a")
    private Long eventA;

    @Id
    @Column(name = "event_b")
    private Long eventB;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Instant timestamp;
}
