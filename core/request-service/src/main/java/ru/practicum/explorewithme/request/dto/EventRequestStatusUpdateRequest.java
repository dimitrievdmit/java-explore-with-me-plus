package ru.practicum.explorewithme.request.dto;

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
    List<Long> requestIds;
    ParticipationRequestStatus status;
}
