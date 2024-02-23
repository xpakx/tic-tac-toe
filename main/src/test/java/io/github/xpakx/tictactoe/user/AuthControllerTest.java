package io.github.xpakx.tictactoe.user;

import io.github.xpakx.tictactoe.user.dto.RegistrationRequest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {
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
    UserRoleRepository roleRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost".concat(":").concat(String.valueOf(port));
        User user = new User();
        user.setPassword(passwordEncoder.encode("password"));
        user.setUsername("test_user");
        userId = userRepository.save(user).getId();
    }

    @AfterEach
    void tearDown() {
        roleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void usernameShouldBeUnique() {
        RegistrationRequest request = getRegRequest("test_user", "password", "password");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase("username exists"));
    }

    @Test
    void passwordsShouldMatch() {
        RegistrationRequest request = getRegRequest("new_user", "password", "password2");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase(("validation failed")))
                .body("errors", hasItem(containsStringIgnoringCase("passwords don't match")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void usernameCannotBeEmpty(String username) {
        RegistrationRequest request = getRegRequest(username, "password", "password");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase(("validation failed")))
                .body("errors", hasItem(containsStringIgnoringCase("username cannot be empty")));

    }
    @ParameterizedTest
    @ValueSource(strings = {"a", "test", "too_long_username"})
    void usernameMustBeOfCorrectLength(String username) {
        RegistrationRequest request = getRegRequest(username, "password", "password");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase(("validation failed")))
                .body("errors", hasItem(containsStringIgnoringCase("between 5 and 15")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void passwordCannotBeEmpty(String password) {
        RegistrationRequest request = getRegRequest("new_user", password, password);
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(BAD_REQUEST.value())
                .body("message", containsStringIgnoringCase(("validation failed")))
                .body("errors", hasItem(containsStringIgnoringCase("password cannot be empty")));
    }

    @Test
    void shouldRegisterNewUser() {
        RegistrationRequest request = getRegRequest("new_user", "password", "password");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(CREATED.value());
        assertThat(userRepository.count(), equalTo(2L));
        var users = userRepository.findAll();
        assertThat(users, hasItem(hasProperty("username", equalTo("new_user"))));
    }

    @Test
    void shouldReturnTokenAfterRegistration() {
        RegistrationRequest request = getRegRequest("new_user", "password", "password");
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(baseUrl + "/register")
                .then()
                .statusCode(CREATED.value())
                .body("username", equalTo("new_user"))
                .body("token", notNullValue());
    }

    private RegistrationRequest getRegRequest(String username, String password, String password2) {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setPasswordRe(password2);
        return request;
    }
}