package ru.practicum.explorewithme.event.service.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import ru.practicum.explorewithme.event.dto.EventSearchParams;
import ru.practicum.explorewithme.event.dto.EventSearchParamsAdmin;
import ru.practicum.explorewithme.event.model.QEvent;
import ru.practicum.explorewithme.interaction.dto.EventState;

public class EventPredicate {
    public static BooleanExpression build(EventSearchParams params) {
        QEvent event = QEvent.event;

        BooleanExpression predicate = event.state.eq(EventState.PUBLISHED);

        if (params.getText() != null && !params.getText().isBlank()) {
            String text = params.getText().trim();
            predicate = predicate.and(
                    event.annotation.containsIgnoreCase(text)
                            .or(event.description.containsIgnoreCase(text)));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(params.getCategories()));
        }

        if (params.getPaid() != null) {
            predicate = predicate.and(event.paid.eq(params.getPaid()));
        }

        if (params.getRangeStart() != null) {
            predicate = predicate.and(event.eventDate.goe(params.getRangeStart()));
        } else {
            predicate = predicate.and(event.eventDate.goe(java.time.LocalDateTime.now()));
        }

        if (params.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.loe(params.getRangeEnd()));
        }

        return predicate;
    }

    public static BooleanExpression buildAdmin(EventSearchParamsAdmin params) {
        QEvent event = QEvent.event;
        BooleanExpression predicate = event.isNotNull();

        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            predicate = predicate.and(event.initiatorId.in(params.getUsers()));
        }

        if (params.getStates() != null && !params.getStates().isEmpty()) {
            predicate = predicate.and(event.state.in(params.getStates()));
        }

        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            predicate = predicate.and(event.category.id.in(params.getCategories()));
        }

        if (params.getRangeStart() != null) {
            predicate = predicate.and(event.eventDate.goe(params.getRangeStart()));
        }

        if (params.getRangeEnd() != null) {
            predicate = predicate.and(event.eventDate.loe(params.getRangeEnd()));
        }

        return predicate;
    }
}
