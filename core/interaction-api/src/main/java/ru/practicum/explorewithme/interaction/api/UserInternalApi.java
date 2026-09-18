package ru.practicum.explorewithme.interaction.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.explorewithme.interaction.dto.UserShortDto;

import java.util.List;

public interface UserInternalApi {

    @GetMapping("/internal/users/{userId}")
    UserShortDto getUserShort(@PathVariable("userId") Long userId);

    @GetMapping("/internal/users")
    List<UserShortDto> getUsersShort(@RequestParam("ids") List<Long> ids);
}