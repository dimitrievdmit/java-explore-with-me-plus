package ru.practicum.explorewithme.location.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.interaction.dto.EventFullDto;
import ru.practicum.explorewithme.interaction.exception.ConflictException;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;
import ru.practicum.explorewithme.interaction.feign.EventClient;
import ru.practicum.explorewithme.location.dal.LocationRepository;
import ru.practicum.explorewithme.location.dto.LocationDto;
import ru.practicum.explorewithme.location.dto.NewLocationRequest;
import ru.practicum.explorewithme.location.dto.UpdateLocationRequest;
import ru.practicum.explorewithme.location.mapper.LocationMapper;
import ru.practicum.explorewithme.location.model.Location;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;
    private final EventClient eventClient;

    @Transactional
    @Override
    public LocationDto createLocation(NewLocationRequest request) {
        if (locationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Локация с именем " + request.getName() + "уже существует");
        }
        Location location = LocationMapper.toEntity(request);
        location = locationRepository.save(location);
        return LocationMapper.toDto(location);
    }

    @Transactional
    @Override
    public LocationDto updateLocation(Long locId, UpdateLocationRequest request) {
        Location location = locationRepository.findById(locId).orElseThrow(() -> new NotFoundException("Локация " + locId + " не найдена"));
        if (request.getName() != null && locationRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), locId)) {
            throw new ConflictException("Название для локации " + request.getName() + " уже существует");
        }
        LocationMapper.updateEntity(request, location);
        return LocationMapper.toDto(location);
    }

    @Transactional
    @Override
    public void deleteLocation(Long locId) {
        Location location = locationRepository.findById(locId).orElseThrow(() -> new NotFoundException("Локация " + locId + " не найдена"));
        locationRepository.delete(location);
    }

    @Override
    public LocationDto getLocationById(Long locId) {
        Location location = locationRepository.findById(locId).orElseThrow(() -> new NotFoundException("Локация " + locId + " не найдена"));
        return LocationMapper.toDto(location);
    }

    @Override
    public List<LocationDto> getAllLocations(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        Page<Location> page = locationRepository.findAll(pageable);
        return page.getContent().stream().map(LocationMapper::toDto).toList();
    }

    @Override
    public List<EventFullDto> getEventsByLocation(Long locId, int from, int size) {
        Location location = locationRepository.findById(locId)
                .orElseThrow(() -> new NotFoundException("Локация " + locId + " не найдена"));
        return eventClient.searchByRadius(location.getLat(), location.getLon(), location.getRadius(), from, size);
    }
}