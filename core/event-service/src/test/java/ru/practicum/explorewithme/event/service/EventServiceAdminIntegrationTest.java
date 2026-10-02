package ru.practicum.explorewithme.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.category.dto.NewCategoryRequest;
import ru.practicum.explorewithme.category.service.CategoryService;
import ru.practicum.explorewithme.event.dto.EventSearchParamsAdmin;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.interaction.dto.*;
import ru.practicum.explorewithme.interaction.feign.RequestClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;
import ru.practicum.explorewithme.interaction.grpc.AnalyzerGrpcClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EventServiceAdminIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private CategoryService categoryService;

    @MockBean
    private UserClient userClient;
    @MockBean
    private RequestClient requestClient;
    @MockBean
    private AnalyzerGrpcClient analyzerGrpcClient;
    @MockBean
    private CollectorGrpcClient collectorGrpcClient;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long catId;
    private static final Long USER1_ID = 1L;
    private static final Long USER2_ID = 2L;

    @BeforeEach
    void setUp() {
        CategoryDto cat = categoryService.createCategory(new NewCategoryRequest("Category Integration"));
        catId = cat.getId();

        when(userClient.getUserShort(anyLong())).thenAnswer(inv ->
                new UserShortDto(inv.getArgument(0), "User " + inv.getArgument(0)));
        when(userClient.getUsersShort(anyList())).thenReturn(
                List.of(new UserShortDto(USER1_ID, "User 1"), new UserShortDto(USER2_ID, "User 2")));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
    }

    @Test
    void getEventsByAdmin_FilterByUsers() {
        eventService.addEvent(USER1_ID, buildDto("Event 1", LocalDateTime.now().plusDays(1)));
        eventService.addEvent(USER2_ID, buildDto("Event 2", LocalDateTime.now().plusDays(2)));

        EventSearchParamsAdmin params = new EventSearchParamsAdmin(List.of(USER1_ID), null, null, null, null, 0, 10);
        List<EventFullDto> result = eventService.getEventsByAdmin(params);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInitiator().getId()).isEqualTo(USER1_ID);
    }

    @Test
    void getEventsByAdmin_FilterByStates() {
        eventService.addEvent(USER1_ID, buildDto("Event 1", LocalDateTime.now().plusDays(1)));
        EventSearchParamsAdmin params = new EventSearchParamsAdmin(
                null, List.of(EventState.PENDING), null, null, null, 0, 10);
        List<EventFullDto> result = eventService.getEventsByAdmin(params);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getState()).isEqualTo(EventState.PENDING);
    }

    private NewEventDto buildDto(String title, LocalDateTime eventDate) {
        return NewEventDto.builder()
                .annotation("Annotation for " + title + " that is long enough")
                .category(catId)
                .description("Description for " + title + " that is long enough")
                .eventDate(eventDate.format(fmt))
                .location(new GeoPointDto(55.0f, 37.0f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title(title)
                .build();
    }
}