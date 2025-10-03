package fr.univ_lille.gitlab.classrooms.mvc;

import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpSession;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
@RequestMapping("/classrooms")
@RolesAllowed("TEACHER")
class ClassroomController {

    private static final String CLASSROOM_MODEL_ATTRIBUTE = "classroom";

    private final ClassroomService classroomService;

    private final Gitlab gitlab;

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassroomController.class.getName());

    ClassroomController(ClassroomService classroomService, Gitlab gitlab) {
        this.classroomService = classroomService;
        this.gitlab = gitlab;
    }

    @GetMapping("/new")
    String newClassroom(Model model) throws GitLabApiException {
        model.addAttribute("groups", this.gitlab.getGroupsOfConnectedUser());
        return "classrooms/new";
    }

    @PostMapping("/new")
    String newClassroom(@RequestParam String classroomName, @RequestParam(required = false) Long parentGitLabGroupId, @ModelAttribute("user") ClassroomUser teacher) throws GitLabApiException {
        this.classroomService.createClassroom(classroomName, parentGitLabGroupId, teacher);
        return "redirect:/";
    }

    @GetMapping("/{classroomId}")
    String showClassroom(@PathVariable UUID classroomId, Model model) {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom with id %s does not exists".formatted(classroomId)));
        model.addAttribute(CLASSROOM_MODEL_ATTRIBUTE, classroom);

        try {
            model.addAttribute("gitlabGroupUrl", gitlab.getGroupURI(classroom));
        }
        catch (GitLabApiException e){
            LOGGER.error(e.getMessage());
        }

        return "classrooms/view";
    }

    @GetMapping("/{classroomId}/join")
    @RolesAllowed({"TEACHER", "STUDENT"})
    String showJoinClassroom(@PathVariable UUID classroomId, Model model) {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute(CLASSROOM_MODEL_ATTRIBUTE, classroom);

        return "classrooms/join";
    }

    @PostMapping("/{classroomId}/join")
    @RolesAllowed({"TEACHER", "STUDENT"})
    String joinClassroom(@PathVariable UUID classroomId, @ModelAttribute("user") ClassroomUser student, Model model, HttpSession session) {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute(CLASSROOM_MODEL_ATTRIBUTE, classroom);

        this.classroomService.joinClassroom(classroom, student);

        var redirect = session.getAttribute("redirect");
        if (redirect != null) {
            session.removeAttribute("redirect");
            return "redirect:" + redirect;
        }

        return "classrooms/joined";
    }

    @GetMapping("/{classroomId}/archive")
    String archiveClassroom(@PathVariable UUID classroomId) {
        var classroom = this.classroomService.getClassroom(classroomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        this.classroomService.archiveClassroom(classroom);
        return "redirect:/";
    }
}
