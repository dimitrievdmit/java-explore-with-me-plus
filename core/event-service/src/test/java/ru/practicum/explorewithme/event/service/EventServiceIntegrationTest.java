package ru.practicum.explorewithme.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.category.dto.NewCategoryRequest;
import ru.practicum.explorewithme.category.service.CategoryService;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.event.enums.UserEventStateAction;
import ru.practicum.explorewithme.interaction.dto.*;
import ru.practicum.explorewithme.interaction.feign.RequestClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EventServiceIntegrationTest {

    @Autowired
    private EventService eventService;
    @MockBean
    private UserClient userClient;
    @MockBean
    private RequestClient requestClient;
    @Autowired
    private CategoryService categoryService;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long categoryId;
    private static final Long USER1_ID = 1L;
    private static final Long USER2_ID = 2L;

    @BeforeEach
    void setUp() {
        CategoryDto cat = categoryService.createCategory(new NewCategoryRequest("Тестовая категория"));
        categoryId = cat.getId();

        when(userClient.getUserShort(anyLong())).thenAnswer(inv ->
                new UserShortDto(inv.getArgument(0), "User " + inv.getArgument(0)));
        when(userClient.getUsersShort(anyList())).thenReturn(
                List.of(new UserShortDto(USER1_ID, "User 1"), new UserShortDto(USER2_ID, "User 2")));
    }

    @Test
    void fullLifecycleTest() {
        when(userClient.getUserShort(anyLong())).thenAnswer(inv ->
                new UserShortDto(inv.getArgument(0), "User " + inv.getArgument(0)));

        NewEventDto newEvent = buildValidDto(categoryId, LocalDateTime.now().plusDays(3));
        EventFullDto created = eventService.addEvent(USER1_ID, newEvent);
        assertThat(created.getState()).isEqualTo(EventState.PENDING);

        // обновление заголовка
        UpdateEventUserRequest update = UpdateEventUserRequest.builder().title("Updated title").build();
        EventFullDto updated = eventService.updateEvent(USER1_ID, created.getId(), update);
        assertThat(updated.getTitle()).isEqualTo("Updated title");

        // отмена
        update = UpdateEventUserRequest.builder().stateAction(UserEventStateAction.CANCEL_REVIEW).build();
        EventFullDto canceled = eventService.updateEvent(USER1_ID, created.getId(), update);
        assertThat(canceled.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void getEvents_Pagination() {
        for (int i = 0; i < 5; i++) {
            eventService.addEvent(USER1_ID, buildValidDto(categoryId, LocalDateTime.now().plusDays(2 + i)));
        }
        var page1 = eventService.getEvents(USER1_ID, 0, 2);
        assertThat(page1).hasSizeLessThanOrEqualTo(2);
    }

    private NewEventDto buildValidDto(Long categoryId, LocalDateTime eventDate) {
        return NewEventDto.builder()
                .annotation("Valid annotation for testing")
                .category(categoryId)
                .description("Valid description for testing")
                .eventDate(eventDate.format(fmt))
                .location(new GeoPointDto(55.75f, 37.62f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Test event")
                .build();
    }
}