package ru.practicum.explorewithme.request.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.explorewithme.request.enums.ParticipationRequestStatus;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {
    @NotEmpty(message = "Список заявок не должен быть пустым")
    private List<@Positive(message = "Идентификатор заявки должен быть положительным") Long> requestIds;

    @NotNull(message = "Статус заявок должен быть указан")
    private ParticipationRequestStatus status;
}
