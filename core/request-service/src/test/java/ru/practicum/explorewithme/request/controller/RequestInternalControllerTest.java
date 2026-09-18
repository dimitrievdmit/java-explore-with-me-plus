package ru.practicum.explorewithme.request.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.request.service.EventRequestService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestInternalController.class)
class RequestInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventRequestService eventRequestService;

    @Test
    void getConfirmedRequestsCounts_Success() throws Exception {
        when(eventRequestService.getConfirmedRequestsCounts(any(EventIdListDto.class)))
                .thenReturn(List.of(
                        new ConfirmedRequestsCountDto(1L, 3L),
                        new ConfirmedRequestsCountDto(2L, 0L)
                ));

        mockMvc.perform(post("/internal/requests/confirmed-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventIdListDto(List.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventId").value(1))
                .andExpect(jsonPath("$[0].count").value(3))
                .andExpect(jsonPath("$[1].eventId").value(2))
                .andExpect(jsonPath("$[1].count").value(0));
    }

    @Test
    void getConfirmedRequestsCounts_EmptyEventIds_ShouldReturnEmptyArray() throws Exception {
        when(eventRequestService.getConfirmedRequestsCounts(any(EventIdListDto.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/internal/requests/confirmed-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventIdListDto(List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getConfirmedRequestsCounts_NoConfirmedRequests_ShouldReturnEmptyArray() throws Exception {
        when(eventRequestService.getConfirmedRequestsCounts(any(EventIdListDto.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/internal/requests/confirmed-counts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EventIdListDto(List.of(5L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}