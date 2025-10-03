package fr.univ_lille.gitlab.classrooms.mvc.assignments;

import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignmentService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;
import java.util.UUID;

/**
 * View Controller that manages reset grade operation a Student or Teacher can do on a StudentAssignment.
 */
@Controller
@RequestMapping("/assignments/{assignmentId}/students/{studentId}")
class StudentAssignmentResetGradeMVCController {

    private final StudentAssignmentService studentAssignmentService;

    private final ClassroomUserService classroomUserService;

    StudentAssignmentResetGradeMVCController(StudentAssignmentService studentAssignmentService, ClassroomUserService classroomUserService) {
        this.studentAssignmentService = studentAssignmentService;
        this.classroomUserService = classroomUserService;
    }

    @RolesAllowed({"STUDENT", "TEACHER"})
    @PostMapping("/reset")
    ModelAndView resetGrades(@PathVariable UUID assignmentId, @PathVariable String studentId, @ModelAttribute("user") ClassroomUser connectedUser, HttpServletRequest request) {
        var student = this.classroomUserService.getClassroomUser(studentId);

        if( ! connectedUser.isTeacher() && (! student.equals(connectedUser))){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student can only reset its own grades");
        }

        this.studentAssignmentService.resetGrades(student, assignmentId);

        // redirect to the referer (ugly)
        var view = Optional.ofNullable(request.getHeader("Referer")).orElse("/");
        return new ModelAndView("redirect:"+view);
    }
}
