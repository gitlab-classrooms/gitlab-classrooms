package fr.univ_lille.gitlab.classrooms.api;

import fr.univ_lille.gitlab.classrooms.assignments.StudentAssignmentService;
import fr.univ_lille.gitlab.classrooms.assignments.grading.AssignmentGradeService;
import fr.univ_lille.gitlab.classrooms.assignments.grading.AssignmentGradingException;
import fr.univ_lille.gitlab.classrooms.users.ClassroomUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/assignments/")
class UploadJunitGradingRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadJunitGradingRestController.class.getName());

    private final StudentAssignmentService studentAssignmentService;

    private final AssignmentGradeService assignmentGradeService;

    UploadJunitGradingRestController(StudentAssignmentService studentAssignmentService, AssignmentGradeService assignmentGradeService) {
        this.studentAssignmentService = studentAssignmentService;
        this.assignmentGradeService = assignmentGradeService;
    }

    /**
     * Submits a JUnit report file to an assignment for grading.
     * Multiple different JUnit reports can be submitted and will be added to the grade.
     * @param authenticationPrincipal The principal <b>MUST</b> be a Gitlab ID Token, to identify the Gitlab Project ID
     * @param student The student submitting its assignment, computed from the token
     * @param testResultsFile The XML test result file, in the JUnit format
     * @throws IOException if the XML test result file cannot be read
     */
    @PostMapping("/submit/junit")
    void submitAssignment(Principal authenticationPrincipal,
                                    @ModelAttribute("user") ClassroomUser student,
                                    @RequestParam("file") MultipartFile testResultsFile) throws IOException {
        // this ressource only works with gitlab id token authentication
        if (!(authenticationPrincipal instanceof JwtAuthenticationToken jwtToken)) {
            LOGGER.error("Forbidden access to /api/assignments/submit/junit");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        // get the gitlab project id from the authentication token
        if(! jwtToken.getTokenAttributes().containsKey("project_id")){
            LOGGER.error("No project_id found in authentication token");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No project_id found in authentication token");
        }
        var gitlabProjectId = Long.parseLong(jwtToken.getTokenAttributes().get("project_id").toString());

        // find the associated exercise assignment
        var exerciseAssignment = this.studentAssignmentService.getByGitlabProjectId(gitlabProjectId)
                .orElseThrow(() -> {
                    var message = "Student Exercise Assignment not found for student '%s', project '%s'".formatted(student.getName(), gitlabProjectId);
                    LOGGER.error(message);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, message );
                });

        // grade using the test report
        try {
            this.assignmentGradeService.gradeAssignmentWithJUnitReport(exerciseAssignment, testResultsFile.getInputStream());
        } catch (AssignmentGradingException e) {
            LOGGER.error("Error when grading assignment for student '{}', project '{}'", student.getName(), gitlabProjectId);
            throw new ResponseStatusException(500, e.getMessage(), e);
        }

        this.studentAssignmentService.save(exerciseAssignment);
    }
}
