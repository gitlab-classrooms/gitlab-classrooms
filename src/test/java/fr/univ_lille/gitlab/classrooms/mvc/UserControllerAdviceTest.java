package fr.univ_lille.gitlab.classrooms.mvc;

import fr.univ_lille.gitlab.classrooms.users.ClassroomRole;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerAdviceTest {

    @InjectMocks
    private UserControllerAdvice userControllerAdvice;

    @Mock
    private ClassroomUserService classroomUserService;

    @Test
    void classroomUser_for_OauthAuthenticationToken() {
        var vader = new ClassroomUser("darth.vader", List.of(ClassroomRole.STUDENT));
        when(classroomUserService.getClassroomUser("darth.vader")).thenReturn(vader);

        var oauth2User = new DefaultOAuth2User(null, Map.of("nameAttr", "darth.vader"), "nameAttr");
        var token = new OAuth2AuthenticationToken(oauth2User, null, "oauth");

        var user = this.userControllerAdvice.classroomUser(token);

        assertThat(user)
                .isNotNull()
                .isEqualTo(vader);

        verify(classroomUserService).getClassroomUser("darth.vader");
    }

    @Test
    void classroomUser_for_JWTAuthenticationToken() {
        var vader = new ClassroomUser("darth.vader", List.of(ClassroomRole.STUDENT));
        when(classroomUserService.getClassroomUser("darth.vader")).thenReturn(vader);

        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim("user_login", "darth.vader")
                .build();
        var token = new JwtAuthenticationToken(jwt);

        var user = this.userControllerAdvice.classroomUser(token);

        assertThat(user)
                .isNotNull()
                .isEqualTo(vader);

        verify(classroomUserService).getClassroomUser("darth.vader");
    }

    @Test
    void classroomUser_returnsNull_whenUserIsNotFound() {
        when(classroomUserService.getClassroomUser(anyString())).thenThrow(new NoSuchElementException());

        var oauth2User = new DefaultOAuth2User(null, Map.of("nameAttr", "darth.vader"), "nameAttr");
        var token = new OAuth2AuthenticationToken(oauth2User, null, "oauth");

        var user = this.userControllerAdvice.classroomUser(token);

        assertThat(user).isNull();

        verify(classroomUserService).getClassroomUser("darth.vader");
    }
}
