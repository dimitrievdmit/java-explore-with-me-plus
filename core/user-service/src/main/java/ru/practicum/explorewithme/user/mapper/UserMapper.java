package ru.practicum.explorewithme.user.mapper;

import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.user.dto.NewUserRequest;
import ru.practicum.explorewithme.user.dto.UserDto;
import ru.practicum.explorewithme.user.model.User;

public final class UserMapper {
    public static User toEntity(NewUserRequest request) {
        return new User(null, request.getEmail(), request.getName());
    }

    public static UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getEmail(), user.getName());
    }

    public static UserShortDto toShortDto(User user) {
        return UserShortDto.builder().id(user.getId()).name(user.getName()).build();
    }
}