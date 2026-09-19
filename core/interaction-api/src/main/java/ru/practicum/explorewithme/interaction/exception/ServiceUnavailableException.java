package ru.practicum.explorewithme.interaction.exception;

import lombok.Getter;

@Getter
public class ServiceUnavailableException extends RuntimeException {
    private final String serviceName;

    public ServiceUnavailableException(String serviceName) {
        super("Сервис " + serviceName + " временно недоступен");
        this.serviceName = serviceName;
    }

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super("Сервис " + serviceName + " временно недоступен", cause);
        this.serviceName = serviceName;
    }
}