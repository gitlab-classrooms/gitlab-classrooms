package fr.univ_lille.gitlab.classrooms.gitlab;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.models.Constants;
import org.gitlab4j.api.GitLabApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;

@Component
class GitlabApiFactory {

    private final GitlabProperties gitlabProperties;

    private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabApiFactory.class);

    public GitlabApiFactory(GitlabProperties gitlabProperties, OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
        this.gitlabProperties = gitlabProperties;
        this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    }

    /**
     * Builds a Gitlab Api client using the credentials of the given classroom user.
     *
     * @param user
     * @return
     */
    public GitLabApi userGitlabApi(ClassroomUser user) {

        var client = new GitLabApi(gitlabProperties.url(), Constants.TokenType.OAUTH2_ACCESS, "");
        client.setAuthTokenSupplier(() -> {
            try {
                // load authorized client from database
                var authorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient("gitlab", user.getName());
                var oauth2AuthorizedRequest = OAuth2AuthorizeRequest
                        .withAuthorizedClient(authorizedClient)
                        .principal(user.getName())
                        .build();
                // re-authenticate client (this may renew the token using refresh-token)
                var oauth2Client = oAuth2AuthorizedClientManager.authorize(oauth2AuthorizedRequest);

                LOGGER.debug("Access token for user '{}' is {}", user.getName(), oauth2Client.getAccessToken().getTokenValue());
                return oauth2Client.getAccessToken().getTokenValue();
            } catch (IllegalArgumentException | ClientAuthorizationRequiredException e) {
                LOGGER.error("Unable to get an access token for user '{}'", user.getName());
                throw new GitLabAuthenticationException("Unable to get an access token for user '%s'".formatted(user.getName()));
            }
        });
        return client;
    }
}
