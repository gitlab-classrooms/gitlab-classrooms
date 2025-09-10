package fr.univ_lille.gitlab.classrooms.gitlab;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.gitlab4j.models.Constants;
import org.gitlab4j.api.GitLabApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
class GitlabApiFactory {

    private final GitlabProperties gitlabProperties;

    private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabApiFactory.class);

    public GitlabApiFactory(GitlabProperties gitlabProperties, OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
        this.gitlabProperties = gitlabProperties;
        this.oAuth2AuthorizedClientManager = oAuth2AuthorizedClientManager;
    }

    /**
     * Builds a Gitlab Api client using the credentials of the given classroom user.
     *
     * @param user
     * @return
     */
    public GitLabApi userGitlabApi(ClassroomUser user) {
        var oauth2AuthorizedRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId("gitlab")
                .principal(user.getName())
                .build();
        var client = new GitLabApi(gitlabProperties.url(), Constants.TokenType.OAUTH2_ACCESS, "");
        client.setAuthTokenSupplier(() -> {
            try {
                var oauth2Client = oAuth2AuthorizedClientManager.authorize(oauth2AuthorizedRequest);
                LOGGER.debug("Access token for user '{}' is {}", user.getName(), oauth2Client.getAccessToken().getTokenValue());
                return oauth2Client.getAccessToken().getTokenValue();
            } catch (ClientAuthorizationRequiredException e) {
                LOGGER.error("Unable to get an access token for user '{}'", user.getName());
                throw new GitLabAuthenticationException("Unable to get an access token for user '%s'".formatted(user.getName()));
            }
        });
        return client;
    }
}
