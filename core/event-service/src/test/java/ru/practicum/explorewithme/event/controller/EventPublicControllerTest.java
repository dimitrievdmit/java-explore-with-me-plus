package ru.practicum.explorewithme.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.event.service.EventService;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.dto.EventShortDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventPublicController.class)
class EventPublicControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EventService eventService;

    @Test
    void shouldReturnEventsWithRatingSort() throws Exception {
        List<EventShortDto> events = List.of(EventShortDto.builder().id(1L).rating(5.0).build());
        when(eventService.getEventsPublic(any())).thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "concert")
                        .param("categories", "1", "2")
                        .param("paid", "true")
                        .param("rangeStart", "2025-01-01 10:00:00")
                        .param("rangeEnd", "2025-01-02 10:00:00")
                        .param("onlyAvailable", "true")
                        .param("sort", "rating")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5.0));

        verify(eventService).getEventsPublic(argThat(params -> "rating".equals(params.getSort())));
    }

    @Test
    void shouldReturn400WhenEndBeforeStart() throws Exception {
        mockMvc.perform(get("/events")
                        .param("rangeStart", "2025-01-02 10:00:00")
                        .param("rangeEnd", "2025-01-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRegisterViewBeforeReturningEvent() throws Exception {
        Long eventId = 1L;
        Long userId = 42L;
        EventFullDto event = EventFullDto.builder().id(eventId).rating(5.0).build();
        when(eventService.getEventPublic(eventId)).thenReturn(event);

        mockMvc.perform(get("/events/{id}", eventId)
                        .header("X-EWM-USER-ID", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.rating").value(5.0));

        verify(eventService).registerView(userId, eventId);
        verify(eventService).getEventPublic(eventId);
    }

    @Test
    void shouldReturnRecommendationsForUser() throws Exception {
        when(eventService.getRecommendationsForUser(42L, 10))
                .thenReturn(List.of(EventShortDto.builder().id(10L).rating(2.5).build()));

        mockMvc.perform(get("/events/recommendations")
                        .header("X-EWM-USER-ID", 42L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));

        verify(eventService).getRecommendationsForUser(42L, 10);
    }

    @Test
    void shouldLikeEventForUser() throws Exception {
        mockMvc.perform(put("/events/{eventId}/like", 10L)
                        .header("X-EWM-USER-ID", 42L))
                .andExpect(status().isOk());

        verify(eventService).likeEvent(42L, 10L);
    }
}
