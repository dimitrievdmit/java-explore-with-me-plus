package ru.practicum.explorewithme.request.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto;
import ru.practicum.explorewithme.request.enums.ParticipationRequestStatus;
import ru.practicum.explorewithme.request.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface EventRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    Integer countByEventIdAndStatus(Long eventId, ParticipationRequestStatus status);

    List<ParticipationRequest> findAllByIdInAndEventIdAndStatus(List<Long> ids, Long eventId, ParticipationRequestStatus status);

    @Query("SELECT new ru.practicum.explorewithme.interaction.dto.ConfirmedRequestsCountDto(r.eventId, COUNT(r.id)) " +
            "FROM ParticipationRequest r " +
            "WHERE r.eventId IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.eventId")
    List<ConfirmedRequestsCountDto> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);

    Boolean existsByRequesterIdAndEventId(Long userId, Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    ParticipationRequest findByIdAndRequesterId(Long requestId, Long userId);
}