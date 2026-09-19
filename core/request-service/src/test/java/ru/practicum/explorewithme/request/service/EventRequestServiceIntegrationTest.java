package ru.practicum.explorewithme.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.interaction.dto.EventInternalDto;
import ru.practicum.explorewithme.interaction.dto.EventState;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.interaction.feign.EventClient;
import ru.practicum.explorewithme.interaction.feign.UserClient;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class EventRequestServiceIntegrationTest {

    @Autowired
    private EventRequestService requestService;

    @MockBean
    private EventClient eventClient;
    @MockBean
    private UserClient userClient;

    private static final Long INITIATOR_ID = 1L;
    private static final Long EVENT_ID = 100L;

    @BeforeEach
    void setUp() {
        when(eventClient.getEventInternal(EVENT_ID)).thenReturn(EventInternalDto.builder()
                .id(EVENT_ID).initiatorId(INITIATOR_ID).state(EventState.PUBLISHED)
                .participantLimit(1).requestModeration(true).build());
    }

    @Test
    void shouldGetEmptyRequestsForNewEvent() {
        List<ParticipationRequestDto> requests = requestService.getEventRequests(INITIATOR_ID, EVENT_ID);
        assertThat(requests).isEmpty();
    }

    @Test
    void shouldSaveParticipationRequestForRealDb() {
        Long requesterId = 2L;
        when(userClient.getUserShort(requesterId)).thenReturn(new UserShortDto(requesterId, "Requester"));

        ParticipationRequestDto result = requestService.saveEventParticipation(requesterId, EVENT_ID);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEvent()).isEqualTo(EVENT_ID);
    }
}