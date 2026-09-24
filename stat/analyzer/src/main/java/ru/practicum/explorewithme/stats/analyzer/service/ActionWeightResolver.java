package ru.practicum.explorewithme.stats.analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Component
public class ActionWeightResolver {
    private final double viewWeight;
    private final double registerWeight;
    private final double likeWeight;

    public ActionWeightResolver(
            @Value("${stats.action-weights.view}") double viewWeight,
            @Value("${stats.action-weights.register}") double registerWeight,
            @Value("${stats.action-weights.like}") double likeWeight) {
        this.viewWeight = viewWeight;
        this.registerWeight = registerWeight;
        this.likeWeight = likeWeight;
    }

    public double getWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewWeight;
            case REGISTER -> registerWeight;
            case LIKE -> likeWeight;
        };
    }
}
