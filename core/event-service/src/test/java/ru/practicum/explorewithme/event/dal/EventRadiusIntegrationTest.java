package ru.practicum.explorewithme.event.dal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.category.dal.CategoryRepository;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.dto.UpdateEventAdminRequest;
import ru.practicum.explorewithme.event.enums.AdminEventStateAction;
import ru.practicum.explorewithme.event.service.EventService;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.interaction.dto.GeoPointDto;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
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
class EventRadiusIntegrationTest {

    @Autowired
    private EventService eventService;
    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private UserClient userClient;
    @MockBean
    private RequestClient requestClient;
    @MockBean
    private AnalyzerGrpcClient analyzerGrpcClient;
    @MockBean
    private CollectorGrpcClient collectorGrpcClient;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Long USER_ID = 1L;

    @Test
    void searchEventsByRadius_returnsOnlyPublishedInsideRadius() {
        when(userClient.getUserShort(anyLong())).thenReturn(new UserShortDto(USER_ID, "User"));
        when(userClient.getUsersShort(anyList())).thenReturn(List.of(new UserShortDto(USER_ID, "User")));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(analyzerGrpcClient.getInteractionsCount(anyList())).thenReturn(Collections.emptyMap());

        Category category = categoryRepository.save(new Category(null, "RadiusTest"));

        Long inside1 = createAndPublish("Inside 1", 55.80f, 37.70f, category.getId());
        Long inside2 = createAndPublish("Inside 2", 55.70f, 37.55f, category.getId());
        createAndPublish("Outside SPb", 59.95f, 30.32f, category.getId());

        List<?> result = eventService.searchEventsByRadius(55.75f, 37.62f, 50f, 0, 10);

        assertThat(result).hasSize(2);
    }

    private Long createAndPublish(String title, float lat, float lon, Long categoryId) {
        var created = eventService.addEvent(USER_ID, NewEventDto.builder()
                .annotation("Annotation for " + title + " that is long enough")
                .category(categoryId)
                .description("Description for " + title + " that is long enough")
                .eventDate(LocalDateTime.now().plusDays(1).format(fmt))
                .location(new GeoPointDto(lat, lon))
                .paid(false).participantLimit(0).requestModeration(false)
                .title(title).build());

        eventService.updateEventByAdmin(created.getId(),
                UpdateEventAdminRequest.builder().stateAction(AdminEventStateAction.PUBLISH_EVENT).build());
        return created.getId();
    }
}