package ru.practicum.explorewithme.location.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.exception.handler.ErrorHandler;
import ru.practicum.explorewithme.location.dto.LocationDto;
import ru.practicum.explorewithme.location.dto.NewLocationRequest;
import ru.practicum.explorewithme.location.service.LocationService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationAdminController.class)
@Import(ErrorHandler.class)
public class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private LocationService locationService;

    private NewLocationRequest request;
    private LocationDto location;
    Long locId;

    @BeforeEach
    void setUp() {
        locId = 1L;
        request = NewLocationRequest.builder().name("Moscow").lat(55.7558f).lon(37.6173f).radius(300.00f).build();
        location = LocationDto.builder().name("Moscow").id(1L).lat(55.7558f).lon(37.6173f).radius(300.00f).build();
    }

    @Test
    void shouldSaveNewLocation() throws Exception {
        Mockito.when(locationService.createLocation(any(NewLocationRequest.class))).thenReturn(location);

        mockMvc.perform(post("/admin/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Moscow"));
    }

    @Test
    void shouldThrowExceptionWhenLocationNotFound() throws Exception {
        doThrow(new NotFoundException("Локация" + locId + " не найдена")).when(locationService).deleteLocation(locId);

        mockMvc.perform(delete("/admin/locations/{locId}", locId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("Требуемый объект не найден"));
    }

    @Test
    void shouldReturnEventList_whenLocationExists() throws Exception {
        EventFullDto dto = EventFullDto.builder().id(10L).title("Test Event").build();
        when(locationService.getEventsByLocation(eq(locId), eq(0), eq(10))).thenReturn(List.of(dto));

        mockMvc.perform(get("/admin/locations/{locId}/events", locId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));

        verify(locationService).getEventsByLocation(locId, 0, 10);
    }

    @Test
    void shouldReturn404_whenLocationNotFound() throws Exception {
        when(locationService.getEventsByLocation(eq(locId), eq(0), eq(10)))
                .thenThrow(new NotFoundException("Локация " + locId + " не найдена"));

        mockMvc.perform(get("/admin/locations/{locId}/events", locId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WithEmptyList_whenNoEvents() throws Exception {
        when(locationService.getEventsByLocation(eq(locId), eq(0), eq(10))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/locations/{locId}/events", locId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}