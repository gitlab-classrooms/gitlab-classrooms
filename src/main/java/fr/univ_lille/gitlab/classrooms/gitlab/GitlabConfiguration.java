package fr.univ_lille.gitlab.classrooms.gitlab;

import org.gitlab4j.models.Constants;
import org.gitlab4j.api.GitLabApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

@Configuration
class GitlabConfiguration {

    private GitlabProperties gitlabProperties;

    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    public GitlabConfiguration(GitlabProperties gitlabProperties, OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
        this.gitlabProperties = gitlabProperties;
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    }

    @Bean
    @Primary
    GitLabApi oauth2GitlabApi() {
        var client = new GitLabApi(gitlabProperties.url(), Constants.TokenType.OAUTH2_ACCESS, "");
        client.setAuthTokenSupplier(() -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var authorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient("gitlab", authentication.getName());
            return authorizedClient.getAccessToken().getTokenValue();
        });
        return client;
    }

}
