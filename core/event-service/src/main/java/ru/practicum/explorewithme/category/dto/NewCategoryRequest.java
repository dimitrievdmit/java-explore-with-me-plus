package ru.practicum.explorewithme.category.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCategoryRequest {
    private String name;
}
