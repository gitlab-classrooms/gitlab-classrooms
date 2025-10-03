package fr.univ_lille.gitlab.classrooms.mvc;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true)
@Configuration
public class WebSecurityConfiguration implements WebMvcConfigurer {

    private static final String LOGIN_PAGE = "login";

    private ClassroomUserService classroomUserService;

    public WebSecurityConfiguration(ClassroomUserService classroomUserService) {
        this.classroomUserService = classroomUserService;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // serve the login page as-is
        registry.addViewController(LOGIN_PAGE).setViewName(LOGIN_PAGE);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(it -> {
                    it.requestMatchers(LOGIN_PAGE).permitAll();
                    it.requestMatchers("images/**", "icons/**", "fonts/**", "*.css").permitAll();

                    // authorize actuator endpoints
                    it.requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll();

                    it.anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> {
                    oauth2.loginPage("/" + LOGIN_PAGE);
                    oauth2.userInfoEndpoint(userInfo -> userInfo.userService(this.userService()));
                })
                .build();
    }

    /**
     * OAuth2AuthorizedClientService is used to store oauth2 authorized clients (end-users).
     * This implementation uses the database instead of in-memory which is the default.
     *
     * @param jdbcOperations
     * @param clientRegistrationRepository
     * @return a JdbcOAuth2AuthorizedClientService
     * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/client/core.html#oauth2Client-authorized-repo-service">Spring Documentation</a>
     * @see OAuth2AuthorizedClientService
     */
    @Bean
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService(JdbcOperations jdbcOperations, ClientRegistrationRepository clientRegistrationRepository) {
        return new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
    }

    /**
     * OAuth2AuthorizedClientManager is used to access an oauth2 authorized client (end-user), out of the scope of a HttpServletRequest. It will also refresh the token if needed.
     * @param authorizedClientService
     * @param clientRegistrationRepository
     * @return
     */
    @Bean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(OAuth2AuthorizedClientService authorizedClientService, ClientRegistrationRepository clientRegistrationRepository) {
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .authorizationCode()
                        .refreshToken()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }


    private OAuth2UserService<OAuth2UserRequest, OAuth2User> userService() {
        var delegate = new DefaultOAuth2UserService();

        return userRequest -> {
            var oauth2User = delegate.loadUser(userRequest);

            // load user from the database, or create it
            var classroomUser = this.classroomUserService.loadOrCreateClassroomUser(oauth2User);

            // map roles
            var authorities = classroomUser.getRoles().stream()
                    .map(it -> new SimpleGrantedAuthority("ROLE_" + it.name()))
                    .toList();

            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                    .getUserNameAttributeName();
            return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), userNameAttributeName);
        };
    }
}
