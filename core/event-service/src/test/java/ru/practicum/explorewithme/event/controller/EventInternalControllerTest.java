package ru.practicum.explorewithme.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.event.service.EventService;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.dto.EventState;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventInternalController.class)
class EventInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EventService eventService;

    @Test
    void getEventInternal_Success() throws Exception {
        when(eventService.getEventInternal(1L)).thenReturn(EventInternalDto.builder()
                .id(1L).initiatorId(5L).state(EventState.PUBLISHED)
                .participantLimit(10).requestModeration(true).build());

        mockMvc.perform(get("/internal/events/{eventId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiatorId").value(5))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    void getEventInternal_NotFound_ShouldReturn404() throws Exception {
        when(eventService.getEventInternal(999L)).thenThrow(new NotFoundException("Событие не найдено"));

        mockMvc.perform(get("/internal/events/{eventId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByRadius_Success() throws Exception {
        when(eventService.searchEventsByRadius(55.75f, 37.62f, 50f, 0, 10)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/internal/events/search-by-radius")
                        .param("lat", "55.75").param("lon", "37.62").param("radius", "50")
                        .param("from", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}