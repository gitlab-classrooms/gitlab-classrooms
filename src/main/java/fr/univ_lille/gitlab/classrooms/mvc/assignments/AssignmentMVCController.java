package fr.univ_lille.gitlab.classrooms.mvc.assignments;

import fr.univ_lille.gitlab.classrooms.assignments.AssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.AssignmentType;
import fr.univ_lille.gitlab.classrooms.assignments.ExerciseAssignment;
import fr.univ_lille.gitlab.classrooms.assignments.QuizAssignment;
import fr.univ_lille.gitlab.classrooms.domain.classrooms.ClassroomService;
import fr.univ_lille.gitlab.classrooms.gitlab.GitLabException;
import fr.univ_lille.gitlab.classrooms.gitlab.Gitlab;
import fr.univ_lille.gitlab.classrooms.quiz.QuizService;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpSession;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
@RolesAllowed("STUDENT")
class AssignmentMVCController {

    private final AssignmentService assignmentService;

    private final QuizService quizService;

    private final ClassroomService classroomService;

    private final Gitlab gitlab;

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentMVCController.class.getName());

    AssignmentMVCController(AssignmentService assignmentService, QuizService quizService, ClassroomService classroomService, Gitlab gitlab) {
        this.assignmentService = assignmentService;
        this.quizService = quizService;
        this.classroomService = classroomService;
        this.gitlab = gitlab;
    }

    @GetMapping("/assignments/{assignmentId}")
    @RolesAllowed("TEACHER")
    String viewAssignment(@PathVariable UUID assignmentId, Model model) {
        var assignment = this.assignmentService.getAssignment(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var assignmentResults = this.assignmentService.getAssignmentResults(assignment);
        model.addAttribute("assignment", assignment);

        if (assignment.getType() == AssignmentType.QUIZ) {
            var quizAssignment = (QuizAssignment) assignment;
            model.addAttribute("quiz", quizAssignment.getQuiz());

            model.addAttribute("quizResults", assignmentResults);

            return "quiz/all-submissions";
        }
        if (assignment.getType() == AssignmentType.EXERCISE) {
            var exerciseAssignment = (ExerciseAssignment) assignment;
            model.addAttribute("exercise", exerciseAssignment);

            model.addAttribute("exerciseResults", assignmentResults);
            return "exercise/all-submissions";
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/assignments/{assignmentId}/accept")
    String showAcceptAssignment(@PathVariable UUID assignmentId, @ModelAttribute("user") ClassroomUser student, Model model, HttpSession session) {
        var assignment = this.assignmentService.getAssignment(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // check if the student already belongs to the classroom, if not, redirect to the classroom join link
        var classroom = assignment.getClassroom();
        if (!classroom.getStudents().contains(student)) {
            session.setAttribute("redirect", "/assignments/" + assignmentId + "/accept");
            return "redirect:/classrooms/" + classroom.getId() + "/join";
        }

        model.addAttribute("assignment", assignment);
        return "assignments/accept";
    }

    @PostMapping("/assignments/{assignmentId}/accept")
    String acceptAssignment(@PathVariable UUID assignmentId, @ModelAttribute("user") ClassroomUser student, Model model) {
        var assignment = this.assignmentService.getAssignment(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        try {
            this.assignmentService.acceptAssigment(assignment, student);
        }
        catch (GitLabApiException | GitLabException e){
            LOGGER.error("Could not accept assignment with id {} for user {}", assignmentId, student.getName());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not accept assignment", e);
        }

        if(assignment instanceof ExerciseAssignment exerciseAssignment){
            // loading the assignment result to be able to show the gitlab URL after project creation
            var assignmentResult = this.assignmentService.getAssignmentResultsForStudent(exerciseAssignment, student);
            model.addAttribute("assignmentResult", assignmentResult);
        }

        model.addAttribute("assignment", assignment);
        return "assignments/accepted";
    }

    @RolesAllowed("TEACHER")
    @GetMapping("/classrooms/{classroomId}/assignments/new")
    String newAssignment(@PathVariable UUID classroomId, Model model) throws GitLabApiException {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("classroom", classroom);

        model.addAttribute("quizzes", this.quizService.getAllQuizzes());

        model.addAttribute("repositories", this.gitlab.getProjectTemplates(classroom));
        return "assignments/new";
    }

    record CreateAssignmentDTO(String assignmentName,
                               AssignmentType assignmentType,
                               String quizName,
                               String repositoryId) {

    }

    @RolesAllowed("TEACHER")
    @PostMapping("/classrooms/{classroomId}/assignments/new")
    String createAssignment(@PathVariable UUID classroomId, CreateAssignmentDTO createAssignmentDTO) throws GitLabApiException {
        var classroom = this.classroomService.getClassroom(classroomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (createAssignmentDTO.assignmentType == AssignmentType.QUIZ) {
            this.assignmentService.createQuizAssignment(classroom, createAssignmentDTO.assignmentName, createAssignmentDTO.quizName);
        } else if (createAssignmentDTO.assignmentType == AssignmentType.EXERCISE) {
            this.assignmentService.createExerciseAssignment(classroom, createAssignmentDTO.assignmentName, createAssignmentDTO.repositoryId);
        }

        return "redirect:/classrooms/" + classroomId;
    }

}
