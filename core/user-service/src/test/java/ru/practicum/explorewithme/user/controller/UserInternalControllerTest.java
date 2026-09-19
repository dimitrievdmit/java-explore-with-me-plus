package ru.practicum.explorewithme.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.user.service.UserService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(ru.practicum.explorewithme.interaction.exception.handler.ErrorHandler.class)
@WebMvcTest(UserInternalController.class)
class UserInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getUserShort_Success() throws Exception {
        when(userService.getUserShort(1L)).thenReturn(new UserShortDto(1L, "Иван Иванов"));

        mockMvc.perform(get("/internal/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван Иванов"));
    }

    @Test
    void getUserShort_NotFound_ShouldReturn404() throws Exception {
        when(userService.getUserShort(999L)).thenThrow(new NotFoundException("Пользователь с id=999 не найден"));

        mockMvc.perform(get("/internal/users/{userId}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsersShort_Success() throws Exception {
        when(userService.getUsersShort(List.of(1L, 2L))).thenReturn(List.of(
                new UserShortDto(1L, "Иван Иванов"),
                new UserShortDto(2L, "Пётр Петров")
        ));

        mockMvc.perform(get("/internal/users").param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Иван Иванов"))
                .andExpect(jsonPath("$[1].name").value("Пётр Петров"));
    }

    @Test
    void getUsersShort_EmptyResult_ShouldReturn200WithEmptyArray() throws Exception {
        when(userService.getUsersShort(List.of(777L))).thenReturn(List.of());

        mockMvc.perform(get("/internal/users").param("ids", "777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}