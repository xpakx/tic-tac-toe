package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.AcceptRequest;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerTest {

    @LocalServerPort
    private int port;
    private String baseUrl;
    private Long userId;

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
        this.userId = userRepository.save(user).getId();
    }

    @AfterEach
    void tearDown() {
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthorizedUserShouldNotBeAbleToCreateGame() {
        GameRequest request = getGameRequest();
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(UNAUTHORIZED.value());
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

    @Test
    void userCreatingGameShouldExist() {
        GameRequest request = getGameRequest("new_user");
        given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(NOT_FOUND.value())
                .body("message", containsStringIgnoringCase("user not found"));
    }

    @Test
    void shouldCreateGameAgainstUser() {
        var opponentId = createUser("opponent");
        GameRequest request = getGameRequest("opponent");
        Integer gameId = given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(CREATED.value())
                .extract()
                .path("id");
        var gameOpt = gameRepository.findById(gameId.longValue());
        assert(gameOpt.isPresent());
        var game = gameOpt.get();
        assertThat(game.isAccepted(), is(false));
        assertThat(game.getCurrentState(), equalTo("?????????"));
        assertThat(game.getType(), equalTo(GameType.USER));
        assertThat(game.getCurrentSymbol(), equalTo(GameSymbol.X));
        assertThat(game.getUser().getId(), equalTo(userId));
        assertThat(game.getOpponent().getId(), equalTo(opponentId));
    }

    @Test
    void shouldCreateGameAgainstAI() {
        GameRequest request = getGameRequest();
        Integer gameId = given()
                .contentType(ContentType.JSON)
                .header(getHeaderForUser("test_user"))
                .body(request)
                .when()
                .post(baseUrl + "/game")
                .then()
                .statusCode(CREATED.value())
                .extract()
                .path("id");
        var gameOpt = gameRepository.findById(gameId.longValue());
        assert(gameOpt.isPresent());
        var game = gameOpt.get();
        assertThat(game.isAccepted(), is(true));
        assertThat(game.getCurrentState(), equalTo("?????????"));
        assertThat(game.getType(), equalTo(GameType.AI));
        assertThat(game.getCurrentSymbol(), equalTo(GameSymbol.X));
        assertThat(game.getUser().getId(), equalTo(userId));
        assertThat(game.getOpponent(), nullValue());
    }

    @Test
    void unauthorizedUserShouldNotBeAbleToViewRequests() {
        given()
                .when()
                .get(baseUrl + "/game/request")
                .then()
                .statusCode(UNAUTHORIZED.value());
    }

    @Test
    void userGettingRequestsShouldExist() {
        given()
                .header(getHeaderForUser("new_user"))
                .when()
                .get(baseUrl + "/game/request")
                .then()
                .statusCode(NOT_FOUND.value())
                .body("message", containsStringIgnoringCase("user not found"));
    }

    @Test
    void shouldRespondWithEmptyListOfRequests() {
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game/request")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(0));
    }

    @Test
    void shouldRespondWithUserRequests() {
        var otherId = createUser("new_user");
        var userGameId = createRequest(userId, otherId);
        var game1Id = createRequest(otherId, userId);
        var game2Id = createRequest(otherId, userId);
        var acceptedGameId = createGame(otherId, userId, true);
        var aiGame = createGame();
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game/request")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(2))
                .body("id", hasItem(game1Id.intValue()))
                .body("id", hasItem(game2Id.intValue()))
                .body("id", not(hasItem(userGameId.intValue())))
                .body("id", not(hasItem(acceptedGameId.intValue())))
                .body("id", not(hasItem(aiGame.intValue())));
    }

    @Test
    void unauthorizedUserShouldNotBeAbleToViewGames() {
        given()
                .when()
                .get(baseUrl + "/game")
                .then()
                .statusCode(UNAUTHORIZED.value());
    }

    @Test
    void userGettingGamesShouldExist() {
        given()
                .header(getHeaderForUser("new_user"))
                .when()
                .get(baseUrl + "/game")
                .then()
                .statusCode(NOT_FOUND.value())
                .body("message", containsStringIgnoringCase("user not found"));
    }

    @Test
    void shouldRespondWithEmptyListOfGames() {
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(0));
    }

    @Test
    void shouldRespondWithUserGames() {
        var otherId = createUser("new_user");
        var userGameId = createGame(userId, otherId);
        var gameId = createGame(otherId, userId);
        var aiGameId = createGame();

        var requestId = createRequest(otherId, userId);
        var rejectedId = createRejectedRequest(otherId, userId);
        var finishedId = createFinishedGame(userId, otherId);
        var aiFinishedId = createFinishedGame();
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(3))
                .body("id", hasItem(userGameId.intValue()))
                .body("id", hasItem(gameId.intValue()))
                .body("id", hasItem(aiGameId.intValue()))
                .body("id", not(hasItem(requestId.intValue())))
                .body("id", not(hasItem(finishedId.intValue())))
                .body("id", not(hasItem(aiFinishedId.intValue())))
                .body("id", not(hasItem(rejectedId.intValue())));
    }

    @Test
    void unauthorizedUserShouldNotBeAbleToViewArchive() {
        given()
                .when()
                .get(baseUrl + "/game/archive")
                .then()
                .statusCode(UNAUTHORIZED.value());
    }

    @Test
    void userGettingArchiveShouldExist() {
        given()
                .header(getHeaderForUser("new_user"))
                .when()
                .get(baseUrl + "/game/archive")
                .then()
                .statusCode(NOT_FOUND.value())
                .body("message", containsStringIgnoringCase("user not found"));
    }

    @Test
    void shouldRespondWithEmptyListOfArchive() {
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game/archive")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(0));
    }

    @Test
    void shouldRespondWithUserArchive() {
        var otherId = createUser("new_user");
        var userGameId = createGame(userId, otherId);
        var gameId = createGame(otherId, userId);
        var aiGameId = createGame();

        var requestId = createRequest(otherId, userId);
        var rejectedId = createRejectedRequest(otherId, userId);
        var finishedId = createFinishedGame(userId, otherId);
        var aiFinishedId = createFinishedGame();
        given()
                .header(getHeaderForUser("test_user"))
                .when()
                .get(baseUrl + "/game/archive")
                .then()
                .statusCode(OK.value())
                .body("$", hasSize(2))
                .body("id", not(hasItem(userGameId.intValue())))
                .body("id", not(hasItem(gameId.intValue())))
                .body("id", not(hasItem(aiGameId.intValue())))
                .body("id", not(hasItem(requestId.intValue())))
                .body("id", hasItem(finishedId.intValue()))
                .body("id", hasItem(aiFinishedId.intValue()))
                .body("id", not(hasItem(rejectedId.intValue())));
    }

    @Test
    void unauthorizedUserShouldNotBeAbleToAcceptRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(true))
                .when()
                .post(baseUrl + "/game/1/request")
                .then()
                .statusCode(UNAUTHORIZED.value());
    }

    @Test
    void userShouldNotBeAbleToAcceptRequestSentToOtherUser() {
        var otherId = createUser("new_user");
        var requestId = createRequest(userId, otherId);
        given()
                .header(getHeaderForUser("test_user"))
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(true))
                .when()
                .post(baseUrl + "/game/{gameId}/request", requestId)
                .then()
                .statusCode(FORBIDDEN.value())
                .body("message", containsStringIgnoringCase("cannot change this request"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotChangeAlreadyAcceptedRequest(boolean accepted) {
        var otherId = createUser("new_user");
        var requestId = createGame(otherId, userId, true);
        given()
                .header(getHeaderForUser("test_user"))
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(accepted))
                .when()
                .post(baseUrl + "/game/{gameId}/request", requestId)
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("request already accepted"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldNotChangeAlreadyRejectedRequest(boolean accepted) {
        var otherId = createUser("new_user");
        var requestId = createRejectedRequest(otherId, userId);
        given()
                .header(getHeaderForUser("test_user"))
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(accepted))
                .when()
                .post(baseUrl + "/game/{gameId}/request", requestId)
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("request already rejected"));
    }

    @Test
    void shouldRejectGameRequest() {
        var otherId = createUser("new_user");
        var requestId = createRequest(otherId, userId);
        given()
                .header(getHeaderForUser("test_user"))
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(false))
                .when()
                .post(baseUrl + "/game/{gameId}/request", requestId)
                .then()
                .statusCode(OK.value());
        var gameOpt = gameRepository.findById(requestId);
        assert(gameOpt.isPresent());
        var game = gameOpt.get();
        assertThat(game.isAccepted(), is(false));
        assertThat(game.isRejected(), is(true));
    }

    @Test
    void shouldAcceptGameRequest() {
        var otherId = createUser("new_user");
        var requestId = createRequest(otherId, userId);
        given()
                .header(getHeaderForUser("test_user"))
                .contentType(ContentType.JSON)
                .body(getAcceptRequest(true))
                .when()
                .post(baseUrl + "/game/{gameId}/request", requestId)
                .then()
                .statusCode(OK.value());
        var gameOpt = gameRepository.findById(requestId);
        assert(gameOpt.isPresent());
        var game = gameOpt.get();
        assertThat(game.isAccepted(), is(true));
        assertThat(game.isRejected(), is(false));
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

    private Long createUser(String username) {
        User user = new User();
        user.setPassword("password");
        user.setUsername(username);
        return userRepository.save(user).getId();
    }

    private Long createRequest(Long userId, Long opponentId) {
        return createGame(userId, opponentId, false);
    }

    private Long createGame(Long userId, Long opponentId) {
        return createGame(userId, opponentId, true);
    }

    private Long createRejectedRequest(Long userId, Long opponentId) {
        return createGame(userId, opponentId, false, true, false);
    }

    private Long createGame(Long userId, Long opponentId, boolean accepted) {
        return createGame(userId, opponentId, accepted, false, false);
    }

    private Long createFinishedGame(Long userId, Long opponentId) {
        return createGame(userId, opponentId, true, false, true);
    }

    private Long createGame(Long userId, Long opponentId, boolean accepted, boolean rejected, boolean finished) {
        Game game = new Game();
        game.setUser(userRepository.getReferenceById(userId));
        game.setOpponent(userRepository.getReferenceById(opponentId));
        game.setCurrentState("?????????");
        game.setType(GameType.USER);
        game.setCurrentSymbol(GameSymbol.X);
        game.setUserStarts(true);
        game.setAccepted(accepted);
        game.setRejected(rejected);
        game.setFinished(finished);
        return gameRepository.save(game).getId();
    }

    private Long createGame() {
        return createGame(false);
    }

    private Long createFinishedGame() {
        return createGame(true);
    }

    private Long createGame(boolean finished) {
        Game game = new Game();
        game.setUser(userRepository.getReferenceById(userId));
        game.setCurrentState("?????????");
        game.setType(GameType.AI);
        game.setCurrentSymbol(GameSymbol.X);
        game.setUserStarts(true);
        game.setAccepted(true);
        game.setFinished(finished);
        return gameRepository.save(game).getId();
    }

    private AcceptRequest getAcceptRequest(boolean accepted) {
        var request = new AcceptRequest();
        request.setAccepted(accepted);
        return request;
    }
}