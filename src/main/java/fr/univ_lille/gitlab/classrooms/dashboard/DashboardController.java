package fr.univ_lille.gitlab.classrooms.dashboard;

import fr.univ_lille.gitlab.classrooms.assignments.AssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignment;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomEntity;
import fr.univ_lille.gitlab.classrooms.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.annotation.security.PermitAll;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/")
@PermitAll
class DashboardController {

    private final ClassroomService classroomService;

    private final AssignmentService assignmentService;

    DashboardController(ClassroomService classroomService, AssignmentService assignmentService) {
        this.classroomService = classroomService;
        this.assignmentService = assignmentService;
    }

    record ClassroomAndStudentAssignmentsDTO(ClassroomEntity classroom, List<StudentAssignment> studentAssignments){}

    @GetMapping
    public String getDashboard(Model model, @ModelAttribute("user") ClassroomUser classroomUser) {
        if(classroomUser.isTeacher()){
            var classrooms = classroomService.getAllClassrooms();
            model.addAttribute("classrooms", classrooms);
            return "dashboard/teacher-dashboard";
        } else {
            var classrooms = classroomService.getAllJoinedClassrooms(classroomUser);

            var classroomsAndAssignments = classrooms.stream().map(classroom -> {
                var studentAssignments = this.assignmentService.getAllStudentAssignmentsForAClassroom(classroom, classroomUser);
                return new ClassroomAndStudentAssignmentsDTO(classroom, studentAssignments);
            }).toList();

            model.addAttribute("classrooms", classroomsAndAssignments);

            return "dashboard/student-dashboard";
        }
    }

}
