package ru.practicum.explorewithme.interaction.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}