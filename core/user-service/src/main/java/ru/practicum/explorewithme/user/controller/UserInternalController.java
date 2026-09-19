package ru.practicum.explorewithme.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.interaction.api.UserInternalApi;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;
import ru.practicum.explorewithme.user.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserInternalController implements UserInternalApi {

    private final UserService userService;

    @Override
    public UserShortDto getUserShort(Long userId) {
        return userService.getUserShort(userId);
    }

    @Override
    public List<UserShortDto> getUsersShort(List<Long> ids) {
        return userService.getUsersShort(ids);
    }
}