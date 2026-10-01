package com.cabride.user.event;

import com.cabride.user.config.RabbitMQConfig;
import com.cabride.user.entity.UserProfile;
import com.cabride.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserProfileRepository userProfileRepository;

    @RabbitListener(
            queues = RabbitMQConfig.USER_CREATED_QUEUE
    )
    public void handleUserCreated(UserCreatedEvent event) {

        if (userProfileRepository
                .findByUserId(event.getUserId())
                .isPresent()) {

            return;
        }

        UserProfile profile = new UserProfile();

        profile.setUserId(event.getUserId());
        profile.setFirstName(event.getFirstName());
        profile.setLastName(event.getLastName());
        profile.setPhoneNumber(event.getPhoneNumber());

        userProfileRepository.save(profile);
    }
}