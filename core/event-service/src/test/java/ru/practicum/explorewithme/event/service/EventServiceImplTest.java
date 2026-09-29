package ru.practicum.explorewithme.event.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendedEventProto;
import ru.practicum.explorewithme.category.dal.CategoryRepository;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.event.dal.EventRepository;
import ru.practicum.explorewithme.event.dal.EventViewRepository;
import ru.practicum.explorewithme.event.dto.EventSearchParams;
import ru.practicum.explorewithme.event.dto.NewEventDto;
import ru.practicum.explorewithme.event.dto.UpdateEventUserRequest;
import ru.practicum.explorewithme.event.enums.UserEventStateAction;
import ru.practicum.explorewithme.event.mapper.EventMapper;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.GeoPoint;
import ru.practicum.explorewithme.interaction.dto.*;
import ru.practicum.explorewithme.interaction.exception.BadRequestException;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.RequestClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;
import ru.practicum.explorewithme.interaction.grpc.AnalyzerGrpcClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private RequestClient requestClient;
    @Mock
    private AnalyzerGrpcClient analyzerGrpcClient;
    @Mock
    private CollectorGrpcClient collectorGrpcClient;
    @Mock
    private EventViewRepository eventViewRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private static final Long USER_ID = 1L;
    private Category category;
    private NewEventDto newEventDto;
    private Event event;
    private UserShortDto userShortDto;

    @BeforeEach
    void setUp() {
        category = new Category(1L, "Концерты");
        userShortDto = new UserShortDto(USER_ID, "User");
        newEventDto = NewEventDto.builder()
                .annotation("Valid annotation for testing")
                .category(1L)
                .description("Valid description for testing")
                .eventDate("2030-12-31 15:10:05")
                .location(new GeoPointDto(55.75f, 37.62f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Valid title")
                .build();
        event = new Event();
        event.setCategory(category);
        event.setInitiatorId(USER_ID);
        event.setAnnotation("Valid annotation for testing");
        event.setDescription("Valid description for testing");
        event.setEventDate(LocalDateTime.parse("2030-12-31T15:10:05"));
        event.setLocation(new GeoPoint(55.75f, 37.62f));
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setTitle("Valid title");
    }

    @Test
    void addEvent_Success() {
        when(userClient.getUserShort(USER_ID)).thenReturn(userShortDto);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        Event savedEvent = EventMapper.toEntity(newEventDto);
        savedEvent.setId(1L);
        savedEvent.setInitiatorId(USER_ID);
        savedEvent.setCategory(category);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventFullDto result = eventService.addEvent(USER_ID, newEventDto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getState()).isEqualTo(EventState.PENDING);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void addEvent_DateTooEarly_ShouldThrowConflict() {
        newEventDto.setEventDate(LocalDateTime.now().plusHours(1).format(EventMapper.FORMATTER));

        assertThatThrownBy(() -> eventService.addEvent(USER_ID, newEventDto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Дата события должна быть не ранее чем через 2 часа");
    }

    @Test
    void getEvents_ShouldApplyPagination() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());
        when(eventRepository.findAllByInitiatorId(USER_ID, pageable)).thenReturn(new PageImpl<>(List.of(event)));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUserShort(USER_ID)).thenReturn(userShortDto);

        List<EventShortDto> result = eventService.getEvents(USER_ID, 0, 10);
        assertThat(result).hasSize(1);
    }

    @Test
    void getEvent_ShouldReturnFullDto() {
        Event event = createEventWithDefaults();
        when(eventRepository.findByIdAndInitiatorId(1L, USER_ID)).thenReturn(Optional.of(event));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUserShort(USER_ID)).thenReturn(userShortDto);

        EventFullDto result = eventService.getEvent(USER_ID, 1L);
        assertThat(result.getId()).isEqualTo(event.getId());
    }

    @Test
    void getEvent_NotFound_ShouldThrowNotFound() {
        when(eventRepository.findByIdAndInitiatorId(999L, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventService.getEvent(USER_ID, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateEvent_ChangeStateToCancel() {
        Event event = createEventWithDefaults();
        event.setState(EventState.PENDING);
        when(eventRepository.findByIdAndInitiatorId(1L, USER_ID)).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUserShort(USER_ID)).thenReturn(userShortDto);

        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .stateAction(UserEventStateAction.CANCEL_REVIEW)
                .build();

        EventFullDto result = eventService.updateEvent(USER_ID, 1L, request);
        assertThat(result.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateEvent_Published_ShouldThrowConflict() {
        Event event = createEventWithDefaults();
        event.setState(EventState.PUBLISHED);
        when(eventRepository.findByIdAndInitiatorId(1L, USER_ID)).thenReturn(Optional.of(event));

        UpdateEventUserRequest request = UpdateEventUserRequest.builder().title("New").build();
        assertThatThrownBy(() -> eventService.updateEvent(USER_ID, 1L, request))
                .isInstanceOf(ConflictException.class);
    }

    private Event createEventWithDefaults() {
        Event event = new Event();
        event.setId(1L);
        event.setCategory(category);
        event.setInitiatorId(USER_ID);
        event.setAnnotation("annotation");
        event.setDescription("description");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setLocation(new GeoPoint(55f, 37f));
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
        event.setState(EventState.PENDING);
        event.setTitle("title");
        event.setCreatedOn(LocalDateTime.now());
        return event;
    }

    @Test
    void shouldReturnEvents() {
        EventSearchParams params = EventSearchParams.builder().from(0).size(10).sort("rating").build();

        Event event = createEventWithDefaults();
        event.setState(EventState.PUBLISHED);
        Page<Event> page = new PageImpl<>(List.of(event));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(eventRepository.findAll(any(BooleanExpression.class), pageableCaptor.capture())).thenReturn(page);
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUsersShort(anyList())).thenReturn(List.of(userShortDto));
        when(analyzerGrpcClient.getInteractionsCount(anyList())).thenReturn(Map.of(event.getId(), 7.5));

        List<EventShortDto> result = eventService.getEventsPublic(params);

        assertFalse(result.isEmpty());
        assertEquals(event.getTitle(), result.get(0).getTitle());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("rating")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("rating").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(result.get(0).getRating()).isEqualTo(7.5);
    }

    @Test
    void shouldReturnEventById() {
        Event event = createEventWithDefaults();
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUserShort(USER_ID)).thenReturn(userShortDto);

        EventFullDto dto = eventService.getEventPublic(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        verify(eventRepository).findById(1L);
    }

    @Test
    void searchEventsByRadius_shouldReturnEventDtos_whenEventsFound() {
        Event e = createEventWithDefaults();
        e.setId(10L);
        Page<Event> page = new PageImpl<>(List.of(e));
        when(eventRepository.findEventsByRadius(eq(55.75f), eq(37.62f), eq(50f), any(Pageable.class))).thenReturn(page);
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUsersShort(anyList())).thenReturn(List.of(userShortDto));

        List<EventFullDto> result = eventService.searchEventsByRadius(55.75f, 37.62f, 50f, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    @Test
    void searchEventsByRadius_shouldReturnEmptyList_whenNoEventsInRadius() {
        when(eventRepository.findEventsByRadius(eq(-90f), eq(0f), eq(1f), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<EventFullDto> result = eventService.searchEventsByRadius(-90f, 0f, 1f, 0, 10);

        assertThat(result).isEmpty();
        verify(analyzerGrpcClient, never()).getInteractionsCount(anyList());
    }

    @Test
    void registerView_ShouldSendActionAndSaveFirstViewOnly() {
        Event publishedEvent = createEventWithDefaults();
        publishedEvent.setState(EventState.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(publishedEvent));
        when(eventViewRepository.existsByUserIdAndEventId(USER_ID, 1L)).thenReturn(false);

        eventService.registerView(USER_ID, 1L);

        verify(collectorGrpcClient).collectUserAction(eq(USER_ID), eq(1L),
                eq(ActionTypeProto.ACTION_VIEW), any());
        verify(eventViewRepository).save(any());
    }

    @Test
    void registerView_ShouldNotDuplicateExistingView() {
        Event publishedEvent = createEventWithDefaults();
        publishedEvent.setState(EventState.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(publishedEvent));
        when(eventViewRepository.existsByUserIdAndEventId(USER_ID, 1L)).thenReturn(true);

        eventService.registerView(USER_ID, 1L);

        verify(collectorGrpcClient).collectUserAction(eq(USER_ID), eq(1L),
                eq(ActionTypeProto.ACTION_VIEW), any());
        verify(eventViewRepository, never()).save(any());
    }

    @Test
    void likeEvent_WithoutView_ShouldThrowBadRequest() {
        when(eventViewRepository.existsByUserIdAndEventId(USER_ID, 1L)).thenReturn(false);

        assertThatThrownBy(() -> eventService.likeEvent(USER_ID, 1L))
                .isInstanceOf(BadRequestException.class);
        verify(collectorGrpcClient, never()).collectUserAction(anyLong(), anyLong(), any(), any());
    }

    @Test
    void likeEvent_AfterView_ShouldSendAction() {
        when(eventViewRepository.existsByUserIdAndEventId(USER_ID, 1L)).thenReturn(true);

        eventService.likeEvent(USER_ID, 1L);

        verify(collectorGrpcClient).collectUserAction(eq(USER_ID), eq(1L),
                eq(ActionTypeProto.ACTION_LIKE), any());
    }

    @Test
    void getRecommendationsForUser_ShouldReturnPublishedEventsInAnalyzerOrder() {
        RecommendedEventProto first = RecommendedEventProto.newBuilder().setEventId(1L).setScore(0.9).build();
        RecommendedEventProto second = RecommendedEventProto.newBuilder().setEventId(2L).setScore(0.8).build();
        Event published = createEventWithDefaults();
        published.setId(1L);
        published.setState(EventState.PUBLISHED);
        Event unpublished = createEventWithDefaults();
        unpublished.setId(2L);
        unpublished.setState(EventState.PENDING);

        when(analyzerGrpcClient.getRecommendationsForUser(USER_ID, 10))
                .thenReturn(java.util.stream.Stream.of(first, second));
        when(eventRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(published, unpublished));
        when(requestClient.getConfirmedRequestsCounts(any(EventIdListDto.class))).thenReturn(Collections.emptyList());
        when(userClient.getUsersShort(anyList())).thenReturn(List.of(userShortDto));
        when(analyzerGrpcClient.getInteractionsCount(anyList())).thenReturn(Map.of(1L, 5.0));

        List<EventShortDto> result = eventService.getRecommendationsForUser(USER_ID, 10);

        assertThat(result).extracting(EventShortDto::getId).containsExactly(1L);
        assertThat(result.getFirst().getRating()).isEqualTo(5.0);
    }

}
