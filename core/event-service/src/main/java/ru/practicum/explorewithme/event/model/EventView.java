package ru.practicum.explorewithme.event.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

//ToDo добавить создание таблицы event_views в schema.sql
@Entity
@Table(name = "event_views", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
