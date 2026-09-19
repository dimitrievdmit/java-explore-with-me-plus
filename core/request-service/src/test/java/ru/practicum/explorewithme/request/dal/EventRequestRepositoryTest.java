package ru.practicum.explorewithme.request.dal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.explorewithme.request.enums.ParticipationRequestStatus;
import ru.practicum.explorewithme.request.model.ParticipationRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRequestRepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private EventRequestRepository requestRepository;

    private static final Long EVENT_ID = 100L;

    @Test
    void countByEventIdAndStatus_ReturnsCorrectCount() {
        createRequest(1L, ParticipationRequestStatus.CONFIRMED);
        createRequest(2L, ParticipationRequestStatus.CONFIRMED);
        createRequest(3L, ParticipationRequestStatus.PENDING);

        int count = requestRepository.countByEventIdAndStatus(EVENT_ID, ParticipationRequestStatus.CONFIRMED);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void findAllByEventId_ReturnsRequests() {
        createRequest(1L, ParticipationRequestStatus.PENDING);
        List<ParticipationRequest> result = requestRepository.findAllByEventId(EVENT_ID);
        assertThat(result).hasSize(1);
    }

    @Test
    void countConfirmedRequestsByEventIds_GroupsCorrectly() {
        createRequestForEvent(EVENT_ID, 1L, ParticipationRequestStatus.CONFIRMED);
        createRequestForEvent(EVENT_ID, 2L, ParticipationRequestStatus.CONFIRMED);
        createRequestForEvent(200L, 3L, ParticipationRequestStatus.CONFIRMED);

        var counts = requestRepository.countConfirmedRequestsByEventIds(List.of(EVENT_ID, 200L));

        assertThat(counts).hasSize(2);
        assertThat(counts).anySatisfy(c -> {
            if (c.getEventId().equals(EVENT_ID)) assertThat(c.getCount()).isEqualTo(2L);
        });
    }

    private void createRequest(Long requesterId, ParticipationRequestStatus status) {
        createRequestForEvent(EVENT_ID, requesterId, status);
    }

    private void createRequestForEvent(Long eventId, Long requesterId, ParticipationRequestStatus status) {
        ParticipationRequest req = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(requesterId)
                .status(status)
                .build();
        em.persist(req);
    }
}