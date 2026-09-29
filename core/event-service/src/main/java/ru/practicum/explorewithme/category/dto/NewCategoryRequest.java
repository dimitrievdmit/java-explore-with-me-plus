package ru.practicum.explorewithme.category.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCategoryRequest {
    @Size(max = 50, message = "Название категории должно быть менее 50")
    private String name;
}
