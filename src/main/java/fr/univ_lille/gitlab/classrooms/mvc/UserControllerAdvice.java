package fr.univ_lille.gitlab.classrooms.mvc;

import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.NoSuchElementException;

@ControllerAdvice({"fr.univ_lille.gitlab.classrooms", "org.springframework.boot.autoconfigure.web.servlet.error"})
class UserControllerAdvice {

    private final ClassroomUserService classroomUserService;

    UserControllerAdvice(ClassroomUserService classroomUserService) {
        this.classroomUserService = classroomUserService;
    }

    /**
     * This model attribute provides a classroom user to the model, for the controller methods that need to retrieve the currently connected user.
     * @param authenticationPrincipal The current authentication principal
     * @return the ClassroomUser for the authenticationPrincipal
     */
    @ModelAttribute("user")
    ClassroomUser classroomUser(Principal authenticationPrincipal){
        try{
            // if user is a JWTAuthenticationToken, (gitlab id token), load the user using the "user_login" claim
            if(authenticationPrincipal instanceof JwtAuthenticationToken jwtToken && (jwtToken.getTokenAttributes().containsKey("user_login"))){
                return this.classroomUserService.getClassroomUser(jwtToken.getTokenAttributes().get("user_login").toString());
            }
            return this.classroomUserService.getClassroomUser(authenticationPrincipal.getName());
        }
        catch(NoSuchElementException e){
            return null;
        }
    }

}
