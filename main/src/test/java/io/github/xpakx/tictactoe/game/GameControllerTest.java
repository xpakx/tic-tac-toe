package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.GameRequest;
import io.github.xpakx.tictactoe.security.JwtUtils;
import io.github.xpakx.tictactoe.user.User;
import io.github.xpakx.tictactoe.user.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerTest {

    @LocalServerPort
    private int port;
    private String baseUrl;
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15.1")
    ).withDatabaseName("ttt_test");
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    JwtUtils jwt;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost".concat(":").concat(String.valueOf(port));
        User user = new User();
        user.setPassword("password");
        user.setUsername("test_user");
    }

    @AfterEach
    void tearDown() {
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void aiGameRequestShouldNotHaveUsername() {
        GameRequest request = getGameRequest(GameType.AI, "username");
        given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("validation failed"))
                .body("errors", hasItem(containsStringIgnoringCase("should not have opponent username")));
    }

    @Test
    void userGameRequestShouldHaveUsername() {
        GameRequest request = getGameRequest(GameType.USER, null);
        given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("validation failed"))
                .body("errors", hasItem(containsStringIgnoringCase("must have opponent username")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"test_user", "username"})
    void gameTypeShouldNotBeNull(String username) {
        GameRequest request = getGameRequest(null, username);
        given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("validation failed"))
                .body("errors", hasItem(containsStringIgnoringCase("game type cannot be null")));
    }

    @ParameterizedTest
    @EnumSource(GameType.class)
    void userCreatingGameShouldExist(GameType type) {
        GameRequest request = getGameRequest(type, type == GameType.USER  ? "test_user" : null);
        given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("new_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(NOT_FOUND.value())
                .body("message", containsStringIgnoringCase("user not found"));
    }

    private GameRequest getGameRequest(GameType type, String username) {
        var request = new GameRequest();
        request.setOpponent(username);
        request.setType(type);
        return request;
    }

    private GameRequest getGameRequest() {
        return getGameRequest(GameType.AI, null);
    }

    private GameRequest getGameRequest(String username) {
        return getGameRequest(GameType.USER, username);
    }

    private Header getHeaderForUser(String username) {
        var token = jwt.generateToken(new org.springframework.security.core.userdetails.User(username, "", List.of()));
        var header = "Bearer " + token;
        return new Header("Authorization", header);
    }
}