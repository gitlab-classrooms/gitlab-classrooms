package fr.univ_lille.gitlab.classrooms.gitlab;

import com.sun.net.httpserver.HttpServer;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.api.GitLabApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OAuth2TokenRefreshIntegrationTest {

    private static HttpServer mockServer;
    private static final AtomicInteger tokenEndpointCallCount = new AtomicInteger(0);

    @Autowired
    private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Autowired
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private GitlabApiFactory gitlabApiFactory;

    @Autowired
    private JdbcOperations jdbcOperations;

    @BeforeAll
    static void startMockServer() throws Exception {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/oauth/token", exchange -> {
            tokenEndpointCallCount.incrementAndGet();
            String response = """
                {
                    "access_token": "refreshed-access-token-12345",
                    "token_type": "Bearer",
                    "expires_in": 7200,
                    "refresh_token": "refreshed-refresh-token-67890"
                }
                """;
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        mockServer.start();
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.client.provider.gitlab.token-uri",
                () -> "http://localhost:" + mockServer.getAddress().getPort() + "/oauth/token");
    }

    @BeforeEach
    void setUp() {
        tokenEndpointCallCount.set(0);
        jdbcOperations.execute("""
            CREATE TABLE IF NOT EXISTS oauth2_authorized_client
            (
                client_registration_id  varchar(100)                            NOT NULL,
                principal_name          varchar(200)                            NOT NULL,
                access_token_type       varchar(100)                            NOT NULL,
                access_token_value      bytea                                   NOT NULL,
                access_token_issued_at  timestamp                               NOT NULL,
                access_token_expires_at timestamp                               NOT NULL,
                access_token_scopes     varchar(1000) DEFAULT NULL,
                refresh_token_value     bytea         DEFAULT NULL,
                refresh_token_issued_at timestamp     DEFAULT NULL,
                created_at              timestamp     DEFAULT CURRENT_TIMESTAMP NOT NULL,
                PRIMARY KEY (client_registration_id, principal_name)
            );
        """);
        jdbcOperations.execute("DELETE FROM oauth2_authorized_client");
    }

    @Test
    void shouldRefreshExpiredTokenWhenCallingAuthorizedClientManager() {
        String username = "testuser";
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("gitlab");
        assertThat(clientRegistration).isNotNull();

        Instant now = Instant.now();
        Instant issuedAt = now.minus(Duration.ofHours(2));
        Instant expiresAt = now.minus(Duration.ofHours(1));

        OAuth2AccessToken expiredAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "initial-expired-access-token",
                issuedAt,
                expiresAt,
                Set.of("api")
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "initial-refresh-token",
                issuedAt
        );

        OAuth2AuthorizedClient initialAuthorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                username,
                expiredAccessToken,
                refreshToken
        );

        Authentication authentication = new TestingAuthenticationToken(username, null, "ROLE_USER");
        oAuth2AuthorizedClientService.saveAuthorizedClient(initialAuthorizedClient, authentication);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("gitlab")
                .principal(username)
                .build();

        OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientManager.authorize(authorizeRequest);

        assertThat(authorizedClient).isNotNull();
        assertThat(authorizedClient.getAccessToken().getTokenValue()).isEqualTo("refreshed-access-token-12345");
        assertThat(tokenEndpointCallCount.get()).isEqualTo(1);

        OAuth2AuthorizedClient persistedClient = oAuth2AuthorizedClientService.loadAuthorizedClient("gitlab", username);
        assertThat(persistedClient).isNotNull();
        assertThat(persistedClient.getAccessToken().getTokenValue()).isEqualTo("refreshed-access-token-12345");
        assertThat(persistedClient.getRefreshToken()).isNotNull();
        assertThat(persistedClient.getRefreshToken().getTokenValue()).isEqualTo("refreshed-refresh-token-67890");
    }

    @Test
    void shouldNotRefreshTokenWhenTokenIsNotExpired() {
        String username = "activeuser";
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("gitlab");
        assertThat(clientRegistration).isNotNull();

        Instant now = Instant.now();
        Instant issuedAt = now.minus(Duration.ofMinutes(10));
        Instant expiresAt = now.plus(Duration.ofHours(1));

        OAuth2AccessToken validAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "valid-active-access-token",
                issuedAt,
                expiresAt,
                Set.of("api")
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "initial-refresh-token",
                issuedAt
        );

        OAuth2AuthorizedClient initialAuthorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                username,
                validAccessToken,
                refreshToken
        );

        Authentication authentication = new TestingAuthenticationToken(username, null, "ROLE_USER");
        oAuth2AuthorizedClientService.saveAuthorizedClient(initialAuthorizedClient, authentication);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("gitlab")
                .principal(username)
                .build();

        OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientManager.authorize(authorizeRequest);

        assertThat(authorizedClient).isNotNull();
        assertThat(authorizedClient.getAccessToken().getTokenValue()).isEqualTo("valid-active-access-token");
        assertThat(tokenEndpointCallCount.get()).isEqualTo(0);
    }

    @Test
    void shouldRefreshExpiredTokenThroughGitlabApiAuthTokenSupplier() {
        String username = "student1";
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("gitlab");
        assertThat(clientRegistration).isNotNull();

        Instant now = Instant.now();
        Instant issuedAt = now.minus(Duration.ofHours(2));
        Instant expiresAt = now.minus(Duration.ofHours(1));

        OAuth2AccessToken expiredAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "initial-expired-access-token",
                issuedAt,
                expiresAt,
                Set.of("api")
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "initial-refresh-token",
                issuedAt
        );

        OAuth2AuthorizedClient initialAuthorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                username,
                expiredAccessToken,
                refreshToken
        );

        Authentication authentication = new TestingAuthenticationToken(username, null, "ROLE_USER");
        oAuth2AuthorizedClientService.saveAuthorizedClient(initialAuthorizedClient, authentication);

        ClassroomUser user = new ClassroomUser();
        user.setName(username);

        GitLabApi gitLabApi = gitlabApiFactory.userGitlabApi(user);
        String authToken = gitLabApi.getAuthToken();

        assertThat(authToken).isEqualTo("refreshed-access-token-12345");
        assertThat(tokenEndpointCallCount.get()).isEqualTo(1);
    }

    @Test
    void shouldReturnValidTokenThroughGitlabApiAuthTokenSupplierWhenNotExpired() {
        String username = "student2";
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("gitlab");
        assertThat(clientRegistration).isNotNull();

        Instant now = Instant.now();
        Instant issuedAt = now.minus(Duration.ofMinutes(10));
        Instant expiresAt = now.plus(Duration.ofHours(1));

        OAuth2AccessToken validAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "valid-token-student2",
                issuedAt,
                expiresAt,
                Set.of("api")
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-student2",
                issuedAt
        );

        OAuth2AuthorizedClient initialAuthorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                username,
                validAccessToken,
                refreshToken
        );

        Authentication authentication = new TestingAuthenticationToken(username, null, "ROLE_USER");
        oAuth2AuthorizedClientService.saveAuthorizedClient(initialAuthorizedClient, authentication);

        ClassroomUser user = new ClassroomUser();
        user.setName(username);

        GitLabApi gitLabApi = gitlabApiFactory.userGitlabApi(user);
        String authToken = gitLabApi.getAuthToken();

        assertThat(authToken).isEqualTo("valid-token-student2");
        assertThat(tokenEndpointCallCount.get()).isEqualTo(0);
    }

    @Test
    void shouldThrowExceptionWhenNoAuthorizedClientExists() {
        ClassroomUser user = new ClassroomUser();
        user.setName("unknown-user");

        GitLabApi gitLabApi = gitlabApiFactory.userGitlabApi(user);

        assertThatThrownBy(gitLabApi::getAuthToken)
                .isInstanceOf(GitLabAuthenticationException.class)
                .hasMessageContaining("unknown-user");
    }
}
