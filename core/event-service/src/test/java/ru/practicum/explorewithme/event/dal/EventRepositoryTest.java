package ru.practicum.explorewithme.event.dal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.explorewithme.category.model.Category;
import ru.practicum.explorewithme.event.model.Event;
import ru.practicum.explorewithme.event.model.GeoPoint;
import ru.practicum.explorewithme.interaction.dto.EventState;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private EventRepository eventRepository;

    private static final Long INITIATOR_ID = 1L;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category(null, "Концерты");
        em.persist(category);
    }

    @Test
    void findAllByInitiatorId_PaginationWorks() {
        createEvent("Event 1", 55f, 37f);
        createEvent("Event 2", 55f, 37f);
        Pageable pageable = PageRequest.of(0, 1);
        var page = eventRepository.findAllByInitiatorId(INITIATOR_ID, pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByIdAndInitiatorId_ReturnsCorrectEvent() {
        Event event = createEvent("My Event", 55f, 37f);
        var found = eventRepository.findByIdAndInitiatorId(event.getId(), INITIATOR_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("My Event");
    }

    @Test
    void findByIdAndInitiatorId_WrongUser_ReturnsEmpty() {
        Event event = createEvent("Event", 55f, 37f);
        var found = eventRepository.findByIdAndInitiatorId(event.getId(), 999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findEventsByRadius_ReturnsOnlyPublishedEventsInsideRadius() {
        Event inside = createEvent("Inside", 55.80f, 37.70f);
        inside.setState(EventState.PUBLISHED);
        em.persist(inside);

        Event outside = createEvent("Outside", 59.95f, 30.32f);
        outside.setState(EventState.PUBLISHED);
        em.persist(outside);

        em.flush();

        var page = eventRepository.findEventsByRadius(55.75f, 37.62f, 50f, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Event::getTitle).containsExactly("Inside");
    }

    private Event createEvent(String title, Float lat, Float lon) {
        Event e = new Event();
        e.setTitle(title);
        e.setAnnotation("annotation for test");
        e.setDescription("description for test");
        e.setEventDate(LocalDateTime.now().plusDays(1));
        e.setLocation(new GeoPoint(lat, lon));
        e.setPaid(false);
        e.setParticipantLimit(0);
        e.setRequestModeration(true);
        e.setState(EventState.PENDING);
        e.setCreatedOn(LocalDateTime.now());
        e.setInitiatorId(INITIATOR_ID);
        e.setCategory(category);
        return em.persist(e);
    }
}