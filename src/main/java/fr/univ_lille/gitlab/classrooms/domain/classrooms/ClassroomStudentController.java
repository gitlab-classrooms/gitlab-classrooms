package fr.univ_lille.gitlab.classrooms.domain.classrooms;

import fr.univ_lille.gitlab.classrooms.assignments.AssignmentService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUserService;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RolesAllowed("TEACHER")
class ClassroomStudentController {

    private final ClassroomService classroomService;

    private final AssignmentService assignmentService;

    private final ClassroomUserService classroomUserService;

    ClassroomStudentController(ClassroomService classroomService, AssignmentService assignmentService, ClassroomUserService classroomUserService) {
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
        this.classroomUserService = classroomUserService;
    }

    @GetMapping("/classrooms/{classroomId}/students/{studentName}")
    ModelAndView showClassroomStudent(@PathVariable UUID classroomId, @PathVariable String studentName) {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classroom with id %s does not exists".formatted(classroomId)));

        var student = this.classroomUserService.getClassroomUser(studentName);

        var studentAssignments = this.assignmentService.getAllStudentAssignmentsForAClassroom(classroom, student);

        var model = new ModelAndView("classrooms/student");

        model.addObject("classroom", classroom);
        model.addObject("student", student);
        model.addObject("assignments", studentAssignments);

        return model;
    }
}
