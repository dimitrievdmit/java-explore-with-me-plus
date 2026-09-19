package ru.practicum.explorewithme.location.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.EventClient;
import ru.practicum.explorewithme.location.dal.LocationRepository;
import ru.practicum.explorewithme.location.model.Location;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private EventClient eventClient;

    @InjectMocks
    private LocationServiceImpl locationService;

    private Location location;
    Long locId;

    @BeforeEach
    void setUp() {
        locId = 1L;
        location = Location.builder().id(1L).name("Moscow").lat(55.7558f).lon(37.6173f).radius(300.00f).build();
    }

    @Test
    void getEventsByLocation_shouldCallEventClientWithLocationCoordinates() {
        when(locationRepository.findById(locId)).thenReturn(Optional.of(location));
        when(eventClient.searchByRadius(55.7558f, 37.6173f, 300.00f, 0, 10))
                .thenReturn(List.of(EventFullDto.builder().id(5L).build()));

        List<EventFullDto> result = locationService.getEventsByLocation(locId, 0, 10);

        assertThat(result.size()).isEqualTo(1);
        verify(eventClient).searchByRadius(55.7558f, 37.6173f, 300.00f, 0, 10);
    }

    @Test
    void getEventsByLocation_locationNotFound_ShouldThrowNotFound() {
        when(locationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> locationService.getEventsByLocation(99L, 0, 10))
                .isInstanceOf(NotFoundException.class);

        verify(eventClient, never()).searchByRadius(anyFloat(), anyFloat(), anyFloat(), anyInt(), anyInt());
    }
}