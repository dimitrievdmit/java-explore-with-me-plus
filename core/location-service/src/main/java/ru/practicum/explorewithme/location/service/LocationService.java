package ru.practicum.explorewithme.location.service;

import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.location.dto.LocationDto;
import ru.practicum.explorewithme.location.dto.NewLocationRequest;
import ru.practicum.explorewithme.location.dto.UpdateLocationRequest;

import java.util.List;

public interface LocationService {
    LocationDto createLocation(NewLocationRequest request);

    LocationDto updateLocation(Long locId, UpdateLocationRequest request);

    void deleteLocation(Long locId);

    LocationDto getLocationById(Long locId);

    List<LocationDto> getAllLocations(int from, int size);

    List<EventFullDto> getEventsByLocation(Long locId, int from, int size);
}