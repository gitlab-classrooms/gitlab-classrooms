package fr.univ_lille.gitlab.classrooms.assignments;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
@RolesAllowed("TEACHER")
class ArchiveRestController {

    private final AssignmentService assignmentService;

    private final ArchiveAssignmentUseCase archiveAssignmentUseCase;

    ArchiveRestController(AssignmentService assignmentService, ArchiveAssignmentUseCase archiveAssignmentUseCase) {
        this.assignmentService = assignmentService;
        this.archiveAssignmentUseCase = archiveAssignmentUseCase;
    }

    @GetMapping("/assignments/{assignmentId}/archive")
    String archiveAssignment(@PathVariable UUID assignmentId, Model model) {
        var assignment = this.assignmentService.getAssignment(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        this.archiveAssignmentUseCase.archive(assignment);

        return "redirect:/classrooms/" + assignment.getClassroom().getId();
    }
}
