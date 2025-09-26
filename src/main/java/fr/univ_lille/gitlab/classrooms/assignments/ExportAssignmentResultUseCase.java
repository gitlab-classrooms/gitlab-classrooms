package fr.univ_lille.gitlab.classrooms.assignments;

import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class ExportAssignmentResultUseCase {

    private final AssignmentService assignmentService;

    ExportAssignmentResultUseCase(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    public record ExportRow(String studentEmail,
                            String studentName,
                            Long assignmentScore,
                            Long maxScore,
                            Integer retakes,
                            ZonedDateTime submissionDate) {}

    public record ExportData(UUID assignmentId, String assignmentName, AssignmentType assignmentType, List<ExportRow> rows) {}

    public ExportData getExportData(UUID assignmentId) {
        var assignment = assignmentService.getAssignment(assignmentId).orElseThrow();
        var results = assignmentService.getAssignmentResults(assignment);

        var rows = results.stream().map(sa -> {
            Integer retakes = null;
            if (sa instanceof StudentQuizAssignment quiz) {
                retakes = quiz.getRetakes();
            }
            return new ExportRow(
                    sa.getStudent() != null ? sa.getStudent().getEmail() : null,
                    sa.getStudent() != null ? sa.getStudent().getName() : null,
                    sa.getScore(),
                    sa.getMaxScore(),
                    retakes,
                    sa.getSubmissionDate()
            );
        }).toList();

        return new ExportData(assignment.getId(), assignment.getName(), assignment.getType(), rows);
    }
}
