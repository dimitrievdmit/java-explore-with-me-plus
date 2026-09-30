package ru.practicum.explorewithme.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.dto.EventState;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.feign.EventClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;
import ru.practicum.explorewithme.request.dal.EventRequestRepository;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.enums.ParticipationRequestStatus;
import ru.practicum.explorewithme.request.model.ParticipationRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRequestServiceImplTest {

    @Mock
    private EventRequestRepository eventRequestRepository;
    @Mock
    private EventClient eventClient;
    @Mock
    private UserClient userClient;
    @Mock
    private CollectorGrpcClient collectorGrpcClient;

    @InjectMocks
    private EventRequestServiceImpl requestService;

    private static final Long OWNER_ID = 1L;
    private static final Long EVENT_ID = 1L;
    private EventInternalDto eventInternalDto;

    @BeforeEach
    void setUp() {
        eventInternalDto = EventInternalDto.builder()
                .id(EVENT_ID)
                .initiatorId(OWNER_ID)
                .state(EventState.PUBLISHED)
                .participantLimit(2)
                .requestModeration(true)
                .build();
    }

    @Test
    void getEventRequests_Success() {
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);
        when(eventRequestRepository.findAllByEventId(EVENT_ID)).thenReturn(List.of());

        List<ParticipationRequestDto> result = requestService.getEventRequests(OWNER_ID, EVENT_ID);
        assertThat(result).isEmpty();
    }

    @Test
    void updateEventRequests_ConfirmWithRemainingLimit() {
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);

        ParticipationRequest req = ParticipationRequest.builder()
                .id(10L)
                .requesterId(2L)
                .eventId(EVENT_ID)
                .status(ParticipationRequestStatus.PENDING)
                .build();

        when(eventRequestRepository.findAllByIdInAndStatus(List.of(10L), ParticipationRequestStatus.PENDING))
                .thenReturn(List.of(req));
        when(eventRequestRepository.countByEventIdAndStatus(EVENT_ID, ParticipationRequestStatus.CONFIRMED)).thenReturn(0);
        when(eventRequestRepository.saveAll(anyList())).thenReturn(List.of());

        EventRequestStatusUpdateRequest updateReq = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(10L))
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();

        EventRequestStatusUpdateResult result = requestService.updateEventRequests(OWNER_ID, EVENT_ID, updateReq);

        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).isEmpty();
    }

    @Test
    void updateEventRequests_ConflictWhenPrerequisitesNotMet() {
        EventInternalDto noModerationEvent = EventInternalDto.builder()
                .id(EVENT_ID).initiatorId(OWNER_ID).state(EventState.PUBLISHED)
                .participantLimit(0).requestModeration(true).build();
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(noModerationEvent);

        EventRequestStatusUpdateRequest updateReq = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();
        assertThatThrownBy(() -> requestService.updateEventRequests(OWNER_ID, EVENT_ID, updateReq))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Подтверждение заявок не требуется");
    }

    @Test
    void saveEventParticipation_Success() {
        when(eventRequestRepository.existsByRequesterIdAndEventId(2L, EVENT_ID)).thenReturn(false);
        when(userClient.getUserShort(2L)).thenReturn(new UserShortDto(2L, "Requester"));
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);
        when(eventRequestRepository.countByEventIdAndStatus(
                EVENT_ID, ParticipationRequestStatus.CONFIRMED)).thenReturn(1);
        when(eventRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParticipationRequestDto result = requestService.saveEventParticipation(2L, EVENT_ID);

        assertThat(result.getStatus()).isEqualTo(ParticipationRequestStatus.PENDING);
    }

    @Test
    void saveEventParticipation_InitiatorCannotJoinOwnEvent() {
        when(eventRequestRepository.existsByRequesterIdAndEventId(OWNER_ID, EVENT_ID)).thenReturn(false);
        when(userClient.getUserShort(OWNER_ID)).thenReturn(new UserShortDto(OWNER_ID, "Owner"));
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);

        assertThatThrownBy(() -> requestService.saveEventParticipation(OWNER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Инициатор не может присылать запрос");
    }

    @Test
    void saveEventParticipation_AllowsPendingRequestWhenParticipantLimitNotReached() {
        when(eventRequestRepository.existsByRequesterIdAndEventId(2L, EVENT_ID)).thenReturn(false);
        when(userClient.getUserShort(2L)).thenReturn(new UserShortDto(2L, "Requester"));
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);
        when(eventRequestRepository.countByEventIdAndStatus(
                EVENT_ID, ParticipationRequestStatus.CONFIRMED)).thenReturn(1);
        when(eventRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ParticipationRequestDto result = requestService.saveEventParticipation(2L, EVENT_ID);

        assertThat(result.getStatus()).isEqualTo(ParticipationRequestStatus.PENDING);
    }

    @Test
    void updateEventRequests_ConflictWhenParticipantLimitExceeded() {
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(eventInternalDto);

        ParticipationRequest first = ParticipationRequest.builder()
                .id(10L)
                .requesterId(2L)
                .eventId(EVENT_ID)
                .status(ParticipationRequestStatus.PENDING)
                .build();
        ParticipationRequest second = ParticipationRequest.builder()
                .id(11L)
                .requesterId(3L)
                .eventId(EVENT_ID)
                .status(ParticipationRequestStatus.PENDING)
                .build();

        List<Long> requestIds = List.of(10L, 11L);
        when(eventRequestRepository.findAllByIdInAndStatus(requestIds, ParticipationRequestStatus.PENDING))
                .thenReturn(List.of(first, second));
        when(eventRequestRepository.countByEventIdAndStatus(
                EVENT_ID, ParticipationRequestStatus.CONFIRMED)).thenReturn(1);

        EventRequestStatusUpdateRequest updateReq = EventRequestStatusUpdateRequest.builder()
                .requestIds(requestIds)
                .status(ParticipationRequestStatus.CONFIRMED)
                .build();

        assertThatThrownBy(() -> requestService.updateEventRequests(OWNER_ID, EVENT_ID, updateReq))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Количество участников события не может превышать 2");
    }
}
