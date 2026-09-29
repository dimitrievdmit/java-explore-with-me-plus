package ru.practicum.explorewithme.category.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCategoryRequest {

    private Long id;
    @Size(max = 50, message = "Название категории должно быть менее 50")
    private String name;
}
