package io.github.xpakx.tictactoe.settings;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMQPConfig {
    private final String movesTopic;

    public AMQPConfig(@Value("${amqp.exchange.moves}") final String movesTopic) {
        this.movesTopic = movesTopic;
    }

    @Bean
    public TopicExchange movesTopicExchange() {
        return ExchangeBuilder
                .topicExchange(movesTopic)
                .durable(true)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
