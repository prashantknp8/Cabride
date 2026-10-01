package auth_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String USER_EXCHANGE= "cabride.user.exchange";
    public static final String USER_CREATED_QUEUE="cabride.user.created.queue";

    public static final String USER_CREATED_ROUTING_KEY="user.created";

    @Bean
    public TopicExchange userExchange(){
        return new TopicExchange(USER_EXCHANGE);
    }

    @Bean
    public Queue userCreatedQueue(){
        return new Queue(USER_CREATED_QUEUE,true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter){
        RabbitTemplate rabbitTemplate=new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}
