package ru.practicum.explorewithme.user.service;

import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.user.dto.NewUserRequest;
import ru.practicum.explorewithme.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto registerUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);

    UserShortDto getUserShort(Long userId);

    List<UserShortDto> getUsersShort(List<Long> ids);
}