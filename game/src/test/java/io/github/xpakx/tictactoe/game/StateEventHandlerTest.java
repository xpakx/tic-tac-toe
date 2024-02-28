package io.github.xpakx.tictactoe.game;

import com.redis.testcontainers.RedisContainer;
import io.github.xpakx.tictactoe.game.dto.StateEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class StateEventHandlerTest {

    @Container
    static RabbitMQContainer rabbitMq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.7.25-management-alpine")
    );

    @Container
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:6.2.6-alpine")
    );

    @Autowired
    RabbitAdmin rabbitAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    GameRepository gameRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.username", rabbitMq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMq::getAdminPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
        rabbitAdmin.purgeQueue("tictactoe.state.queue");
        gameRepository.deleteAll();
    }

    @Test
    void shouldSaveGameInRedisOnEvent() {
        var event = new StateEvent();
        event.setId(5L);
        event.setUsername1("user1");
        event.setUsername2("user2");
        event.setCurrentState("????X????");
        rabbitTemplate.convertAndSend("tictactoe.state.topic", "state", event);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(redisHasRecord());
        var gameOpt = gameRepository.findById(5L);
        assertThat(gameOpt.isPresent(), is(true));
        var game = gameOpt.get();
        assertThat(game.getCurrentState(), equalTo("????X????"));
        assertThat(game.getUsername1(), equalTo("user1"));
        assertThat(game.getUsername2(), equalTo("user2"));
    }

    @Test
    void shouldNotSaveGameWithError() throws Exception {
        var event = new StateEvent();
        event.setId(5L);
        event.setUsername1("user1");
        event.setUsername2("user2");
        event.setCurrentState("????X????");
        event.setError(true);
        event.setErrorMessage("Error");
        rabbitTemplate.convertAndSend("tictactoe.state.topic", "state", event);
        Thread.sleep(1000);
        assertThat(getRedisRecordCount(), equalTo(0L));
    }

    private Long getRedisRecordCount() {
        return gameRepository.count();
    }

    private Callable<Boolean> redisHasRecord() {
        return () -> getRedisRecordCount() > 0;
    }
}