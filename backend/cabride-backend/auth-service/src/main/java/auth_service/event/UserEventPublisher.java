package auth_service.event;

import auth_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishedUserCreated(UserCreatedEvent event){
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EXCHANGE,
                RabbitMQConfig.USER_CREATED_ROUTING_KEY,
                event

        );
    }

}
