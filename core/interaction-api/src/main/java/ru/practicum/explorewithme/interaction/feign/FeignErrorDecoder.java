package ru.practicum.explorewithme.interaction.feign;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.interaction.exception.NotFoundException;

@Component
public class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new NotFoundException("Объект не найден при вызове " + methodKey);
        }
        return defaultDecoder.decode(methodKey, response);
    }
}