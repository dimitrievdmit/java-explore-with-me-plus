package ru.practicum.explorewithme.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Builder
public class NewCompilationDto {
    @NotBlank
    @Size(min = 1, max = 50, message = "Название компиляции должно быть более 1 и менее 50 символов")
    private String title;

    private Boolean pinned;

    private List<Long> events;

    public NewCompilationDto(String title, Boolean pinned, List<Long> events) {
        this.title = title;
        this.pinned = pinned;
        this.events = events;
    }
}