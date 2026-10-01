package ru.practicum.explorewithme.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.proto.collector.ActionTypeProto;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.interaction.dto.EventIdListDto;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.dto.EventState;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.EventClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;
import ru.practicum.explorewithme.interaction.grpc.CollectorGrpcClient;
import ru.practicum.explorewithme.request.dal.EventRequestRepository;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explorewithme.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.request.enums.ParticipationRequestStatus;
import ru.practicum.explorewithme.request.mapper.ParticipationRequestMapper;
import ru.practicum.explorewithme.request.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventRequestServiceImpl implements EventRequestService {

    private final EventRequestRepository eventRequestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final CollectorGrpcClient collectorGrpcClient;

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение заявок на событие id={} пользователя id={}", eventId, userId);
        EventInternalDto event = getEventAndValidateOwnership(userId, eventId);
        List<ParticipationRequest> requests = eventRequestRepository.findAllByEventId(eventId);
        return requests.stream().map(ParticipationRequestMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequests(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Изменение статуса заявок на событие id={} пользователем id={}", eventId, userId);

        EventInternalDto event = getEventAndValidateOwnership(userId, eventId);
        validateRequestPrerequisites(event);

        ParticipationRequestStatus newStatus = validateNewStatus(request.getStatus());
        List<ParticipationRequest> pendingRequests = getPendingRequestsOrThrow(eventId, request.getRequestIds());

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newStatus == ParticipationRequestStatus.CONFIRMED) {
            processConfirmation(event, request.getRequestIds(), pendingRequests, confirmed, rejected);
        } else {
            rejectAll(pendingRequests, rejected);
        }

        eventRequestRepository.saveAll(pendingRequests);

        return buildResult(confirmed, rejected);
    }

    private EventInternalDto getEventAndValidateOwnership(Long userId, Long eventId) {
        EventInternalDto event = eventClient.getEventInternal(eventId);
        if (!event.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено или недоступно");
        }
        return event;
    }

    private void validateRequestPrerequisites(EventInternalDto event) {
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ConflictException("Подтверждение заявок не требуется для данного события");
        }
    }

    private ParticipationRequestStatus validateNewStatus(ParticipationRequestStatus status) {
        if (status != ParticipationRequestStatus.CONFIRMED && status != ParticipationRequestStatus.REJECTED) {
            throw new ConflictException("Неверный статус заявки");
        }
        return status;
    }

    private List<ParticipationRequest> getPendingRequestsOrThrow(Long eventId, List<Long> requestIds) {
        List<ParticipationRequest> requests = eventRequestRepository.findAllByIdInAndEventIdAndStatus(
                requestIds, eventId, ParticipationRequestStatus.PENDING);
        if (requests.size() != requestIds.size()) {
            throw new ConflictException("Не все заявки находятся в состоянии ожидания");
        }
        return requests;
    }

    private void processConfirmation(EventInternalDto event, List<Long> requestIds,
                                     List<ParticipationRequest> requests,
                                     List<ParticipationRequest> confirmed,
                                     List<ParticipationRequest> rejected) {
        int currentConfirmed = eventRequestRepository.countByEventIdAndStatus(
                event.getId(), ParticipationRequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        if (currentConfirmed >= limit) {
            throw new ConflictException(
                    "Количество участников события не может превышать " + limit);
        }

        Map<Long, ParticipationRequest> requestsById = requests.stream()
                .collect(Collectors.toMap(ParticipationRequest::getId, request -> request));
        int availableSlots = limit - currentConfirmed;

        for (int i = 0; i < requestIds.size(); i++) {
            ParticipationRequest request = requestsById.get(requestIds.get(i));
            if (i < availableSlots) {
                request.setStatus(ParticipationRequestStatus.CONFIRMED);
                confirmed.add(request);
            } else {
                request.setStatus(ParticipationRequestStatus.REJECTED);
                rejected.add(request);
            }
        }
    }

    private void rejectAll(List<ParticipationRequest> requests, List<ParticipationRequest> rejected) {
        for (ParticipationRequest r : requests) {
            r.setStatus(ParticipationRequestStatus.REJECTED);
            rejected.add(r);
        }
    }

    private EventRequestStatusUpdateResult buildResult(List<ParticipationRequest> confirmed,
                                                       List<ParticipationRequest> rejected) {
        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(ParticipationRequestMapper::toDto).collect(Collectors.toList()))
                .rejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public ParticipationRequestDto saveEventParticipation(Long userId, Long eventId) {
        if (eventRequestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос на добавление пользователя" + userId + "на событие " + eventId + " уже существует");
        }

        userClient.getUserShort(userId); // валидирует существование пользователя (404 из fallback, если нет)
        EventInternalDto event = eventClient.getEventInternal(eventId);

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор не может присылать запрос на свое событие");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя добавиться в неопубликованное событие");
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .build();

        int limit = event.getParticipantLimit();
        int confirmed = eventRequestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        log.info("limit={}, confirmed={}", limit, confirmed);

        if (limit > 0 && confirmed >= limit) {
            throw new ConflictException("Количество участников события не может превышать " + limit);
        }

        boolean autoConfirm = limit == 0 || !event.getRequestModeration();
        request.setStatus(autoConfirm ? ParticipationRequestStatus.CONFIRMED : ParticipationRequestStatus.PENDING);

        ParticipationRequest savedRequest = eventRequestRepository.save(request);
        collectorGrpcClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER,
                java.time.Instant.now());
        return ParticipationRequestMapper.toDto(savedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getUserEvents(Long userId) {
        return eventRequestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto removeParticipation(Long userId, Long requestId) {
        ParticipationRequest request = eventRequestRepository.findByIdAndRequesterId(requestId, userId);
        if (request == null) {
            throw new NotFoundException("Заявка с id=" + requestId + " не найдена");
        }
        if (request.getStatus() != ParticipationRequestStatus.PENDING) {
            throw new ConflictException("Отменить можно только заявку в состоянии ожидания");
        }
        request.setStatus(ParticipationRequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(request);
    }

    @Override
    public List<ConfirmedRequestsCountDto> getConfirmedRequestsCounts(EventIdListDto eventIdListDto) {
        return eventRequestRepository.countConfirmedRequestsByEventIds(eventIdListDto.getEventIds());
    }
}